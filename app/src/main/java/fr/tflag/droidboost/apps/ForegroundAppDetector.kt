package fr.tflag.droidboost.apps

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context

/** Package currently in the foreground, from usage events (needs usage access). */
class ForegroundAppDetector(context: Context) {

    private val usage = context.getSystemService(UsageStatsManager::class.java)

    fun current(): String? = runCatching {
        val end = System.currentTimeMillis()
        val events = usage.queryEvents(end - LOOKBACK_MS, end)
        val event = UsageEvents.Event()
        var resumed: String? = null
        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            when (event.eventType) {
                UsageEvents.Event.ACTIVITY_RESUMED -> resumed = event.packageName
                UsageEvents.Event.ACTIVITY_PAUSED -> if (event.packageName == resumed) resumed = null
            }
        }
        resumed
    }.getOrNull()

    private companion object {
        const val LOOKBACK_MS = 30 * 60_000L
    }
}
