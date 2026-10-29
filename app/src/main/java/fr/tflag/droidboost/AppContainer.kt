package fr.tflag.droidboost

import android.content.Context
import androidx.datastore.preferences.preferencesDataStore
import fr.tflag.droidboost.access.AccessChecker
import fr.tflag.droidboost.apps.AppActions
import fr.tflag.droidboost.apps.AppRepository
import fr.tflag.droidboost.data.HistoryDatabase
import fr.tflag.droidboost.data.HistoryRepository
import fr.tflag.droidboost.journal.JournalRecorder
import fr.tflag.droidboost.maintenance.BatteryUsageReader
import fr.tflag.droidboost.maintenance.DebloatRegistry
import fr.tflag.droidboost.maintenance.StorageCleaner
import fr.tflag.droidboost.monitor.MonitorSettings
import fr.tflag.droidboost.shell.ShizukuShell
import fr.tflag.droidboost.system.BatteryReader
import fr.tflag.droidboost.system.CpuReader
import fr.tflag.droidboost.system.MemoryReader
import fr.tflag.droidboost.system.NetworkSpeedReader
import fr.tflag.droidboost.system.ServiceDumper
import fr.tflag.droidboost.system.StorageReader
import fr.tflag.droidboost.system.ThermalReader
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

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
    val dumper = ServiceDumper(appContext, shell)
    val monitorSettings = MonitorSettings(appContext.settingsStore)
    val history: HistoryDatabase by lazy { HistoryDatabase.create(appContext) }
    val historyRepository: HistoryRepository by lazy { HistoryRepository(history) }
    val journal: JournalRecorder by lazy { JournalRecorder(appContext, history, appContext.settingsStore) }
    val debloatRegistry = DebloatRegistry(appContext.settingsStore)
    val cleaner = StorageCleaner(appContext, shell)
    val batteryUsage = BatteryUsageReader(appContext, dumper)

    /** Work that must outlive a screen (journal refresh after an action, export). */
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    // Readers keeping state between samples: one instance per consumer.
    fun newThermalReader() = ThermalReader(appContext, dumper)
    fun newCpuReader() = CpuReader(shell)
    fun newMemoryReader() = MemoryReader(appContext, shell)
    fun newNetworkSpeedReader() = NetworkSpeedReader()

    val packageName: String = appContext.packageName
}
