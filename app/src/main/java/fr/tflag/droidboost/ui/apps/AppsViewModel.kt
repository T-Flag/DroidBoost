package fr.tflag.droidboost.ui.apps

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import fr.tflag.droidboost.AppContainer
import fr.tflag.droidboost.apps.AppEntry
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class AppFilter(val label: String) {
    USER("Utilisateur"),
    SYSTEM("Système"),
    INACTIVE("Désactivées / retirées"),
    ALL("Toutes"),
    ;

    fun matches(app: AppEntry): Boolean = when (this) {
        USER -> !app.isSystem
        SYSTEM -> app.isSystem
        INACTIVE -> !app.enabled || app.suspended || !app.installedForUser
        ALL -> true
    }
}

enum class AppSort(val label: String) {
    NAME("Nom"),
    SIZE("Taille"),
    CACHE("Cache"),
    LAST_USED("Dernière utilisation"),
    INSTALLED("Date d'installation"),
    UPDATED("Dernière mise à jour"),
    ;

    val comparator: Comparator<AppEntry>
        get() = when (this) {
            NAME -> compareBy<AppEntry> { it.label.lowercase() }
            SIZE -> compareByDescending<AppEntry> { it.size?.totalBytes ?: -1L }
            CACHE -> compareByDescending<AppEntry> { it.size?.cacheBytes ?: -1L }
            LAST_USED -> compareByDescending<AppEntry> { it.lastUsed ?: -1L }
            INSTALLED -> compareByDescending<AppEntry> { it.firstInstallTime }
            UPDATED -> compareByDescending<AppEntry> { it.lastUpdateTime }
        }
}

data class AppsState(
    val loading: Boolean = true,
    val visible: List<AppEntry> = emptyList(),
    val total: Int = 0,
    val query: String = "",
    val filter: AppFilter = AppFilter.USER,
    val sort: AppSort = AppSort.NAME,
    val hasUsageAccess: Boolean = false,
)

class AppsViewModel(private val container: AppContainer) : ViewModel() {

    private val all = MutableStateFlow<List<AppEntry>?>(null)
    private val query = MutableStateFlow("")
    private val filter = MutableStateFlow(AppFilter.USER)
    private val sort = MutableStateFlow(AppSort.NAME)

    val state: StateFlow<AppsState> = combine(all, query, filter, sort) { apps, q, f, s ->
        val needle = q.trim().lowercase()
        val visible = apps.orEmpty()
            .filter { f.matches(it) }
            .filter { needle.isEmpty() || needle in it.label.lowercase() || needle in it.packageName.lowercase() }
            .sortedWith(s.comparator)
        AppsState(
            loading = apps == null,
            visible = visible,
            total = apps?.size ?: 0,
            query = q,
            filter = f,
            sort = s,
            hasUsageAccess = container.access.hasUsageStats(),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppsState())

    init {
        refresh()
        viewModelScope.launch {
            container.apps.changes.collect { pkg ->
                val updated = container.apps.load(pkg)
                all.value = all.value?.mapNotNull { if (it.packageName == pkg) updated else it }
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            all.value = null
            all.value = container.apps.loadAll()
        }
    }

    fun setQuery(value: String) {
        query.value = value
    }

    fun setFilter(value: AppFilter) {
        filter.value = value
    }

    fun setSort(value: AppSort) {
        sort.value = value
    }
}
