package fr.tflag.droidboost.system

import android.app.ActivityManager
import android.content.Context
import fr.tflag.droidboost.core.model.MemInfo
import fr.tflag.droidboost.core.parse.MemInfoParser
import fr.tflag.droidboost.core.shell.ShellCommands
import fr.tflag.droidboost.shell.ShizukuShell
import java.io.File

data class MemorySnapshot(
    val totalBytes: Long,
    val availableBytes: Long,
    val lowMemory: Boolean,
    val thresholdBytes: Long,
    /** `/proc/meminfo`, read directly or through Shizuku; null when neither works. */
    val details: MemInfo?,
) {
    val usedBytes: Long get() = totalBytes - availableBytes
    val usedFraction: Float get() = if (totalBytes > 0) usedBytes.toFloat() / totalBytes else 0f
}

class MemoryReader(context: Context, private val shell: ShizukuShell) {

    private val activityManager = context.getSystemService(ActivityManager::class.java)

    suspend fun read(): MemorySnapshot {
        val info = ActivityManager.MemoryInfo().also(activityManager::getMemoryInfo)
        val raw = runCatching { File("/proc/meminfo").readText() }.getOrNull()
            ?: shell.output(ShellCommands.MEMINFO)
        return MemorySnapshot(
            totalBytes = info.totalMem,
            availableBytes = info.availMem,
            lowMemory = info.lowMemory,
            thresholdBytes = info.threshold,
            details = raw?.let(MemInfoParser::parse),
        )
    }
}
