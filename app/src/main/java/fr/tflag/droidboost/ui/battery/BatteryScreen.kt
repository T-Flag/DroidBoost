package fr.tflag.droidboost.ui.battery

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BatteryAlert
import androidx.compose.material.icons.outlined.ElectricBolt
import androidx.compose.material.icons.outlined.HealthAndSafety
import androidx.compose.material.icons.outlined.ShowChart
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import fr.tflag.droidboost.core.format.Formats
import fr.tflag.droidboost.core.monitor.BatteryHealth
import fr.tflag.droidboost.core.monitor.ChargeSession
import fr.tflag.droidboost.data.ChargeSessionEntity
import fr.tflag.droidboost.ui.components.LevelBar
import fr.tflag.droidboost.ui.components.LineChart
import fr.tflag.droidboost.ui.components.MetricRow
import fr.tflag.droidboost.ui.components.MissingAccessHint
import fr.tflag.droidboost.ui.components.SectionCard
import fr.tflag.droidboost.ui.components.WindowSelector
import fr.tflag.droidboost.ui.theme.critical
import fr.tflag.droidboost.ui.theme.ok
import fr.tflag.droidboost.ui.theme.warning
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val SESSION_DATE = DateTimeFormatter.ofPattern("EEE d MMM, HH:mm", Locale.FRANCE)
private val ONE_DECIMAL = DecimalFormat("0.0", DecimalFormatSymbols(Locale.FRANCE))

