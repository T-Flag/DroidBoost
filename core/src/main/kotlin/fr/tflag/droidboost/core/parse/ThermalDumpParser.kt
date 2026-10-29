package fr.tflag.droidboost.core.parse

import fr.tflag.droidboost.core.model.HalTemperature
import fr.tflag.droidboost.core.model.ThermalDump
import fr.tflag.droidboost.core.model.ThrottlingThresholds

/** Parses the output of `dumpsys thermalservice`. */
object ThermalDumpParser {

    private val TEMPERATURE = Regex(
        "Temperature\\{mValue=([^,]+), mType=(-?\\d+), mName=([^,]+), mStatus=(-?\\d+)\\}",
    )
    private val THRESHOLD = Regex(
        "TemperatureThreshold\\{mType=(-?\\d+), mName=([^,]+), mHotThrottlingThresholds=\\[([^\\]]*)\\]",
    )
    private val STATUS = Regex("^Thermal Status: (\\d+)", RegexOption.MULTILINE)

    private const val HAL_SECTION = "Current temperatures from HAL:"
    private const val CACHED_SECTION = "Cached temperatures:"

    fun parse(dump: String): ThermalDump {
        val lines = dump.lines()
        val halTemps = section(lines, HAL_SECTION).mapNotNull(::parseTemperature)
        val temperatures = halTemps.ifEmpty { section(lines, CACHED_SECTION).mapNotNull(::parseTemperature) }
        val thresholds = lines.mapNotNull { line ->
            THRESHOLD.find(line)?.let { match ->
                val (type, name, values) = match.destructured
                ThrottlingThresholds(
                    name = name.trim(),
                    type = type.toInt(),
                    hot = values.split(',').map { it.trim().toFloatOrNull() ?: Float.NaN },
                )
            }
        }
        val status = STATUS.find(dump)?.groupValues?.get(1)?.toIntOrNull()
        return ThermalDump(status = status, temperatures = temperatures, thresholds = thresholds)
    }

    /** Lines following [header] up to the next non-indented line. */
    private fun section(lines: List<String>, header: String): List<String> {
        val start = lines.indexOfFirst { it.trim() == header }
        if (start < 0) return emptyList()
        return lines.drop(start + 1).takeWhile { it.startsWith("\t") || it.startsWith(" ") }
    }

    private fun parseTemperature(line: String): HalTemperature? {
        val match = TEMPERATURE.find(line) ?: return null
        val (value, type, name, status) = match.destructured
        val celsius = value.trim().toFloatOrNull() ?: return null
        if (celsius.isNaN()) return null
        return HalTemperature(name = name.trim(), type = type.toInt(), celsius = celsius, status = status.toInt())
    }
}
