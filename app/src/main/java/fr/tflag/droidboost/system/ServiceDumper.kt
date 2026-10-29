package fr.tflag.droidboost.system

import android.content.Context
import android.content.pm.PackageManager
import android.os.IBinder
import android.os.ParcelFileDescriptor
import fr.tflag.droidboost.shell.ShizukuShell
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.concurrent.thread

/**
 * Dumps a system service like `dumpsys <service>`.
 *
 * With the DUMP permission (granted once by `pm grant`), the app asks the service directly through its
 * binder, so it works even when Shizuku is stopped; otherwise it falls back to the Shizuku shell.
 */
class ServiceDumper(private val context: Context, private val shell: ShizukuShell) {

    val canDumpDirectly: Boolean
        get() = context.checkSelfPermission(DUMP) == PackageManager.PERMISSION_GRANTED

    suspend fun dump(service: String, args: List<String> = emptyList()): String? = withContext(Dispatchers.IO) {
        require(SERVICE_NAME.matches(service) && args.all { ARG.matches(it) }) { "Dump refusé : $service $args" }
        (if (canDumpDirectly) direct(service, args) else null)
            ?: shell.output((listOf("dumpsys", service) + args).joinToString(" "), timeoutMs = SHELL_TIMEOUT_MS)
    }

    private fun direct(service: String, args: List<String>): String? = runCatching {
        val binder = Class.forName("android.os.ServiceManager")
            .getMethod("getService", String::class.java)
            .invoke(null, service) as? IBinder ?: return null
        val (readSide, writeSide) = ParcelFileDescriptor.createPipe()
        val output = StringBuilder()
        // The service writes synchronously into the pipe: drain it on another thread to avoid a deadlock.
        val reader = thread(name = "dump-$service") {
            ParcelFileDescriptor.AutoCloseInputStream(readSide).bufferedReader().use { input ->
                val buffer = CharArray(16 * 1024)
                while (true) {
                    val read = input.read(buffer)
                    if (read < 0) break
                    if (output.length < MAX_CHARS) output.append(buffer, 0, minOf(read, MAX_CHARS - output.length))
                }
            }
        }
        try {
            binder.dump(writeSide.fileDescriptor, args.toTypedArray())
        } finally {
            writeSide.close()
        }
        reader.join(READ_TIMEOUT_MS)
        output.toString().takeIf { it.isNotBlank() && !it.startsWith("Permission Denial") }
    }.getOrNull()

    private companion object {
        const val DUMP = "android.permission.DUMP"
        val SERVICE_NAME = Regex("^[a-z_.]+$")
        val ARG = Regex("^-{0,2}[A-Za-z0-9_.=-]+$")
        const val MAX_CHARS = 4 * 1024 * 1024
        const val READ_TIMEOUT_MS = 15_000L
        const val SHELL_TIMEOUT_MS = 60_000L
    }
}
