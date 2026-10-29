package fr.tflag.droidboost.monitor

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import fr.tflag.droidboost.DroidBoostApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Restarts the persistent notification after a reboot when the user enabled it, and after an app update,
 * which kills the service while the setting stays on.
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val boot = intent.action == Intent.ACTION_BOOT_COMPLETED
        if (!boot && intent.action != Intent.ACTION_MY_PACKAGE_REPLACED) return
        val settings = (context.applicationContext as DroidBoostApp).container.monitorSettings
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val config = settings.current()
                if (boot && config.keepWirelessDebugging) ShizukuWatchdog(context, AlertNotifier(context)).enableWirelessDebugging()
                if (config.enabled) MonitorService.start(context)
            } finally {
                pending.finish()
            }
        }
    }
}
