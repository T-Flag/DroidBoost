package fr.tflag.droidboost.data

import fr.tflag.droidboost.core.monitor.HistoryWindow
import kotlinx.coroutines.flow.Flow

data class HistoryStats(val sampleCount: Long, val oldestTimestamp: Long?)

/** Read side of the history database, for the screens. */
class HistoryRepository(private val db: HistoryDatabase) {

    suspend fun series(metric: String, window: HistoryWindow, tag: String = ""): List<SeriesPoint> =
        db.samples().series(metric, tag, System.currentTimeMillis() - window.durationMs, window.bucketMs)

    suspend fun tags(metric: String, window: HistoryWindow): List<String> =
        db.samples().tags(metric, System.currentTimeMillis() - window.durationMs)

    fun chargeSessions(limit: Int = 30): Flow<List<ChargeSessionEntity>> = db.chargeSessions().recent(limit)

    fun thermalEpisodes(limit: Int = 50): Flow<List<ThermalEpisodeEntity>> = db.thermalEpisodes().recent(limit)

    suspend fun capacityEstimates(limit: Int = 10): List<Double> = db.chargeSessions().recentCapacityEstimates(limit)

    suspend fun stats(): HistoryStats = HistoryStats(db.samples().count(), db.samples().oldestTimestamp())
}
