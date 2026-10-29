package fr.tflag.droidboost.monitor

import fr.tflag.droidboost.AppContainer
import fr.tflag.droidboost.apps.ForegroundAppDetector
import fr.tflag.droidboost.core.format.Formats
import fr.tflag.droidboost.core.model.TemperatureTypes
import fr.tflag.droidboost.core.model.ThermalStatus
import fr.tflag.droidboost.core.monitor.ChargeEvent
import fr.tflag.droidboost.core.monitor.ChargeLevelAlert
import fr.tflag.droidboost.core.monitor.ChargeSession
import fr.tflag.droidboost.core.monitor.ChargeSessionTracker
import fr.tflag.droidboost.core.monitor.Metrics
import fr.tflag.droidboost.core.monitor.PowerReading
import fr.tflag.droidboost.core.monitor.ThermalEpisode
import fr.tflag.droidboost.core.monitor.ThermalEpisodeTracker
import fr.tflag.droidboost.core.monitor.ThermalEvent
import fr.tflag.droidboost.core.monitor.ThresholdAlert
import fr.tflag.droidboost.core.parse.ThermalDumpParser
import fr.tflag.droidboost.data.ChargeSessionEntity
import fr.tflag.droidboost.data.SampleEntity
import fr.tflag.droidboost.data.ThermalEpisodeEntity
import fr.tflag.droidboost.system.BatterySnapshot
import fr.tflag.droidboost.system.MemorySnapshot
import fr.tflag.droidboost.system.NetworkSpeed
import fr.tflag.droidboost.system.PowerRailsReader
import fr.tflag.droidboost.system.ThermalSnapshot
import java.util.concurrent.TimeUnit

/**
 * Turns the monitoring loop's readings into history rows, charge sessions, thermal episodes and alerts.
 * Called by [MonitorService] on every tick.
 */
class Recorder(private val container: AppContainer, private val alerts: AlertNotifier) {

    private val db = container.history
    private val chargeTracker = ChargeSessionTracker()
    private val thermalTracker = ThermalEpisodeTracker()
    private val foreground = ForegroundAppDetector(container.appContext)
    private val cpu = container.newCpuReader()

    private var chargeSessionId: Long? = null
    private var thermalEpisodeId: Long? = null
    private var lastSessionWriteAt = 0L
    private var lastEpisodeWriteAt = 0L
    private var lastRecordAt = 0L
    private var lastPurgeAt = 0L
    private var lastAppSizesAt = 0L
    private val rails = PowerRailsReader(container.appContext)

    private var chargeAlert: Pair<Int, ChargeLevelAlert>? = null
    private var temperatureAlert: Pair<Int, ThresholdAlert>? = null
    private val thermalAlert = ThresholdAlert(ThermalStatus.SEVERE.level.toFloat(), hysteresis = 2f)

    suspend fun start() {
        db.chargeSessions().closeDangling()
        db.thermalEpisodes().closeDangling()
    }

    suspend fun tick(
        now: Long,
        battery: BatterySnapshot?,
        thermal: ThermalSnapshot,
        memory: MemorySnapshot,
        network: NetworkSpeed?,
        config: MonitorConfig,
    ) {
        battery?.let {
            trackCharge(now, it)
            checkBatteryAlerts(it, config)
        }
        trackThermal(now, thermal, battery?.isCharging == true, config)
        if (now - lastRecordAt >= config.recordIntervalSec * 1000L) {
            lastRecordAt = now
            record(now, battery, thermal, memory, network)
        }
        if (now - lastAppSizesAt >= APP_SIZES_INTERVAL_MS) {
            lastAppSizesAt = now
            recordAppSizes(now)
        }
        if (now - lastPurgeAt >= PURGE_INTERVAL_MS) {
            lastPurgeAt = now
            purge(now, config.retentionDays)
        }
    }

