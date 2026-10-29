package fr.tflag.droidboost.core.monitor

data class Anomaly(val key: String, val title: String, val details: String)

/**
 * Rules comparing the recent past with this phone's own baseline, so alerts adapt to how it is used
 * instead of relying on generic thresholds.
 */
object AnomalyDetector {

    /**
     * Battery draining much faster than usual while unplugged.
     * [recentRatePerHour] covers the last hour; [baselineRates] are hourly rates from previous days.
     */
    fun drain(recentRatePerHour: Double?, baselineRates: List<Double>): Anomaly? {
        val recent = recentRatePerHour ?: return null
        val baseline = BatteryHealth.medianEstimate(baselineRates.filter { it >= 0 }) ?: return null
        if (baselineRates.size < MIN_BASELINE_HOURS) return null
        val threshold = maxOf(baseline * 2, baseline + 3.0, MIN_ALERT_DRAIN)
        if (recent < threshold) return null
        return Anomaly(
            key = "drain",
            title = "Décharge anormale",
            details = "%.1f %%/h sur la dernière heure, contre %.1f %%/h d'habitude.".format(recent, baseline),
        )
    }

    /** Free space dropping by more than [minDropBytes] between two readings about an hour apart. */
    fun storageDrop(freeBefore: Double?, freeNow: Double?, minDropBytes: Double = 2.0 * GIB): Anomaly? {
        if (freeBefore == null || freeNow == null) return null
        val drop = freeBefore - freeNow
        if (drop < minDropBytes) return null
        return Anomaly("storage", "Espace disque consommé rapidement", "%.1f Go utilisés en une heure.".format(drop / GIB))
    }

    /** Same app dying [minCount] times or more within the window (crash loop, repeated ANR or memory kills). */
    fun crashBurst(exitsByPackage: Map<String, Int>, minCount: Int = 3, labels: (String) -> String): List<Anomaly> =
        exitsByPackage.filterValues { it >= minCount }.map { (pkg, count) ->
            Anomaly("crash:$pkg", "${labels(pkg)} plante en boucle", "$count arrêts anormaux en une heure.")
        }

    /** An app whose data grew by more than [minGrowthBytes] in a day. */
    fun appGrowth(growthByApp: Map<String, Double>, minGrowthBytes: Double = 1.0 * GIB, labels: (String) -> String): List<Anomaly> =
        growthByApp.filterValues { it >= minGrowthBytes }.map { (pkg, growth) ->
            Anomaly("growth:$pkg", "${labels(pkg)} grossit vite", "+%.1f Go de données en 24 h.".format(growth / GIB))
        }

    /** Hourly discharge rates (%/h) from unplugged level samples, one value per full hour of data. */
    fun hourlyRates(levels: List<Pair<Long, Double>>, plugged: List<Pair<Long, Double>>): List<Double> {
        val pluggedHours = plugged.filter { it.second > 0.5 }.map { it.first / HOUR_MS }.toSet()
        return levels.groupBy { it.first / HOUR_MS }
            .filterKeys { it !in pluggedHours }
            .values
            .mapNotNull { hour ->
                if (hour.size < 2) return@mapNotNull null
                val first = hour.minBy { it.first }
                val last = hour.maxBy { it.first }
                val hours = (last.first - first.first) / HOUR_MS.toDouble()
                if (hours < 0.5) null else ((first.second - last.second) / hours).takeIf { it >= 0 }
            }
    }

    private const val MIN_BASELINE_HOURS = 12
    private const val MIN_ALERT_DRAIN = 4.0
    private const val GIB = 1024.0 * 1024 * 1024
    private const val HOUR_MS = 3_600_000L
}
