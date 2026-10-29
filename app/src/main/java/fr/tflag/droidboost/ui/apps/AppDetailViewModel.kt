package fr.tflag.droidboost.ui.apps

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import fr.tflag.droidboost.AppContainer
import fr.tflag.droidboost.apps.AppEntry
import fr.tflag.droidboost.apps.PermissionEntry
import fr.tflag.droidboost.core.maintenance.DebloatCatalog
import fr.tflag.droidboost.core.shell.CommandOutcome
import fr.tflag.droidboost.core.shell.StandbyBucket
import fr.tflag.droidboost.shell.ShizukuState
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class AppDetailState(
    val loading: Boolean = true,
    val app: AppEntry? = null,
    val permissions: List<PermissionEntry> = emptyList(),
    val standbyBucket: StandbyBucket? = null,
    val backgroundRestricted: Boolean? = null,
    val shizuku: ShizukuState = ShizukuState.Unavailable,
    val busy: Boolean = false,
    /** Set when Android refused `pm uninstall --user 0`: the screen offers to disable instead. */
    val offerDisable: Boolean = false,
    val isSelf: Boolean = false,
    /** Why disabling/removing is blocked (critical Pixel component), null when allowed. */
    val protectedReason: String? = null,
)

class AppDetailViewModel(private val container: AppContainer, private val packageName: String) : ViewModel() {

    private val content = MutableStateFlow(
        AppDetailState(
            isSelf = packageName == container.packageName,
            protectedReason = DebloatCatalog.find(packageName)?.takeIf { DebloatCatalog.isProtected(packageName) }?.note
                ?: if (DebloatCatalog.isProtected(packageName)) "Composant chargé au démarrage du système." else null,
        ),
    )
    private val _messages = Channel<String>(Channel.BUFFERED)
    val messages: Flow<String> = _messages.receiveAsFlow()

    val state: StateFlow<AppDetailState> = combine(content, container.shell.state) { detail, shizuku ->
        detail.copy(shizuku = shizuku)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), content.value)

    init {
        reload()
        viewModelScope.launch {
            // Privileged details appear as soon as Shizuku becomes ready.
            container.shell.state.collect { if (it is ShizukuState.Ready) loadPrivileged() }
        }
    }

    fun reload() {
        viewModelScope.launch {
            val app = container.apps.load(packageName)
            content.value = content.value.copy(
                loading = false,
                app = app,
                permissions = if (app?.installedForUser == true) container.apps.permissions(packageName) else emptyList(),
            )
            loadPrivileged()
        }
    }

    private suspend fun loadPrivileged() {
        if (!container.shell.isReady) return
        content.value = content.value.copy(
            standbyBucket = container.appActions.standbyBucket(packageName),
            backgroundRestricted = container.appActions.isBackgroundRestricted(packageName),
        )
    }

    fun forceStop() = act("Appli arrêtée") { forceStop(packageName) }

    fun setEnabled(enabled: Boolean) {
        if (!enabled && content.value.protectedReason != null) return
        act(if (enabled) "Appli réactivée" else "Appli désactivée") { setEnabled(packageName, enabled) }
    }

    fun setSuspended(suspended: Boolean) =
        act(if (suspended) "Appli suspendue" else "Appli réactivée") { setSuspended(packageName, suspended) }

    fun uninstallForUser() {
        if (content.value.protectedReason != null) return
        act("Appli retirée pour cet utilisateur (réinstallable)") { uninstallForUser(packageName) }
    }

    fun reinstall() = act("Appli réinstallée") { reinstall(packageName) }

    fun clearData() = act("Données effacées") { clearData(packageName) }

    fun setPermission(permission: String, granted: Boolean) =
        act(if (granted) "Permission accordée" else "Permission révoquée") { setPermission(packageName, permission, granted) }

    fun setStandbyBucket(bucket: StandbyBucket) = act("Bucket de veille modifié") { setStandbyBucket(packageName, bucket) }

    fun setBackgroundRestricted(restricted: Boolean) =
        act(if (restricted) "Arrière-plan restreint" else "Arrière-plan autorisé") { setBackgroundRestricted(packageName, restricted) }

    fun dismissDisableOffer() {
        content.value = content.value.copy(offerDisable = false)
    }

    private fun act(successMessage: String, action: suspend fr.tflag.droidboost.apps.AppActions.() -> CommandOutcome) {
        if (content.value.busy) return
        viewModelScope.launch {
            content.value = content.value.copy(busy = true, offerDisable = false)
            val outcome = container.appActions.action()
            content.value = content.value.copy(busy = false, offerDisable = outcome is CommandOutcome.RootRequired)
            _messages.send(
                when (outcome) {
                    is CommandOutcome.Success -> successMessage
                    is CommandOutcome.RootRequired -> "Android refuse cette action sans root"
                    is CommandOutcome.Failed -> "Échec : ${outcome.message.lineSequence().firstOrNull().orEmpty().take(MAX_MESSAGE)}"
                },
            )
            if (outcome is CommandOutcome.Success) {
                val label = content.value.app?.label ?: packageName
                container.journal.logAction(packageName, "$successMessage : $label", "depuis DroidBoost")
                container.appScope.launch { runCatching { container.journal.refresh() } }
            }
            container.apps.notifyChanged(packageName)
            reload()
        }
    }

    private companion object {
        const val MAX_MESSAGE = 160
    }
}
