package fr.tflag.droidboost.ui.access

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import fr.tflag.droidboost.AppContainer
import fr.tflag.droidboost.access.DevGrant
import fr.tflag.droidboost.core.shell.CommandOutcome
import fr.tflag.droidboost.core.shell.ShellCommands
import fr.tflag.droidboost.shell.ShizukuState
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class AccessState(
    val shizuku: ShizukuState = ShizukuState.Unavailable,
    val usageStats: Boolean = false,
    val allFiles: Boolean = false,
    val notifications: Boolean = false,
    val overlay: Boolean = false,
    val grants: Map<DevGrant, Boolean> = emptyMap(),
    val adbScript: String = "",
    val granting: Boolean = false,
)

class AccessViewModel(private val container: AppContainer) : ViewModel() {

    private val content = MutableStateFlow(AccessState())
    private val _messages = Channel<String>(Channel.BUFFERED)
    val messages: Flow<String> = _messages.receiveAsFlow()

    val state: StateFlow<AccessState> = combine(content, container.shell.state) { access, shizuku ->
        access.copy(shizuku = shizuku)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AccessState())

    val packageName: String get() = container.packageName

    fun refresh() {
        container.shell.refresh()
        val access = container.access
        content.value = content.value.copy(
            usageStats = access.hasUsageStats(),
            allFiles = access.hasAllFilesAccess(),
            notifications = access.hasNotifications(),
            overlay = access.hasOverlay(),
            grants = DevGrant.entries.associateWith(access::isGranted),
            adbScript = access.adbScript(),
        )
    }

    fun requestShizukuPermission() = container.shell.requestPermission()

    /**
     * Grants every development permission through Shizuku; they then persist without it.
     * A grant that restarts the app ends this coroutine, so those go last, once everything else is done.
     */
    fun grantAllViaShizuku() {
        if (content.value.granting) return
        viewModelScope.launch {
            content.value = content.value.copy(granting = true)
            val pkg = container.packageName
            container.shell.run(ShellCommands.allowUsageStats(pkg))
            val (restarting, others) = DevGrant.entries.filterNot(container.access::isGranted).partition { it.restartsApp }
            suspend fun grant(entry: DevGrant) = entry to container.shell.run(ShellCommands.grant(pkg, entry.permission))
            val failures = others.map { grant(it) }.filter { (_, outcome) -> outcome !is CommandOutcome.Success }.toMutableList()
            if (restarting.isNotEmpty()) {
                _messages.send("DroidBoost va redémarrer pour appliquer : ${restarting.joinToString { it.title }}")
                delay(RESTART_NOTICE_MS)
                // A successful grant kills the app here: a result only comes back when it failed.
                failures += restarting.map { grant(it) }.filter { (_, outcome) -> outcome !is CommandOutcome.Success }
            }
            content.value = content.value.copy(granting = false)
            refresh()
            _messages.send(
                if (failures.isEmpty()) "Permissions accordées : elles resteront actives après redémarrage"
                else "Échec pour : ${failures.joinToString { it.first.title }}",
            )
        }
    }

    private companion object {
        /** Long enough to read the restart notice before Android kills the app. */
        const val RESTART_NOTICE_MS = 1_500L
    }
}
