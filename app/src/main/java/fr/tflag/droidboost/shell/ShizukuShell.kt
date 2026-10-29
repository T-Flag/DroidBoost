package fr.tflag.droidboost.shell

import android.content.ComponentName
import android.content.Context
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.IBinder
import fr.tflag.droidboost.BuildConfig
import fr.tflag.droidboost.core.shell.CommandOutcome
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import rikka.shizuku.Shizuku
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

sealed interface ShizukuState {
    /** No Shizuku-compatible server is running (not installed, or stopped since the last reboot). */
    data object Unavailable : ShizukuState

    data object PermissionRequired : ShizukuState

    /** The user refused with "don't ask again": only Shizuku's own app can re-authorize. */
    data object PermissionDenied : ShizukuState

    data class Ready(val uid: Int, val serverVersion: Int) : ShizukuState {
        val isRoot: Boolean get() = uid == 0
    }
}

data class ShellResult(val exitCode: Int, val stdout: String, val stderr: String) {
    val outcome: CommandOutcome get() = CommandOutcome.from(exitCode, stdout, stderr)
}

/**
 * Privileged shell through Shizuku (or any server speaking its API: Porter, forks, Sui).
 *
 * Commands run in [ShellService], a UserService that Shizuku spawns with the shell identity.
 */
class ShizukuShell(private val context: Context) {

    private val _state = MutableStateFlow<ShizukuState>(ShizukuState.Unavailable)
    val state: StateFlow<ShizukuState> = _state.asStateFlow()

    val isReady: Boolean get() = _state.value is ShizukuState.Ready

    private val serviceMutex = Mutex()

    @Volatile
    private var service: IShellService? = null

    private val userServiceArgs = Shizuku.UserServiceArgs(ComponentName(context.packageName, ShellService::class.java.name))
        .daemon(false)
        .processNameSuffix("shell")
        .debuggable(BuildConfig.DEBUG)
        .version(BuildConfig.VERSION_CODE)
        .tag("droidboost-shell")

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            service = binder?.takeIf { it.pingBinder() }?.let(IShellService.Stub::asInterface)
            pendingConnection?.let { continuation ->
                pendingConnection = null
                val connected = service
                if (connected != null) continuation(Result.success(connected))
                else continuation(Result.failure(IllegalStateException("Le service shell n'a pas répondu")))
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            service = null
        }
    }

    @Volatile
    private var pendingConnection: ((Result<IShellService>) -> Unit)? = null

    fun start() {
        Shizuku.addBinderReceivedListenerSticky { refresh() }
        Shizuku.addBinderDeadListener {
            service = null
            refresh()
        }
        Shizuku.addRequestPermissionResultListener { _, _ -> refresh() }
        refresh()
    }

    fun refresh() {
        _state.value = runCatching {
            when {
                !Shizuku.pingBinder() || Shizuku.isPreV11() -> ShizukuState.Unavailable
                Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED ->
                    ShizukuState.Ready(uid = Shizuku.getUid(), serverVersion = Shizuku.getVersion())
                Shizuku.shouldShowRequestPermissionRationale() -> ShizukuState.PermissionDenied
                else -> ShizukuState.PermissionRequired
            }
        }.getOrDefault(ShizukuState.Unavailable)
    }

    fun requestPermission() {
        if (runCatching { Shizuku.pingBinder() }.getOrDefault(false)) {
            Shizuku.requestPermission(PERMISSION_REQUEST_CODE)
        }
    }

    /** Runs [command] as the shell user. Throws when Shizuku is not ready. */
    suspend fun exec(command: String, timeoutMs: Long = DEFAULT_TIMEOUT_MS): ShellResult = withContext(Dispatchers.IO) {
        check(isReady) { "Shizuku n'est pas actif ou pas autorisé" }
        val bundle = obtainService().exec(command, timeoutMs)
        ShellResult(
            exitCode = bundle.getInt(ShellService.KEY_CODE),
            stdout = bundle.getString(ShellService.KEY_OUT).orEmpty(),
            stderr = bundle.getString(ShellService.KEY_ERR).orEmpty(),
        )
    }

    /** Like [exec] but never throws: failures become [CommandOutcome.Failed]. */
    suspend fun run(command: String, timeoutMs: Long = DEFAULT_TIMEOUT_MS): CommandOutcome =
        runCatching { exec(command, timeoutMs).outcome }
            .getOrElse { CommandOutcome.Failed(it.message ?: it.javaClass.simpleName) }

    /** Stdout of [command], or null when Shizuku is unavailable or the command fails. */
    suspend fun output(command: String, timeoutMs: Long = DEFAULT_TIMEOUT_MS): String? {
        if (!isReady) return null
        return runCatching { exec(command, timeoutMs) }.getOrNull()?.takeIf { it.exitCode == 0 }?.stdout
    }

    private suspend fun obtainService(): IShellService = serviceMutex.withLock {
        service?.takeIf { it.asBinder().pingBinder() }?.let { return it }
        withTimeout(BIND_TIMEOUT_MS) {
            withContext(Dispatchers.Main) {
                suspendCancellableCoroutine { continuation ->
                    pendingConnection = { result ->
                        result.fold(
                            onSuccess = { if (continuation.isActive) continuation.resume(it) },
                            onFailure = { if (continuation.isActive) continuation.resumeWithException(it) },
                        )
                    }
                    continuation.invokeOnCancellation { pendingConnection = null }
                    Shizuku.bindUserService(userServiceArgs, connection)
                }
            }
        }
    }

    companion object {
        private const val PERMISSION_REQUEST_CODE = 4242
        private const val DEFAULT_TIMEOUT_MS = 30_000L
        private const val BIND_TIMEOUT_MS = 15_000L
    }
}
