package fr.tflag.droidboost.monitor

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.provider.Settings
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import fr.tflag.droidboost.DroidBoostApp
import fr.tflag.droidboost.MainActivity
import fr.tflag.droidboost.R
import fr.tflag.droidboost.core.format.Formats
import fr.tflag.droidboost.system.BatterySnapshot
import fr.tflag.droidboost.system.CpuSnapshot
import fr.tflag.droidboost.system.MemorySnapshot
import fr.tflag.droidboost.system.NetworkSpeed
import fr.tflag.droidboost.system.ThermalSnapshot
import fr.tflag.droidboost.widget.StatsWidget
import androidx.glance.appwidget.updateAll
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Foreground service keeping the key metrics in a persistent notification. */
class MonitorService : LifecycleService() {

    private var loop: Job? = null
    private var pendingJournal: Job? = null

    /** Installs, updates and removals refresh the journal right away (debounced). */
    private val packageReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            pendingJournal?.cancel()
            pendingJournal = lifecycleScope.launch(Dispatchers.Default) {
                delay(PACKAGE_DEBOUNCE_MS)
                runCatching { (application as DroidBoostApp).container.journal.refresh() }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_PACKAGE_ADDED)
            addAction(Intent.ACTION_PACKAGE_REMOVED)
            addAction(Intent.ACTION_PACKAGE_REPLACED)
            addAction(Intent.ACTION_PACKAGE_CHANGED)
            addDataScheme("package")
        }
        registerReceiver(packageReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
    }

    override fun onDestroy() {
        unregisterReceiver(packageReceiver)
        super.onDestroy()
    }

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
        val alerts = AlertNotifier(this)
        val recorder = Recorder(container, alerts)
        val anomalies = AnomalyMonitor(container, alerts)
        val watchdog = ShizukuWatchdog(this, alerts)
        val notifications = getSystemService(NotificationManager::class.java)
        val config = container.monitorSettings.config.stateIn(lifecycleScope, SharingStarted.Eagerly, MonitorConfig())
        lifecycleScope.launch { runOverlay(config) }
        recorder.start()
        network.read()
        var lastJournalAt = 0L
        var lastWidgetAt = 0L
        var shizukuReady = container.shell.isReady
        while (lifecycleScope.isActive) {
            val now = System.currentTimeMillis()
            if (now - lastJournalAt >= JOURNAL_INTERVAL_MS) {
                lastJournalAt = now
                runCatching { container.journal.refresh() }.onFailure { Log.w(TAG, "Journal impossible", it) }
                if (config.value.anomalyAlerts) {
                    runCatching { anomalies.check(now) }.onFailure { Log.w(TAG, "Détection d'anomalies impossible", it) }
                }
            }
            if (container.shell.isReady != shizukuReady) {
                shizukuReady = container.shell.isReady
                runCatching { container.journal.logShizuku(shizukuReady) }
                if (!shizukuReady) watchdog.onShizukuStopped(config.value.keepWirelessDebugging)
            }
            val battery = container.battery.read()
            val thermalSnapshot = thermal.read(includeHal = false)
            val memorySnapshot = memory.read()
            val speed = network.read()
            runCatching {
                recorder.tick(now, battery, thermalSnapshot, memorySnapshot, speed, config.value)
            }.onFailure { Log.w(TAG, "Enregistrement de l'historique impossible", it) }
            val title = battery?.let(::batteryLine) ?: "DroidBoost"
            val text = listOfNotNull(
                thermalLine(thermalSnapshot),
                memoryLine(memorySnapshot),
                speed?.let(::networkLine),
            ).joinToString(" · ")
            notifications.notify(NOTIFICATION_ID, buildNotification(title, text))
            if (now - lastWidgetAt >= WIDGET_INTERVAL_MS) {
                lastWidgetAt = now
                runCatching { StatsWidget().updateAll(this@MonitorService) }
            }
            delay(REFRESH_MS)
        }
    }

    /** Floating monitor, refreshed every second while enabled and allowed to draw over other apps. */
    private suspend fun runOverlay(config: StateFlow<MonitorConfig>) {
        val container = (application as DroidBoostApp).container
        val overlay = OverlayController(this)
        val thermal = container.newThermalReader()
        val cpu = container.newCpuReader()
        try {
            while (lifecycleScope.isActive) {
                if (config.value.overlayEnabled && Settings.canDrawOverlays(this)) {
                    val text = withContext(Dispatchers.Default) { overlayText(container.battery.read(), thermal.read(includeHal = false), cpu.read()) }
                    withContext(Dispatchers.Main) {
                        overlay.show()
                        overlay.update(text)
                    }
                    delay(OVERLAY_REFRESH_MS)
                } else {
                    withContext(Dispatchers.Main) { overlay.hide() }
                    delay(OVERLAY_IDLE_MS)
                }
            }
        } finally {
            withContext(NonCancellable + Dispatchers.Main) { overlay.hide() }
        }
    }

    private fun overlayText(battery: BatterySnapshot?, thermal: ThermalSnapshot, cpu: CpuSnapshot): String {
        val first = battery?.let(::batteryLine) ?: ""
        val maxFreq = cpu.clusters.flatMap { it.cores }.mapNotNull { it.currentKhz }.maxOrNull()
        val second = listOfNotNull(
            cpu.totalLoad?.let { "CPU ${Formats.percent(it)}" },
            maxFreq?.let { Formats.frequencyKhz(it) },
            thermal.status.label + (thermal.headroom?.let { " ${Formats.percent(it)}" } ?: ""),
        ).joinToString(" · ")
        return "$first\n$second"
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
        private const val JOURNAL_INTERVAL_MS = 15 * 60_000L
        private const val PACKAGE_DEBOUNCE_MS = 3_000L
        private const val WIDGET_INTERVAL_MS = 60_000L
        private const val OVERLAY_REFRESH_MS = 1_000L
        private const val OVERLAY_IDLE_MS = 3_000L
        private const val ACTION_STOP = "fr.tflag.droidboost.action.STOP_MONITOR"

        fun start(context: Context) {
            context.startForegroundService(Intent(context, MonitorService::class.java))
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, MonitorService::class.java))
        }
    }
}
