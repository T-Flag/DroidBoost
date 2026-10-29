package fr.tflag.droidboost.system

import fr.tflag.droidboost.core.model.CpuCluster
import fr.tflag.droidboost.core.model.CpuCore
import fr.tflag.droidboost.core.model.CpuTimes
import fr.tflag.droidboost.core.model.CpuTopology
import fr.tflag.droidboost.core.parse.CpuFreqParser
import fr.tflag.droidboost.core.parse.ProcStatParser
import fr.tflag.droidboost.core.shell.ShellCommands
import fr.tflag.droidboost.shell.ShizukuShell
import java.io.File

data class CpuSnapshot(
    val clusters: List<CpuCluster>,
    /** Busy fraction per core; [CpuTimes.TOTAL] is the whole CPU. Only through Shizuku (`/proc/stat`). */
    val load: Map<Int, Float>,
    val source: Source,
) {
    enum class Source { APP, SHELL, NONE }

    val totalLoad: Float? get() = load[CpuTimes.TOTAL]
}

class CpuReader(private val shell: ShizukuShell) {

    private val possibleCores: List<Int> by lazy {
        readText("$CPU_DIR/possible")?.let(CpuFreqParser::parseRange)
            ?: (0 until Runtime.getRuntime().availableProcessors()).toList()
    }

    private var previousTimes: Map<Int, CpuTimes>? = null

    suspend fun read(): CpuSnapshot {
        val fromApp = possibleCores.map(::readCoreFromSysfs)
        val (cores, source) = when {
            fromApp.any { it.currentKhz != null } -> fromApp to CpuSnapshot.Source.APP
            else -> shell.output(CpuFreqParser.COMMAND)?.let(CpuFreqParser::parse)?.takeIf { it.isNotEmpty() }
                ?.let { it to CpuSnapshot.Source.SHELL }
                ?: (fromApp to CpuSnapshot.Source.NONE)
        }
        return CpuSnapshot(clusters = CpuTopology.clusters(cores), load = load(), source = source)
    }

    private suspend fun load(): Map<Int, Float> {
        val current = shell.output(ShellCommands.PROC_STAT)?.let(ProcStatParser::parse) ?: return emptyMap()
        val previous = previousTimes
        previousTimes = current
        return if (previous == null) emptyMap() else ProcStatParser.load(previous, current)
    }

    private fun readCoreFromSysfs(index: Int): CpuCore {
        val base = "$CPU_DIR/cpu$index"
        val online = index == 0 || readText("$base/online")?.trim() != "0"
        return CpuCore(
            index = index,
            currentKhz = readLong("$base/cpufreq/scaling_cur_freq"),
            minKhz = readLong("$base/cpufreq/cpuinfo_min_freq"),
            maxKhz = readLong("$base/cpufreq/cpuinfo_max_freq"),
            online = online,
        )
    }

    private fun readLong(path: String): Long? = readText(path)?.trim()?.toLongOrNull()

    private fun readText(path: String): String? = runCatching { File(path).readText() }.getOrNull()

    private companion object {
        const val CPU_DIR = "/sys/devices/system/cpu"
    }
}
