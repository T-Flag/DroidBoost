package fr.tflag.droidboost.ui.storage

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import fr.tflag.droidboost.AppContainer
import fr.tflag.droidboost.apps.AppEntry
import fr.tflag.droidboost.core.format.Formats
import fr.tflag.droidboost.core.monitor.Metrics
import fr.tflag.droidboost.core.shell.CommandOutcome
import fr.tflag.droidboost.core.shell.ShellCommands
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
    /** Apps whose data grew the most over 7 days (from the history recorded every 6 h). */
    val growth: List<Pair<AppEntry, Long>> = emptyList(),
    /** User apps from the Play Store unused for 30 days, biggest first. */
    val archiveCandidates: List<AppEntry> = emptyList(),
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
                val byPackage = apps.associateBy { it.packageName }
                val weekAgo = System.currentTimeMillis() - WEEK_MS
                val growth = container.history.samples().growth(Metrics.APP_DATA, weekAgo).mapNotNull { row ->
                    val app = byPackage[row.tag] ?: return@mapNotNull null
                    val delta = (row.latest ?: return@mapNotNull null) - (row.previous ?: return@mapNotNull null)
                    if (delta > MIN_GROWTH_BYTES) app to delta.toLong() else null
                }.sortedByDescending { it.second }.take(MAX_LISTED)
                val now = System.currentTimeMillis()
                val archive = apps.filter {
                    !it.isSystem && it.installedForUser && it.installer == PLAY_STORE && it.packageName != container.packageName &&
                        (it.lastUsed == null || now - it.lastUsed > UNUSED_MS)
                }.sortedByDescending { it.size?.totalBytes ?: 0L }.take(MAX_LISTED)
                content.value.copy(
                    loading = false,
                    totals = container.storage.totals(),
                    media = container.storage.media(),
                    apps = apps,
                    hasUsageAccess = container.access.hasUsageStats(),
                    hasAllFilesAccess = container.access.hasAllFilesAccess(),
                    growth = growth,
                    archiveCandidates = archive,
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
                        container.journal.logAction(null, "Caches de toutes les applis vidés", freed?.let { "${Formats.bytes(it)} libérés" })
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

    /** Archives through Shizuku (`pm archive`); returns false when the caller should open the system screen instead. */
    fun archive(app: AppEntry): Boolean {
        if (!container.shell.isReady) return false
        viewModelScope.launch {
            val outcome = container.shell.run(ShellCommands.archive(app.packageName))
            if (outcome is CommandOutcome.Success) {
                container.journal.logAction(app.packageName, "Archivée : ${app.label}", app.size?.let { "${Formats.bytes(it.appBytes)} libérés" })
            }
            _messages.send(
                when (outcome) {
                    is CommandOutcome.Success -> "${app.label} archivée : l'icône reste, l'appli se réinstalle d'un toucher"
                    else -> "Archivage impossible : ouvrez les infos de l'appli"
                },
            )
            refresh()
        }
        return true
    }

    private companion object {
        const val WEEK_MS = 7 * 24 * 3_600_000L
        const val UNUSED_MS = 30 * 24 * 3_600_000L
        const val MIN_GROWTH_BYTES = 20.0 * 1024 * 1024
        const val MAX_LISTED = 10
        const val PLAY_STORE = "com.android.vending"
    }
}