    private suspend fun trackCharge(now: Long, battery: BatterySnapshot) {
        val reading = PowerReading(
            timestamp = now,
            plugged = battery.plugged != 0,
            levelPercent = battery.levelPercent,
            chargeCounterMah = battery.chargeCounterMah,
            currentMa = battery.currentMa,
            powerW = battery.powerW,
            temperatureC = battery.temperatureC,
        )
        val dao = db.chargeSessions()
        when (val event = chargeTracker.update(reading)) {
            is ChargeEvent.Started -> {
                chargeSessionId = dao.insert(event.session.toEntity(id = 0, ended = false))
                lastSessionWriteAt = now
            }
            is ChargeEvent.Updated -> if (now - lastSessionWriteAt >= SESSION_WRITE_INTERVAL_MS) {
                chargeSessionId?.let { dao.update(event.session.toEntity(it, ended = false)) }
                lastSessionWriteAt = now
            }
            is ChargeEvent.Finished -> chargeSessionId?.let { dao.update(event.session.toEntity(it, ended = true)) }
            is ChargeEvent.Discarded -> chargeSessionId?.let { dao.delete(it) }
            null -> Unit
        }
        if (chargeTracker.current == null) chargeSessionId = null
    }

    private suspend fun trackThermal(now: Long, thermal: ThermalSnapshot, charging: Boolean, config: MonitorConfig) {
        // Usage events are only scanned while the device is throttling, to keep idle ticks cheap.
        val throttling = thermal.status.level >= ThermalStatus.LIGHT.level
        val app = if (throttling && thermalTracker.current?.foregroundPackage == null) foreground.current() else null
        val dao = db.thermalEpisodes()
        when (val event = thermalTracker.update(now, thermal.status, thermal.headroom, app, charging)) {
            is ThermalEvent.Started -> {
                thermalEpisodeId = dao.insert(event.episode.toEntity(id = 0, ended = false))
                lastEpisodeWriteAt = now
            }
            is ThermalEvent.Updated -> if (now - lastEpisodeWriteAt >= SESSION_WRITE_INTERVAL_MS) {
                thermalEpisodeId?.let { dao.update(event.episode.toEntity(it, ended = false)) }
                lastEpisodeWriteAt = now
            }
            is ThermalEvent.Finished -> thermalEpisodeId?.let { dao.update(event.episode.toEntity(it, ended = true)) }
            null -> Unit
        }
        if (thermalTracker.current == null) thermalEpisodeId = null

        if (config.thermalAlert && thermalAlert.check(thermal.status.level.toFloat())) {
            val culprit = thermalTracker.current?.foregroundPackage?.let { " pendant l'utilisation de ${label(it)}" }.orEmpty()
            alerts.notify(
                AlertKind.THERMAL,
                "Bridage thermique ${thermal.status.label.lowercase()}",
                "Le processeur est fortement ralenti pour refroidir le téléphone$culprit.",
            )
        }
    }

    private fun checkBatteryAlerts(battery: BatterySnapshot, config: MonitorConfig) {
        if (config.chargeAlertPercent > 0) {
            val alert = chargeAlert?.takeIf { it.first == config.chargeAlertPercent }?.second
                ?: ChargeLevelAlert(config.chargeAlertPercent).also { chargeAlert = config.chargeAlertPercent to it }
            if (alert.check(battery.plugged != 0, battery.levelPercent)) {
                alerts.notify(AlertKind.CHARGE_LEVEL, "Batterie à ${battery.levelPercent} %", "Vous pouvez débrancher le chargeur.")
            }
        }
        if (config.batteryTempAlertC > 0) {
            val alert = temperatureAlert?.takeIf { it.first == config.batteryTempAlertC }?.second
                ?: ThresholdAlert(config.batteryTempAlertC.toFloat(), hysteresis = 2f)
                    .also { temperatureAlert = config.batteryTempAlertC to it }
            if (alert.check(battery.temperatureC)) {
                alerts.notify(
                    AlertKind.BATTERY_TEMPERATURE,
                    "Batterie à ${Formats.celsius(battery.temperatureC)}",
                    if (battery.isCharging) "Elle chauffe pendant la charge : retirez la coque ou mettez le téléphone à l'ombre."
                    else "Une chaleur prolongée use la batterie plus vite.",
                )
            }
        }
    }

