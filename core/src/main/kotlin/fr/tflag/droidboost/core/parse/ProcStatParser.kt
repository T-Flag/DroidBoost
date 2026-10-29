package fr.tflag.droidboost.core.parse

import fr.tflag.droidboost.core.model.CpuTimes

/** Parses `/proc/stat` and derives CPU load between two snapshots. */
object ProcStatParser {

    /** Key [CpuTimes.TOTAL] holds the aggregate `cpu` line; other keys are core indices. */
    fun parse(text: String): Map<Int, CpuTimes> = text.lineSequence()
        .filter { it.startsWith("cpu") }
        .mapNotNull { line ->
            val parts = line.trim().split(Regex("\\s+"))
            val label = parts.first()
            val id = if (label == "cpu") CpuTimes.TOTAL else label.removePrefix("cpu").toIntOrNull() ?: return@mapNotNull null
            // user nice system idle iowait irq softirq steal [guest guest_nice]: guest time is already in user.
            val values = parts.drop(1).take(8).map { it.toLongOrNull() ?: 0L }
            if (values.size < 4) return@mapNotNull null
            val idle = values[3] + values.getOrElse(4) { 0L }
            id to CpuTimes(idle = idle, total = values.sum())
        }
        .toMap()

    /** Busy fraction (0..1) per key present in both snapshots; cores offline in either are skipped. */
    fun load(previous: Map<Int, CpuTimes>, current: Map<Int, CpuTimes>): Map<Int, Float> =
        current.mapNotNull { (id, now) ->
            val before = previous[id] ?: return@mapNotNull null
            val total = now.total - before.total
            val idle = now.idle - before.idle
            if (total <= 0) return@mapNotNull null
            id to ((total - idle).toFloat() / total).coerceIn(0f, 1f)
        }.toMap()
}
