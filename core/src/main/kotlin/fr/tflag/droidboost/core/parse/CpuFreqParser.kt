package fr.tflag.droidboost.core.parse

import fr.tflag.droidboost.core.model.CpuCore

/**
 * Reads per-core frequencies through the shell when sysfs is not readable by the app.
 *
 * [COMMAND] prints one line per core: `cpuN <online> <cur> <min> <max>`, with `-` for unreadable values.
 */
object CpuFreqParser {

    const val COMMAND =
        "cd /sys/devices/system/cpu && for c in cpu[0-9]*; do " +
            "echo \"\$c \$(cat \$c/online 2>/dev/null || echo 1) " +
            "\$(cat \$c/cpufreq/scaling_cur_freq 2>/dev/null || echo -) " +
            "\$(cat \$c/cpufreq/cpuinfo_min_freq 2>/dev/null || echo -) " +
            "\$(cat \$c/cpufreq/cpuinfo_max_freq 2>/dev/null || echo -)\"; done"

    private val LINE = Regex("^cpu(\\d+)\\s+(\\S+)\\s+(\\S+)\\s+(\\S+)\\s+(\\S+)$")

    fun parse(output: String): List<CpuCore> = output.lineSequence()
        .mapNotNull { LINE.find(it.trim()) }
        .map { match ->
            val (index, online, cur, min, max) = match.destructured
            CpuCore(
                index = index.toInt(),
                currentKhz = cur.toLongOrNull(),
                minKhz = min.toLongOrNull(),
                maxKhz = max.toLongOrNull(),
                online = online != "0",
            )
        }
        .sortedBy { it.index }
        .toList()

    /** Parses `/sys/devices/system/cpu/possible`, e.g. `0-8` or `0-3,6`. */
    fun parseRange(text: String): List<Int> = text.trim().split(',').filter { it.isNotBlank() }.flatMap { part ->
        val bounds = part.split('-').mapNotNull { it.trim().toIntOrNull() }
        when (bounds.size) {
            1 -> listOf(bounds[0])
            2 -> (bounds[0]..bounds[1]).toList()
            else -> emptyList()
        }
    }
}
