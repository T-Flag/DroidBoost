package fr.tflag.droidboost.core.monitor

import fr.tflag.droidboost.core.model.ThermalStatus

/** A period during which the system reported thermal throttling (status LIGHT or worse). */
data class ThermalEpisode(
    val startTimestamp: Long,
    val lastTimestamp: Long,
    val maxStatus: ThermalStatus,
    val maxHeadroom: Float?,
    val foregroundPackage: String?,
    val charging: Boolean,
) {
    val durationMs: Long get() = lastTimestamp - startTimestamp
}

sealed interface ThermalEvent {
    data class Started(val episode: ThermalEpisode) : ThermalEvent
    data class Updated(val episode: ThermalEpisode) : ThermalEvent
    data class Finished(val episode: ThermalEpisode) : ThermalEvent
}

class ThermalEpisodeTracker {

    var current: ThermalEpisode? = null
        private set

    fun update(
        timestamp: Long,
        status: ThermalStatus,
        headroom: Float?,
        foregroundPackage: String?,
        charging: Boolean,
    ): ThermalEvent? {
        val episode = current
        val throttling = status.level >= ThermalStatus.LIGHT.level
        return when {
            throttling && episode == null -> {
                val started = ThermalEpisode(timestamp, timestamp, status, headroom, foregroundPackage, charging)
                current = started
                ThermalEvent.Started(started)
            }
            throttling && episode != null -> {
                val updated = episode.copy(
                    lastTimestamp = timestamp,
                    maxStatus = if (status.level > episode.maxStatus.level) status else episode.maxStatus,
                    maxHeadroom = listOfNotNull(episode.maxHeadroom, headroom).maxOrNull(),
                    // Keep the app that was in front when it started heating, unless it was unknown.
                    foregroundPackage = episode.foregroundPackage ?: foregroundPackage,
                    charging = episode.charging || charging,
                )
                current = updated
                ThermalEvent.Updated(updated)
            }
            !throttling && episode != null -> {
                current = null
                ThermalEvent.Finished(episode.copy(lastTimestamp = timestamp))
            }
            else -> null
        }
    }
}
