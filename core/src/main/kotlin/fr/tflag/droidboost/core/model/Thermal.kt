package fr.tflag.droidboost.core.model

data class HalTemperature(
    val name: String,
    /** One of the `android.os.Temperature.TYPE_*` constants. */
    val type: Int,
    val celsius: Float,
    /** Throttling severity reported for this sensor, same scale as [ThermalStatus]. */
    val status: Int,
)

data class ThrottlingThresholds(
    val name: String,
    val type: Int,
    /** Temperature at which each severity level starts, indexed by [ThermalStatus.level]. */
    val hot: List<Float>,
) {
    /** First throttling threshold (LIGHT), or null when the sensor never throttles. */
    val firstThrottle: Float? get() = hot.getOrNull(ThermalStatus.LIGHT.level)?.takeUnless { it.isNaN() }
}

data class ThermalDump(
    val status: Int?,
    val temperatures: List<HalTemperature>,
    val thresholds: List<ThrottlingThresholds>,
) {
    fun thresholdFor(sensor: HalTemperature): ThrottlingThresholds? =
        thresholds.firstOrNull { it.name == sensor.name && it.type == sensor.type }
}

/** Mirrors `PowerManager.THERMAL_STATUS_*`. */
enum class ThermalStatus(val level: Int, val label: String) {
    NONE(0, "Normal"),
    LIGHT(1, "Léger"),
    MODERATE(2, "Modéré"),
    SEVERE(3, "Sévère"),
    CRITICAL(4, "Critique"),
    EMERGENCY(5, "Urgence"),
    SHUTDOWN(6, "Arrêt imminent"),
    ;

    companion object {
        fun of(level: Int): ThermalStatus = entries.firstOrNull { it.level == level } ?: NONE
    }
}

/** French labels for `android.os.Temperature.TYPE_*`. */
object TemperatureTypes {
    private val LABELS = mapOf(
        -1 to "Inconnu",
        0 to "CPU",
        1 to "GPU",
        2 to "Batterie",
        3 to "Surface",
        4 to "Port USB",
        5 to "Amplificateur radio",
        6 to "Tension (BCL)",
        7 to "Courant (BCL)",
        8 to "Charge (BCL)",
        9 to "NPU",
        10 to "TPU",
        11 to "Écran",
        12 to "Modem",
        13 to "SoC",
        14 to "Wi-Fi",
        15 to "Caméra",
        16 to "Flash",
        17 to "Haut-parleur",
        18 to "Ambiante",
        19 to "Pogo",
    )

    const val UNKNOWN = -1

    fun label(type: Int): String = LABELS[type] ?: "Type $type"

    /** "BIG · CPU" for typed sensors; vendor sensors of unknown type (most of the Pixel's) keep their own name. */
    fun displayName(sensor: HalTemperature): String =
        if (sensor.type == UNKNOWN) sensor.name else "${sensor.name} · ${label(sensor.type)}"

    /** BCL sensors report volts, amps or percent, not degrees. */
    fun isTemperature(type: Int): Boolean = type !in 6..8
}
