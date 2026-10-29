package fr.tflag.droidboost.ui.storage

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import fr.tflag.droidboost.AppContainer
import fr.tflag.droidboost.apps.AppEntry
import fr.tflag.droidboost.core.format.Formats
import fr.tflag.droidboost.core.shell.CommandOutcome
import fr.tflag.droidboost.shell.ShizukuState
import fr.tflag.droidboost.system.MediaBreakdown
import fr.tflag.droidboost.system.StorageTotals
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class StorageState(
    val loading: Boolean = true,
    val totals: StorageTotals? = null,
    val media: MediaBreakdown? = null,
    /** Installed apps with a known size. */
    val apps: List<AppEntry> = emptyList(),
    val hasUsageAccess: Boolean = false,
    val hasAllFilesAccess: Boolean = false,
    val shizuku: ShizukuState = ShizukuState.Unavailable,
    val trimming: Boolean = false,
) {
    val appsBytes: Long get() = apps.sumOf { it.size?.totalBytes ?: 0L }
    val cacheBytes: Long get() = apps.sumOf { it.size?.cacheBytes ?: 0L }
}

class StorageViewModel(private val container: AppContainer) : ViewModel() {

    private val content = MutableStateFlow(StorageState())
    private val _messages = Channel<String>(Channel.BUFFERED)
    val messages: Flow<String> = _messages.receiveAsFlow()

    val state: StateFlow<StorageState> = combine(content, container.shell.state) { storage, shizuku ->
        storage.copy(shizuku = shizuku)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), StorageState())

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            content.value = content.value.copy(loading = true)
            val apps = container.apps.loadAll().filter { it.size != null }
            content.value = withContext(Dispatchers.IO) {
                content.value.copy(
                    loading = false,
                    totals = container.storage.totals(),
                    media = container.storage.media(),
                    apps = apps,
                    hasUsageAccess = container.access.hasUsageStats(),
                    hasAllFilesAccess = container.access.hasAllFilesAccess(),
                )
            }
        }
    }

    /** Device-wide cache purge through Shizuku (`pm trim-caches`); per-app clearing needs root. */
    fun trimAllCaches() {
        if (content.value.trimming) return
        viewModelScope.launch {
            content.value = content.value.copy(trimming = true)
            val before = withContext(Dispatchers.IO) { container.storage.totals()?.freeBytes }
            val outcome = container.appActions.trimAllCaches()
            val after = withContext(Dispatchers.IO) { container.storage.totals()?.freeBytes }
            content.value = content.value.copy(trimming = false)
            _messages.send(
                when (outcome) {
                    is CommandOutcome.Success -> {
                        val freed = if (before != null && after != null) (after - before).coerceAtLeast(0) else null
                        freed?.let { "Caches vidés : ${Formats.bytes(it)} libérés" } ?: "Caches vidés"
                    }
                    is CommandOutcome.RootRequired -> "Android refuse cette action sans root"
                    is CommandOutcome.Failed -> "Échec : ${outcome.message.lineSequence().firstOrNull().orEmpty()}"
                },
            )
            refresh()
        }
    }

    fun onSystemCacheDialogClosed() = refresh()
}
