package fr.tflag.droidboost.monitor

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import fr.tflag.droidboost.DroidBoostApp
import fr.tflag.droidboost.MainActivity
import fr.tflag.droidboost.R
import fr.tflag.droidboost.core.format.Formats
import fr.tflag.droidboost.system.BatterySnapshot
import fr.tflag.droidboost.system.MemorySnapshot
import fr.tflag.droidboost.system.NetworkSpeed
import fr.tflag.droidboost.system.ThermalSnapshot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/** Foreground service keeping the key metrics in a persistent notification. */
class MonitorService : LifecycleService() {

    private var loop: Job? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        if (intent?.action == ACTION_STOP) {
            // Stopping from the notification also means "don't restart at boot".
            lifecycleScope.launch {
                (application as DroidBoostApp).container.monitorSettings.setEnabled(false)
                stopSelf()
            }
            return START_NOT_STICKY
        }
        startForeground(NOTIFICATION_ID, buildNotification("Démarrage…", ""), ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        // Readings and database writes stay off the main thread.
        if (loop == null) loop = lifecycleScope.launch(Dispatchers.Default) { sampleForever() }
        return START_STICKY
    }

    private suspend fun sampleForever() {
        val container = (application as DroidBoostApp).container
        val thermal = container.newThermalReader()
        val memory = container.newMemoryReader()
        val network = container.newNetworkSpeedReader()
        val recorder = Recorder(container, AlertNotifier(this))
        val notifications = getSystemService(NotificationManager::class.java)
        val config = container.monitorSettings.config.stateIn(lifecycleScope, SharingStarted.Eagerly, MonitorConfig())
        recorder.start()
        network.read()
        while (lifecycleScope.isActive) {
            val battery = container.battery.read()
            val thermalSnapshot = thermal.read(includeHal = false)
            val memorySnapshot = memory.read()
            val speed = network.read()
            runCatching {
                recorder.tick(System.currentTimeMillis(), battery, thermalSnapshot, memorySnapshot, speed, config.value)
            }.onFailure { Log.w(TAG, "Enregistrement de l'historique impossible", it) }
            val title = battery?.let(::batteryLine) ?: "DroidBoost"
            val text = listOfNotNull(
                thermalLine(thermalSnapshot),
                memoryLine(memorySnapshot),
                speed?.let(::networkLine),
            ).joinToString(" · ")
            notifications.notify(NOTIFICATION_ID, buildNotification(title, text))
            delay(REFRESH_MS)
        }
    }

    private fun batteryLine(b: BatterySnapshot): String = buildList {
        add("${b.levelPercent} %")
        b.powerW?.let { add((if (b.isCharging) "+" else "−") + Formats.watts(it)) }
        add(Formats.celsius(b.temperatureC))
    }.joinToString(" · ")

    private fun thermalLine(t: ThermalSnapshot): String =
        "Thermique ${t.status.label.lowercase()}" + (t.headroom?.let { " (${Formats.percent(it)})" } ?: "")

    private fun memoryLine(m: MemorySnapshot): String = "RAM libre ${Formats.bytes(m.availableBytes)}"

    private fun networkLine(n: NetworkSpeed): String =
        "↓ ${Formats.bytesPerSecond(n.rxBytesPerSecond)} ↑ ${Formats.bytesPerSecond(n.txBytesPerSecond)}"

    private fun buildNotification(title: String, text: String): Notification {
        val open = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val stop = PendingIntent.getService(
            this, 1, Intent(this, MonitorService::class.java).setAction(ACTION_STOP), PendingIntent.FLAG_IMMUTABLE,
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_monitor)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setShowWhen(false)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .setContentIntent(open)
            .addAction(0, "Arrêter", stop)
            .build()
    }

    companion object {
        const val CHANNEL_ID = "monitor"
        private const val TAG = "MonitorService"
        private const val NOTIFICATION_ID = 1
        private const val REFRESH_MS = 5_000L
        private const val ACTION_STOP = "fr.tflag.droidboost.action.STOP_MONITOR"

        fun start(context: Context) {
            context.startForegroundService(Intent(context, MonitorService::class.java))
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, MonitorService::class.java))
        }
    }
}
