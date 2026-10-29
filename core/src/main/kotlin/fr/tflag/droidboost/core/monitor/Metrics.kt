package fr.tflag.droidboost.core.monitor

/** Names of the time series stored in the history database. */
object Metrics {
    const val BATTERY_LEVEL = "battery_level"

    /** Signed: positive while charging, negative while discharging. */
    const val BATTERY_CURRENT_MA = "battery_current_ma"
    const val BATTERY_POWER_W = "battery_power_w"
    const val BATTERY_TEMPERATURE = "battery_temp"
    const val BATTERY_VOLTAGE = "battery_voltage"
    const val BATTERY_PLUGGED = "battery_plugged"
    const val THERMAL_STATUS = "thermal_status"
    const val THERMAL_HEADROOM = "thermal_headroom"

    /** Tagged with the sensor type label (Surface, CPU, GPU…); hottest sensor of that type. */
    const val HAL_TEMPERATURE = "hal_temp"
    const val CPU_LOAD = "cpu_load"
    const val RAM_AVAILABLE = "ram_available"
    const val STORAGE_FREE = "storage_free"
    const val NET_RX = "net_rx_bps"
    const val NET_TX = "net_tx_bps"
}

/** Time window shown by history charts, with the bucket used to downsample it. */
enum class HistoryWindow(val label: String, val durationMs: Long, val bucketMs: Long) {
    SIX_HOURS("6 h", 6 * HOUR, 2 * MINUTE),
    DAY("24 h", 24 * HOUR, 5 * MINUTE),
    WEEK("7 j", 7 * 24 * HOUR, 30 * MINUTE),
    MONTH("30 j", 30 * 24 * HOUR, 2 * HOUR),
}

private const val MINUTE = 60_000L
private const val HOUR = 60 * MINUTE
