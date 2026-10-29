package fr.tflag.droidboost.shell

import android.os.Bundle
import java.io.InputStream
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread
import kotlin.system.exitProcess

/**
 * Shizuku UserService: Shizuku instantiates this class in its own process running as the ADB shell
 * user, so every command inherits the shell's permissions.
 */
class ShellService : IShellService.Stub() {

    override fun destroy() {
        exitProcess(0)
    }

    override fun exec(command: String, timeoutMs: Long): Bundle {
        val process = ProcessBuilder("sh", "-c", command).start()
        process.outputStream.close()
        val out = StringBuilder()
        val err = StringBuilder()
        // Both streams are drained concurrently so a chatty command cannot block on a full pipe.
        val readers = listOf(
            thread { drain(process.inputStream, out) },
            thread { drain(process.errorStream, err) },
        )
        val finished = process.waitFor(timeoutMs, TimeUnit.MILLISECONDS)
        if (!finished) process.destroyForcibly()
        readers.forEach { it.join(READER_JOIN_MS) }
        return Bundle().apply {
            putInt(KEY_CODE, if (finished) process.exitValue() else TIMEOUT_CODE)
            putString(KEY_OUT, out.toString())
            putString(KEY_ERR, if (finished) err.toString() else "Délai dépassé (${timeoutMs / 1000} s)\n$err")
        }
    }

    private fun drain(stream: InputStream, target: StringBuilder) {
        stream.bufferedReader().use { reader ->
            val buffer = CharArray(8192)
            while (true) {
                val read = reader.read(buffer)
                if (read < 0) break
                // Binder transactions are capped at 1 MB; keep well below it.
                if (target.length < MAX_OUTPUT_CHARS) {
                    target.append(buffer, 0, minOf(read, MAX_OUTPUT_CHARS - target.length))
                }
            }
        }
    }

    companion object {
        const val KEY_CODE = "code"
        const val KEY_OUT = "out"
        const val KEY_ERR = "err"
        const val TIMEOUT_CODE = -1
        private const val MAX_OUTPUT_CHARS = 256 * 1024
        private const val READER_JOIN_MS = 2_000L
    }
}
