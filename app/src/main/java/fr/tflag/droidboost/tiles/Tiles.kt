package fr.tflag.droidboost.tiles

import android.app.PendingIntent
import android.content.Intent
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import fr.tflag.droidboost.DroidBoostApp
import fr.tflag.droidboost.MainActivity
import fr.tflag.droidboost.core.format.Formats
import fr.tflag.droidboost.core.shell.CommandOutcome
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Quick Settings tile showing battery and thermal state; tapping it opens the app. */
class MonitorTileService : TileService() {

    private var scope: CoroutineScope? = null

    override fun onStartListening() {
        super.onStartListening()
        val tileScope = CoroutineScope(SupervisorJob() + Dispatchers.Main).also { scope = it }
        tileScope.launch {
            val container = (application as DroidBoostApp).container
            val (battery, thermal) = withContext(Dispatchers.IO) {
                container.battery.read() to container.newThermalReader().read(includeHal = false)
            }
            qsTile?.apply {
                state = Tile.STATE_ACTIVE
                subtitle = listOfNotNull(
                    battery?.let { "${it.levelPercent} % · ${Formats.celsius(it.temperatureC)}" },
                    thermal.status.label,
                ).joinToString(" · ")
                updateTile()
            }
        }
    }

    override fun onStopListening() {
        scope?.cancel()
        scope = null
        super.onStopListening()
    }

    override fun onClick() {
        super.onClick()
        val intent = Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivityAndCollapse(PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE))
    }
}

/** One-tap device-wide cache purge through Shizuku. */
class TrimCachesTileService : TileService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun onStartListening() {
        super.onStartListening()
        val ready = (application as DroidBoostApp).container.shell.isReady
        qsTile?.apply {
            state = if (ready) Tile.STATE_INACTIVE else Tile.STATE_UNAVAILABLE
            subtitle = if (ready) "Shizuku prêt" else "Shizuku requis"
            updateTile()
        }
    }

    override fun onClick() {
        super.onClick()
        val container = (application as DroidBoostApp).container
        if (!container.shell.isReady) return
        qsTile?.apply {
            state = Tile.STATE_ACTIVE
            subtitle = "Vidage…"
            updateTile()
        }
        scope.launch {
            val before = withContext(Dispatchers.IO) { container.storage.totals()?.freeBytes }
            val outcome = container.appActions.trimAllCaches()
            val after = withContext(Dispatchers.IO) { container.storage.totals()?.freeBytes }
            val freed = if (before != null && after != null) (after - before).coerceAtLeast(0) else null
            if (outcome is CommandOutcome.Success) {
                container.journal.logAction(null, "Caches vidés depuis la tuile", freed?.let { "${Formats.bytes(it)} libérés" })
            }
            qsTile?.apply {
                state = Tile.STATE_INACTIVE
                subtitle = when {
                    outcome !is CommandOutcome.Success -> "Échec"
                    freed != null -> "${Formats.bytes(freed)} libérés"
                    else -> "Terminé"
                }
                updateTile()
            }
        }
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }
}
