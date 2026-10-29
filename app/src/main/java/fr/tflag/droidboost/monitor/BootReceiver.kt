package fr.tflag.droidboost.monitor

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import fr.tflag.droidboost.DroidBoostApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/** Restarts the persistent notification after a reboot when the user enabled it. */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        val settings = (context.applicationContext as DroidBoostApp).container.monitorSettings
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                if (settings.isEnabled()) MonitorService.start(context)
            } finally {
                pending.finish()
            }
        }
    }
}
