package fr.tflag.droidboost.ui.debloat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import fr.tflag.droidboost.AppContainer
import fr.tflag.droidboost.apps.AppEntry
import fr.tflag.droidboost.core.maintenance.DebloatCatalog
import fr.tflag.droidboost.core.maintenance.DebloatEntry
import fr.tflag.droidboost.core.shell.CommandOutcome
import fr.tflag.droidboost.shell.ShizukuState
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class DebloatItem(val entry: DebloatEntry, val app: AppEntry, val registered: Boolean) {
    val active: Boolean get() = app.enabled && app.installedForUser && !app.suspended

    /** Disabled from DroidBoost, then brought back (system update, factory reset…). */
    val reverted: Boolean get() = registered && active
}

data class DebloatState(
    val loading: Boolean = true,
    val items: List<DebloatItem> = emptyList(),
    val shizuku: ShizukuState = ShizukuState.Unavailable,
    val busy: Boolean = false,
) {
    val reverted: List<DebloatItem> get() = items.filter { it.reverted }
}

class DebloatViewModel(private val container: AppContainer) : ViewModel() {

    private val content = MutableStateFlow(DebloatState())
    private val _messages = Channel<String>(Channel.BUFFERED)
    val messages: Flow<String> = _messages.receiveAsFlow()

    val state: StateFlow<DebloatState> = combine(content, container.shell.state) { debloat, shizuku -> debloat.copy(shizuku = shizuku) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DebloatState())

    init {
        viewModelScope.launch { container.debloatRegistry.packages.collect { reload() } }
    }

    private suspend fun reload() {
        val registered = container.debloatRegistry.packages.first()
        val items = DebloatCatalog.entries.mapNotNull { entry ->
            container.apps.load(entry.packageName)?.let { DebloatItem(entry, it, entry.packageName in registered) }
        }
        content.value = content.value.copy(loading = false, items = items)
    }

    fun disable(item: DebloatItem) = act(item, "Désactivée") {
        container.appActions.setEnabled(item.app.packageName, false).also {
            if (it is CommandOutcome.Success) container.debloatRegistry.add(item.app.packageName)
        }
    }

    fun enable(item: DebloatItem) = act(item, "Réactivée") {
        val pkg = item.app.packageName
        val outcome = if (item.app.installedForUser) container.appActions.setEnabled(pkg, true) else container.appActions.reinstall(pkg)
        if (outcome is CommandOutcome.Success) {
            if (item.app.suspended) container.appActions.setSuspended(pkg, false)
            container.debloatRegistry.remove(pkg)
        }
        outcome
    }

    /** Re-disables every package a system update brought back. */
    fun redoReverted() {
        val targets = content.value.reverted
        viewModelScope.launch {
            content.value = content.value.copy(busy = true)
            val failed = targets.count { container.appActions.setEnabled(it.app.packageName, false) !is CommandOutcome.Success }
            content.value = content.value.copy(busy = false)
            container.journal.logAction(null, "Debloat réappliqué", "${targets.size - failed} appli(s) désactivée(s) à nouveau")
            _messages.send(if (failed == 0) "Debloat réappliqué" else "$failed échec(s)")
            reload()
        }
    }

    private fun act(item: DebloatItem, verb: String, action: suspend () -> CommandOutcome) {
        if (content.value.busy || DebloatCatalog.isProtected(item.app.packageName)) return
        viewModelScope.launch {
            content.value = content.value.copy(busy = true)
            val outcome = action()
            content.value = content.value.copy(busy = false)
            if (outcome is CommandOutcome.Success) {
                container.journal.logAction(item.app.packageName, "$verb : ${item.entry.label}", "debloat depuis DroidBoost")
                container.apps.notifyChanged(item.app.packageName)
            }
            _messages.send(
                when (outcome) {
                    is CommandOutcome.Success -> "$verb : ${item.entry.label}"
                    is CommandOutcome.RootRequired -> "Android refuse cette action sans root"
                    is CommandOutcome.Failed -> "Échec : ${outcome.message.lineSequence().firstOrNull().orEmpty()}"
                },
            )
            reload()
        }
    }
}
