package fr.tflag.droidboost.monitor

import fr.tflag.droidboost.AppContainer
import fr.tflag.droidboost.core.journal.JournalTypes
import fr.tflag.droidboost.core.monitor.Anomaly
import fr.tflag.droidboost.core.monitor.AnomalyDetector
import fr.tflag.droidboost.core.monitor.BatteryHealth
import fr.tflag.droidboost.core.monitor.Metrics
import fr.tflag.droidboost.data.JournalEventEntity

/** Periodically compares recent history with the phone's own baseline and reports what stands out. */
class AnomalyMonitor(private val container: AppContainer, private val alerts: AlertNotifier) {

    private val lastReported = HashMap<String, Long>()

    suspend fun check(now: Long) {
        val anomalies = buildList {
            addAll(listOfNotNull(drain(now), storage(now)))
            addAll(crashes(now))
            addAll(growth(now))
        }
        anomalies.filter { now - (lastReported[it.key] ?: 0L) >= REPEAT_AFTER_MS }.forEach { anomaly ->
            lastReported[anomaly.key] = now
            alerts.notify(AlertKind.ANOMALY, anomaly.title, anomaly.details, id = AlertKind.ANOMALY.notificationId + (anomaly.key.hashCode() and 0xFF))
            container.history.journal().insertAll(
                listOf(JournalEventEntity(ts = now, type = JournalTypes.ANOMALY, title = anomaly.title, details = anomaly.details)),
            )
        }
    }

    private suspend fun drain(now: Long): Anomaly? {
        val samples = container.history.samples()
        val start = now - BASELINE_MS
        val levels = samples.seriesBetween(Metrics.BATTERY_LEVEL, "", start, now - HOUR_MS, BUCKET_MS).map { it.bucket to it.value }
        val plugged = samples.seriesBetween(Metrics.BATTERY_PLUGGED, "", start, now - HOUR_MS, BUCKET_MS).map { it.bucket to it.value }
        val baseline = AnomalyDetector.hourlyRates(levels, plugged)
        val recentLevels = samples.seriesBetween(Metrics.BATTERY_LEVEL, "", now - HOUR_MS, now, BUCKET_MS).map { it.bucket to it.value }
        val recentPlugged = samples.seriesBetween(Metrics.BATTERY_PLUGGED, "", now - HOUR_MS, now, BUCKET_MS).map { it.bucket to it.value }
        return AnomalyDetector.drain(BatteryHealth.dischargeRatePerHour(recentLevels, recentPlugged), baseline)
    }

    private suspend fun storage(now: Long): Anomaly? {
        val samples = container.history.samples()
        val before = samples.average(Metrics.STORAGE_FREE, "", now - 75 * MINUTE_MS, now - 45 * MINUTE_MS)
        val current = samples.average(Metrics.STORAGE_FREE, "", now - 15 * MINUTE_MS, now + MINUTE_MS)
        return AnomalyDetector.storageDrop(before, current)
    }

    private suspend fun crashes(now: Long): List<Anomaly> {
        val counts = container.history.journal().topPackages(JournalTypes.EXIT, now - HOUR_MS, 10).associate { it.packageName to it.count }
        return AnomalyDetector.crashBurst(counts, labels = ::label)
    }

    private suspend fun growth(now: Long): List<Anomaly> {
        val rows = container.history.samples().growth(Metrics.APP_DATA, now - DAY_MS)
        val growth = rows.mapNotNull { row ->
            val latest = row.latest ?: return@mapNotNull null
            val previous = row.previous ?: return@mapNotNull null
            row.tag to (latest - previous)
        }.toMap()
        return AnomalyDetector.appGrowth(growth, labels = ::label)
    }

    private fun label(packageName: String): String = runCatching {
        val pm = container.appContext.packageManager
        pm.getApplicationLabel(pm.getApplicationInfo(packageName, 0)).toString()
    }.getOrDefault(packageName)

    private companion object {
        const val MINUTE_MS = 60_000L
        const val HOUR_MS = 60 * MINUTE_MS
        const val DAY_MS = 24 * HOUR_MS
        const val BASELINE_MS = 7 * DAY_MS
        const val BUCKET_MS = 5 * MINUTE_MS
        const val REPEAT_AFTER_MS = 6 * HOUR_MS
    }
}
