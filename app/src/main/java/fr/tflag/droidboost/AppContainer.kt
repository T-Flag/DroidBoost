package fr.tflag.droidboost

import android.content.Context
import androidx.datastore.preferences.preferencesDataStore
import fr.tflag.droidboost.access.AccessChecker
import fr.tflag.droidboost.apps.AppActions
import fr.tflag.droidboost.apps.AppRepository
import fr.tflag.droidboost.data.HistoryDatabase
import fr.tflag.droidboost.data.HistoryRepository
import fr.tflag.droidboost.monitor.MonitorSettings
import fr.tflag.droidboost.shell.ShizukuShell
import fr.tflag.droidboost.system.BatteryReader
import fr.tflag.droidboost.system.CpuReader
import fr.tflag.droidboost.system.MemoryReader
import fr.tflag.droidboost.system.NetworkSpeedReader
import fr.tflag.droidboost.system.StorageReader
import fr.tflag.droidboost.system.ThermalReader

private val Context.settingsStore by preferencesDataStore(name = "settings")

/** Manual dependency graph, created once by [DroidBoostApp]. */
class AppContainer(context: Context) {
    val appContext: Context = context.applicationContext

    val shell = ShizukuShell(appContext)
    val access = AccessChecker(appContext)
    val storage = StorageReader(appContext)
    val apps = AppRepository(appContext, storage)
    val appActions = AppActions(shell)
    val battery = BatteryReader(appContext)
    val monitorSettings = MonitorSettings(appContext.settingsStore)
    val history: HistoryDatabase by lazy { HistoryDatabase.create(appContext) }
    val historyRepository: HistoryRepository by lazy { HistoryRepository(history) }

    // Readers keeping state between samples: one instance per consumer.
    fun newThermalReader() = ThermalReader(appContext, shell)
    fun newCpuReader() = CpuReader(shell)
    fun newMemoryReader() = MemoryReader(appContext, shell)
    fun newNetworkSpeedReader() = NetworkSpeedReader()

    val packageName: String = appContext.packageName
}