    private suspend fun record(
        now: Long,
        battery: BatterySnapshot?,
        thermal: ThermalSnapshot,
        memory: MemorySnapshot,
        network: NetworkSpeed?,
    ) {
        val rows = mutableListOf<SampleEntity>()
        fun add(metric: String, value: Double, tag: String = "") {
            rows += SampleEntity(ts = now, metric = metric, tag = tag, value = value)
        }
        battery?.let { b ->
            val sign = if (b.isCharging) 1 else -1
            add(Metrics.BATTERY_LEVEL, b.levelPercent.toDouble())
            b.currentMa?.let { add(Metrics.BATTERY_CURRENT_MA, sign * it) }
            b.powerW?.let { add(Metrics.BATTERY_POWER_W, sign * it) }
            add(Metrics.BATTERY_TEMPERATURE, b.temperatureC.toDouble())
            add(Metrics.BATTERY_VOLTAGE, b.voltageMv.toDouble())
            add(Metrics.BATTERY_PLUGGED, if (b.plugged != 0) 1.0 else 0.0)
        }
        add(Metrics.THERMAL_STATUS, thermal.status.level.toDouble())
        thermal.headroom?.let { add(Metrics.THERMAL_HEADROOM, it.toDouble()) }
        add(Metrics.RAM_AVAILABLE, memory.availableBytes.toDouble())
        container.storage.totals()?.let { add(Metrics.STORAGE_FREE, it.freeBytes.toDouble()) }
        network?.let {
            add(Metrics.NET_RX, it.rxBytesPerSecond.toDouble())
            add(Metrics.NET_TX, it.txBytesPerSecond.toDouble())
        }
        if (container.shell.isReady) {
            // Average load since the previous record, since the reader diffs consecutive /proc/stat snapshots.
            cpu.read().totalLoad?.let { add(Metrics.CPU_LOAD, it.toDouble()) }
        }
        container.dumper.dump("thermalservice")?.let(ThermalDumpParser::parse)?.temperatures
            ?.filter { TemperatureTypes.isTemperature(it.type) }
            ?.groupBy { TemperatureTypes.label(it.type) }
            ?.forEach { (type, sensors) -> add(Metrics.HAL_TEMPERATURE, sensors.maxOf { it.celsius }.toDouble(), tag = type) }
        rails.read().forEach { add(Metrics.POWER_RAIL, it.watts, tag = it.name) }
        db.samples().insertAll(rows)
    }

    /** Cache and data size of every app, to spot the ones that keep growing. */
    private suspend fun recordAppSizes(now: Long) {
        if (!container.access.hasUsageStats()) return
        val rows = container.apps.loadAll().flatMap { app ->
            val size = app.size ?: return@flatMap emptyList()
            listOf(
                SampleEntity(ts = now, metric = Metrics.APP_CACHE, tag = app.packageName, value = size.cacheBytes.toDouble()),
                SampleEntity(ts = now, metric = Metrics.APP_DATA, tag = app.packageName, value = size.dataBytes.toDouble()),
            )
        }
        db.samples().insertAll(rows)
    }

    private suspend fun purge(now: Long, retentionDays: Int) {
        db.samples().deleteBefore(now - TimeUnit.DAYS.toMillis(retentionDays.toLong()))
        val eventsBefore = now - TimeUnit.DAYS.toMillis(EVENT_RETENTION_DAYS)
        db.chargeSessions().deleteBefore(eventsBefore)
        db.thermalEpisodes().deleteBefore(eventsBefore)
    }

    private fun label(packageName: String): String = runCatching {
        val pm = container.appContext.packageManager
        pm.getApplicationLabel(pm.getApplicationInfo(packageName, 0)).toString()
    }.getOrDefault(packageName)

    private fun ChargeSession.toEntity(id: Long, ended: Boolean) = ChargeSessionEntity(
        id = id,
        startTs = startTimestamp,
        endTs = if (ended) lastTimestamp else null,
        lastTs = lastTimestamp,
        startLevel = startLevel,
        endLevel = endLevel,
        addedMah = addedMah,
        estimatedCapacityMah = estimatedCapacityMah,
        averagePowerW = averagePowerW,
        maxPowerW = maxPowerW,
        maxTemperatureC = maxTemperatureC,
    )

    private fun ThermalEpisode.toEntity(id: Long, ended: Boolean) = ThermalEpisodeEntity(
        id = id,
        startTs = startTimestamp,
        endTs = if (ended) lastTimestamp else null,
        lastTs = lastTimestamp,
        maxStatus = maxStatus.level,
        maxHeadroom = maxHeadroom,
        foregroundPackage = foregroundPackage,
        charging = charging,
    )

    private companion object {
        const val SESSION_WRITE_INTERVAL_MS = 30_000L
        const val PURGE_INTERVAL_MS = 6 * 3_600_000L
        const val APP_SIZES_INTERVAL_MS = 6 * 3_600_000L
        const val EVENT_RETENTION_DAYS = 365L
    }
}
