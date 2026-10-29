package fr.tflag.droidboost.monitor

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import fr.tflag.droidboost.MainActivity
import fr.tflag.droidboost.R

enum class AlertKind(val notificationId: Int) {
    CHARGE_LEVEL(100),
    BATTERY_TEMPERATURE(101),
    THERMAL(102),
    SHIZUKU(103),
    ANOMALY(110),
}

class AlertNotifier(private val context: Context) {

    private val notifications = context.getSystemService(NotificationManager::class.java)

    fun notify(kind: AlertKind, title: String, text: String, id: Int = kind.notificationId, action: Pair<String, Intent>? = null) {
        val open = PendingIntent.getActivity(
            context, kind.notificationId, Intent(context, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_monitor)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setAutoCancel(true)
            .setContentIntent(open)
            .apply {
                action?.let { (label, intent) ->
                    addAction(0, label, PendingIntent.getActivity(context, id, intent, PendingIntent.FLAG_IMMUTABLE))
                }
            }
            .build()
        notifications.notify(id, notification)
    }

    companion object {
        const val CHANNEL_ID = "alerts"
    }
}
