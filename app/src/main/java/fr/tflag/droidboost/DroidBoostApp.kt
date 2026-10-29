package fr.tflag.droidboost

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import fr.tflag.droidboost.monitor.AlertNotifier
import fr.tflag.droidboost.monitor.MonitorService
import kotlinx.coroutines.launch
import org.lsposed.hiddenapibypass.HiddenApiBypass

class DroidBoostApp : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        // ServiceDumper reaches system services by name through this hidden class.
        runCatching { HiddenApiBypass.addHiddenApiExemptions("Landroid/os/ServiceManager;") }
        container = AppContainer(this)
        container.shell.start()
        container.appScope.launch { runCatching { container.journal.refresh() } }
        getSystemService(NotificationManager::class.java).createNotificationChannels(
            listOf(
                NotificationChannel(MonitorService.CHANNEL_ID, "Surveillance en continu", NotificationManager.IMPORTANCE_LOW).apply {
                    description = "Mesures clés affichées en permanence"
                    setShowBadge(false)
                },
                NotificationChannel(AlertNotifier.CHANNEL_ID, "Alertes", NotificationManager.IMPORTANCE_DEFAULT).apply {
                    description = "Seuil de charge, température de la batterie, bridage thermique"
                },
            ),
        )
    }
}
