package fr.tflag.droidboost.ui.cleaner

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import fr.tflag.droidboost.AppContainer
import fr.tflag.droidboost.core.format.Formats
import fr.tflag.droidboost.core.maintenance.DuplicateGroup
import fr.tflag.droidboost.core.maintenance.FileEntry
import fr.tflag.droidboost.core.shell.CommandOutcome
import fr.tflag.droidboost.maintenance.Leftover
import fr.tflag.droidboost.maintenance.ScanResult
import fr.tflag.droidboost.shell.ShizukuState
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class CleanerState(
    val scanning: Boolean = false,
    val scannedFiles: Int = 0,
    val result: ScanResult? = null,
    val leftovers: List<Leftover>? = null,
    val hasAllFiles: Boolean = false,
    val shizuku: ShizukuState = ShizukuState.Unavailable,
    val busy: Boolean = false,
)

class CleanerViewModel(private val container: AppContainer) : ViewModel() {

    private val content = MutableStateFlow(CleanerState(hasAllFiles = container.access.hasAllFilesAccess()))
    private val _messages = Channel<String>(Channel.BUFFERED)
    val messages: Flow<String> = _messages.receiveAsFlow()
    private var scanJob: Job? = null

    val state: StateFlow<CleanerState> = combine(content, container.shell.state) { cleaner, shizuku -> cleaner.copy(shizuku = shizuku) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), content.value)

    fun scan() {
        if (scanJob?.isActive == true) return
        val hasAllFiles = container.access.hasAllFilesAccess()
        content.value = content.value.copy(hasAllFiles = hasAllFiles, scanning = true, scannedFiles = 0)
        scanJob = viewModelScope.launch {
            val result = if (hasAllFiles) {
                runCatching { container.cleaner.scan { count -> content.value = content.value.copy(scannedFiles = count) } }.getOrNull()
            } else {
                null
            }
            val leftovers = container.cleaner.leftovers()
            content.value = content.value.copy(scanning = false, result = result, leftovers = leftovers)
        }
    }

    fun deleteFile(file: FileEntry) = mutate("Fichier supprimé : ${Formats.bytes(file.sizeBytes)} libérés") {
        container.cleaner.delete(file.path).also { deleted ->
            if (deleted) updateResult { it.copy(largest = it.largest - file) }
        }
    }

    /** Keeps the oldest copy of each group and deletes the others after a full-content check. */
    fun deleteDuplicates(group: DuplicateGroup) = mutate("Copies supprimées : ${Formats.bytes(group.wastedBytes)} libérés") {
        val keep = group.files.first()
        val allDeleted = group.files.drop(1).all { container.cleaner.delete(it.path, keepIdenticalTo = keep.path) }
        updateResult { it.copy(duplicates = it.duplicates - group) }
        allDeleted
    }

    fun deleteEmptyFolders() = mutate("Dossiers vides supprimés") {
        val folders = content.value.result?.emptyFolders.orEmpty()
        // Deepest first, so parents emptied by their children can go too.
        val deleted = folders.sortedByDescending { it.length }.count { container.cleaner.delete(it) }
        updateResult { it.copy(emptyFolders = emptyList()) }
        deleted > 0
    }

    fun deleteLeftover(leftover: Leftover) = mutate("Restes de ${leftover.packageName} supprimés") {
        val outcome = container.cleaner.deleteLeftover(leftover)
        if (outcome is CommandOutcome.Success) {
            content.value = content.value.copy(leftovers = content.value.leftovers?.minus(leftover))
            container.journal.logAction(leftover.packageName, "Restes supprimés : ${leftover.packageName}", leftover.area.label)
        }
        outcome is CommandOutcome.Success
    }

    private fun updateResult(transform: (ScanResult) -> ScanResult) {
        content.value.result?.let { content.value = content.value.copy(result = transform(it)) }
    }

    private fun mutate(successMessage: String, action: suspend () -> Boolean) {
        if (content.value.busy) return
        viewModelScope.launch {
            content.value = content.value.copy(busy = true)
            val ok = runCatching { action() }.getOrDefault(false)
            content.value = content.value.copy(busy = false)
            _messages.send(if (ok) successMessage else "Suppression impossible (fichier modifié, protégé ou déjà supprimé)")
        }
    }
}
