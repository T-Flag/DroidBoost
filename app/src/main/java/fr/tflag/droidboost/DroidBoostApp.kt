package fr.tflag.droidboost

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import fr.tflag.droidboost.monitor.AlertNotifier
import fr.tflag.droidboost.monitor.MonitorService

class DroidBoostApp : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        container.shell.start()
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
