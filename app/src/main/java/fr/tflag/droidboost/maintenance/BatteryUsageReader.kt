package fr.tflag.droidboost.maintenance

import android.content.Context
import fr.tflag.droidboost.core.maintenance.BatteryStatsParser
import fr.tflag.droidboost.system.ServiceDumper

data class AppBatteryUse(val uid: Int, val label: String, val packageName: String?, val mah: Double)

/** Battery used per app since the last full charge, from `dumpsys batterystats` (DUMP permission or Shizuku). */
class BatteryUsageReader(private val context: Context, private val dumper: ServiceDumper) {

    suspend fun sinceLastCharge(): List<AppBatteryUse>? {
        val dump = dumper.dump("batterystats", listOf("--charged")) ?: dumper.dump("batterystats") ?: return null
        val pm = context.packageManager
        return BatteryStatsParser.parse(dump).map { power ->
            val packages = pm.getPackagesForUid(power.uid).orEmpty()
            val pkg = packages.firstOrNull()
            val label = when {
                power.uid == 0 -> "Noyau et root"
                power.uid == SYSTEM_UID -> "Système Android"
                pkg != null -> runCatching { pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString() }.getOrDefault(pkg)
                else -> "UID ${power.uid}"
            }
            AppBatteryUse(power.uid, label, pkg.takeIf { power.uid >= FIRST_APP_UID }, power.mah)
        }
    }

    /**
     * Capacity learned by the fuel gauge. Filtering the dump on our own package keeps it to ~0.6 MB, and the
     * capacity lines sit in its global header.
     */
    suspend fun learnedCapacityMah(): Int? =
        dumper.dump("batterystats", listOf("--charged", context.packageName))?.let(BatteryStatsParser::learnedCapacityMah)

    private companion object {
        const val SYSTEM_UID = 1000
        const val FIRST_APP_UID = 10_000
    }
}