@Composable
fun BatteryScreen(
    viewModel: BatteryViewModel,
    contentPadding: PaddingValues,
    onOpenAccess: () -> Unit,
    onOpenDashboard: () -> Unit,
    onOpenApp: (String) -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = contentPadding.calculateTopPadding() + 8.dp,
            bottom = contentPadding.calculateBottomPadding() + 16.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item { HealthCard(state, onOpenAccess) }
        item { AppUsageCard(state, viewModel::refreshAppUsage, onOpenApp, onOpenAccess) }
        if (state.railsSupported) item { RailsCard(state) }
        item {
            SectionCard("Historique", Icons.Outlined.ShowChart) {
                if (!state.monitorEnabled) {
                    MissingAccessHint(
                        "L'historique s'enregistre quand la notification permanente est active.",
                        "Activer",
                        onOpenDashboard,
                    )
                }
                WindowSelector(state.window, viewModel::setWindow)
                state.history.dischargePerHour?.let {
                    MetricRow("Décharge moyenne (débranché)", "${ONE_DECIMAL.format(it)} %/h")
                }
                Text("Niveau", style = MaterialTheme.typography.labelLarge)
                LineChart(state.history.level, state.window, { "${it.toInt()} %" }, fixedMin = 0.0, fixedMax = 100.0)
                Text("Puissance (+ charge, − décharge)", style = MaterialTheme.typography.labelLarge)
                LineChart(state.history.power, state.window, { Formats.watts(it) }, color = MaterialTheme.colorScheme.tertiary, reference = 0.0)
                Text("Température", style = MaterialTheme.typography.labelLarge)
                LineChart(
                    state.history.temperature,
                    state.window,
                    { Formats.celsius(it.toFloat()) },
                    color = MaterialTheme.colorScheme.error,
                    reference = 40.0,
                )
            }
        }
        item {
            SectionCard("Sessions de charge", Icons.Outlined.ElectricBolt) {
                if (state.sessions.isEmpty()) {
                    Text(
                        "Aucune session enregistrée pour l'instant.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        items(state.sessions, key = { it.id }) { session -> SessionRow(session) }
    }
}

@Composable
private fun HealthCard(state: BatteryScreenState, onOpenAccess: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val battery = state.battery
    SectionCard("Santé de la batterie", Icons.Outlined.HealthAndSafety) {
        val design = state.designCapacityMah
        design?.let { MetricRow("Capacité d'origine", "${Formats.number(it.toLong())} mAh") }
        val stateOfHealth = battery?.stateOfHealthPercent
        when {
            stateOfHealth != null -> {
                MetricRow("Santé déclarée par le système", "$stateOfHealth %")
                LevelBar(stateOfHealth / 100f, color = healthColor(stateOfHealth / 100f))
            }
            !state.hasBatteryStats -> MissingAccessHint("La santé officielle demande BATTERY_STATS.", "Accorder", onOpenAccess)
            else -> MetricRow("Santé déclarée par le système", "Non communiquée")
        }
        state.learnedCapacityMah?.let { learned ->
            val health = design?.let { " · ${Formats.percent(BatteryHealth.healthFraction(learned.toDouble(), it))}" }.orEmpty()
            MetricRow("Capacité apprise par la jauge", "${Formats.number(learned.toLong())} mAh$health")
        }
        state.gaugeCapacityMah?.let { gauge ->
            val health = design?.let { " · ${Formats.percent(BatteryHealth.healthFraction(gauge, it))}" }.orEmpty()
            MetricRow("Capacité d'après la charge restante", "${Formats.number(gauge.toLong())} mAh$health")
        }
        state.sessionCapacityMah?.let { estimate ->
            val health = design?.let { " · ${Formats.percent(BatteryHealth.healthFraction(estimate, it))}" }.orEmpty()
            MetricRow("Capacité mesurée en charge", "${Formats.number(estimate.toLong())} mAh$health")
        } ?: Text(
            "La capacité mesurée apparaîtra après une charge d'au moins ${ChargeSession.MIN_LEVEL_FOR_ESTIMATE} points.",
            style = MaterialTheme.typography.bodySmall,
            color = colors.onSurfaceVariant,
        )
        battery?.cycleCount?.let { MetricRow("Cycles de charge", it.toString()) }
        Text(
            "Google n'affiche pas la santé dans les Réglages du Pixel 8 Pro, et celle déclarée par le système y reste à " +
                "100 % : la capacité apprise par la jauge est l'estimation la plus fiable. Repère Google : environ 80 % " +
                "après 800 cycles.",
            style = MaterialTheme.typography.bodySmall,
            color = colors.onSurfaceVariant,
        )
    }
}

@Composable
private fun AppUsageCard(state: BatteryScreenState, onRefresh: () -> Unit, onOpenApp: (String) -> Unit, onOpenAccess: () -> Unit) {
    SectionCard("Consommation par appli", Icons.Outlined.BatteryAlert) {
        Text(
            "Estimation du système depuis la dernière charge complète, en mAh.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        val usage = state.appUsage
        when {
            state.appUsageLoading && usage == null -> Text("Lecture en cours…", style = MaterialTheme.typography.bodySmall)
            usage == null -> MissingAccessHint("Demande la permission DUMP ou Shizuku.", "Configurer", onOpenAccess)
            usage.isEmpty() -> Text("Aucune donnée (le téléphone vient peut-être d'être chargé).", style = MaterialTheme.typography.bodySmall)
            else -> {
                val total = usage.sumOf { it.mah }.takeIf { it > 0 } ?: 1.0
                usage.take(MAX_APPS).forEach { app ->
                    val pkg = app.packageName
                    Column(Modifier.fillMaxWidth().then(if (pkg != null) Modifier.clickable { onOpenApp(pkg) } else Modifier)) {
                        MetricRow(app.label, "${ONE_DECIMAL.format(app.mah)} mAh · ${Formats.percent((app.mah / total).toFloat())}")
                        LevelBar((app.mah / usage.first().mah).toFloat())
                    }
                }
            }
        }
        TextButton(onClick = onRefresh) { Text("Actualiser") }
    }
}

@Composable
private fun RailsCard(state: BatteryScreenState) {
    // The API lists the ODPM rails and the energy consumers derived from them (DISPLAY, CPU/0…):
    // keep the rails only, otherwise the same energy shows twice.
    val rails = state.rails.filterNot { it.isConsumer }.ifEmpty { state.rails }
    SectionCard("Énergie par composant", Icons.Outlined.ElectricBolt) {
        if (rails.isEmpty()) {
            Text(
                "Mesure en cours (environ 20 s), ou capteurs d'énergie non exposés par ce téléphone.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        val max = rails.maxOfOrNull { it.watts }?.takeIf { it > 0 } ?: 1.0
        rails.forEach { rail ->
            MetricRow(rail.name, Formats.watts(rail.watts))
            LevelBar((rail.watts / max).toFloat())
        }
        Text(
            "Mesures des rails d'alimentation (ODPM) : CPU par cluster, GPU, modem, écran… Moyenne sur l'intervalle entre deux relevés.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private const val MAX_APPS = 15

@Composable
private fun healthColor(fraction: Float) = when {
    fraction >= 0.85f -> MaterialTheme.colorScheme.ok
    fraction >= 0.75f -> MaterialTheme.colorScheme.warning
    else -> MaterialTheme.colorScheme.critical
}

@Composable
private fun SessionRow(session: ChargeSessionEntity) {
    val start = Instant.ofEpochMilli(session.startTs).atZone(ZoneId.systemDefault())
    val ongoing = session.endTs == null
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(SESSION_DATE.format(start).replaceFirstChar { it.uppercase() }, style = MaterialTheme.typography.bodyMedium)
            Text(
                "${session.startLevel} → ${session.endLevel} %" + if (ongoing) " (en cours)" else "",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        val details = listOfNotNull(
            Formats.duration(session.lastTs - session.startTs),
            session.addedMah?.let { "+${Formats.number(it.toLong())} mAh" },
            session.averagePowerW?.let { "moy. ${Formats.watts(it)}" },
            "max ${Formats.watts(session.maxPowerW)}",
            "max ${Formats.celsius(session.maxTemperatureC)}",
            session.estimatedCapacityMah?.let { "≈ ${Formats.number(it.toLong())} mAh pleine" },
        )
        Text(details.joinToString(" · "), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        HorizontalDivider(Modifier.padding(top = 8.dp))
    }
}
