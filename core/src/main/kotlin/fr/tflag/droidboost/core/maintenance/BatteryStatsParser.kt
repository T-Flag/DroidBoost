package fr.tflag.droidboost.core.maintenance

data class UidPower(
    /** Linux uid (u0a123 → 10123); system and shared uids keep their number. */
    val uid: Int,
    val mah: Double,
)

/**
 * Reads the "Estimated power use (mAh)" section of `dumpsys batterystats`, e.g.
 * `UID u0a221: 123 fg: 100 bg: 20 ( cpu=… )` or `Uid 1000: 45.2 ( … )`.
 * Only the first number after the uid is used, which has stayed stable across Android versions.
 */
object BatteryStatsParser {

    private val UID_LINE = Regex("^\\s*(?:UID|Uid)\\s+(u(\\d+)a(\\d+)|u(\\d+)s(\\d+)|\\d+):\\s*([\\d.]+)")

    fun parse(dump: String): List<UidPower> {
        val lines = dump.lines()
        val header = lines.indexOfFirst { it.trimStart().startsWith("Estimated power use") }
        if (header < 0) return emptyList()
        val headerIndent = indent(lines[header])
        // The section ends at the next line indented like (or less than) its header.
        return lines.asSequence()
            .drop(header + 1)
            .takeWhile { it.isBlank() || indent(it) > headerIndent }
            .mapNotNull { line ->
                val match = UID_LINE.find(line) ?: return@mapNotNull null
                val uid = toUid(match.groupValues[1]) ?: return@mapNotNull null
                val mah = match.groupValues[6].toDoubleOrNull() ?: return@mapNotNull null
                UidPower(uid, mah)
            }
            .groupBy { it.uid }
            .map { (uid, entries) -> UidPower(uid, entries.sumOf { it.mah }) }
            .sortedByDescending { it.mah }
    }

    private fun indent(line: String): Int = line.length - line.trimStart().length

    /** `u0a123` → 10123, `u0s1000`-style system ids keep their app id, plain numbers stay as is. */
    fun toUid(label: String): Int? {
        Regex("^u(\\d+)a(\\d+)$").find(label)?.let { m ->
            return m.groupValues[1].toInt() * PER_USER_RANGE + FIRST_APPLICATION_UID + m.groupValues[2].toInt()
        }
        Regex("^u(\\d+)s(\\d+)$").find(label)?.let { m ->
            return m.groupValues[1].toInt() * PER_USER_RANGE + m.groupValues[2].toInt()
        }
        return label.toIntOrNull()
    }

    private const val PER_USER_RANGE = 100_000
    private const val FIRST_APPLICATION_UID = 10_000
}
