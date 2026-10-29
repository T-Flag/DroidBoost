package fr.tflag.droidboost.ui.dashboard

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BatteryChargingFull
import androidx.compose.material.icons.outlined.DeveloperBoard
import androidx.compose.material.icons.outlined.Memory
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.PhoneAndroid
import androidx.compose.material.icons.outlined.SdStorage
import androidx.compose.material.icons.outlined.Thermostat
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import fr.tflag.droidboost.core.format.Formats
import fr.tflag.droidboost.core.model.ThermalStatus
import fr.tflag.droidboost.core.model.TemperatureTypes
import fr.tflag.droidboost.shell.ShizukuState
import fr.tflag.droidboost.system.BatterySnapshot
import fr.tflag.droidboost.system.CpuSnapshot
import fr.tflag.droidboost.system.MemorySnapshot
import fr.tflag.droidboost.system.ThermalSnapshot
import fr.tflag.droidboost.ui.components.LevelBar
import fr.tflag.droidboost.ui.components.MetricRow
import fr.tflag.droidboost.ui.components.MissingAccessHint
import fr.tflag.droidboost.ui.components.SectionCard
import fr.tflag.droidboost.ui.components.StatusPill
import fr.tflag.droidboost.ui.theme.critical
import fr.tflag.droidboost.ui.theme.ok
import fr.tflag.droidboost.ui.theme.warning
import java.time.format.DateTimeFormatter
import java.util.Locale

private val DATE = DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.FRANCE)

@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    contentPadding: PaddingValues,
    onOpenAccess: () -> Unit,
    onOpenBattery: () -> Unit,
    onOpenThermal: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val shizukuReady = state.shizuku is ShizukuState.Ready
    val notificationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) viewModel.setMonitorEnabled(true)
    }

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
        item {
            SectionCard(
                "Notification permanente",
                Icons.Outlined.Notifications,
                trailing = {
                    Switch(
                        checked = state.monitorEnabled,
                        onCheckedChange = { enabled ->
                            if (enabled) notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                            else viewModel.setMonitorEnabled(false)
                        },
                    )
                },
            ) {
                Text(
                    "Batterie, thermique, RAM et débit réseau en permanence, relancée au démarrage.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        state.battery?.let { battery -> item { BatteryCard(battery, state.hasBatteryStats, onOpenAccess, onOpenBattery) } }
        state.thermal?.let { thermal -> item { ThermalCard(thermal, shizukuReady, onOpenAccess, onOpenThermal) } }
        state.cpu?.let { cpu -> item { CpuCard(cpu, shizukuReady, onOpenAccess) } }
        state.memory?.let { memory -> item { MemoryCard(memory) } }
        item {
            SectionCard("Stockage et réseau", Icons.Outlined.SdStorage) {
                state.storage?.let { storage ->
                    MetricRow("Utilisé", "${Formats.bytes(storage.usedBytes)} sur ${Formats.bytes(storage.totalBytes)}")
                    LevelBar(storage.usedFraction)
                    MetricRow("Libre", Formats.bytes(storage.freeBytes))
                }
                state.network?.let { network ->
                    MetricRow("Réception", Formats.bytesPerSecond(network.rxBytesPerSecond))
                    MetricRow("Envoi", Formats.bytesPerSecond(network.txBytesPerSecond))
                }
            }
        }
        state.system?.let { system ->
            item {
                SectionCard("Système", Icons.Outlined.PhoneAndroid) {
                    MetricRow("Appareil", system.model)
                    MetricRow("Android", "${system.androidRelease} (API ${system.sdkInt})")
                    MetricRow("Correctif de sécurité", system.securityPatch)
                    MetricRow("Build", system.buildId)
                    system.kernel?.let { MetricRow("Noyau", it) }
                    MetricRow("Allumé depuis", Formats.duration(system.elapsedRealtimeMs))
                    MetricRow("Sommeil profond", Formats.percent(system.deepSleepFraction))
                }
            }
        }
    }
}

@Composable
private fun BatteryCard(battery: BatterySnapshot, hasBatteryStats: Boolean, onOpenAccess: () -> Unit, onOpen: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    SectionCard(
        "Batterie",
        Icons.Outlined.BatteryChargingFull,
        trailing = { StatusPill(battery.statusLabel, if (battery.isCharging) colors.ok else colors.primary) },
        onClick = onOpen,
    ) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text("${battery.levelPercent} %", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.width(12.dp))
            Text(battery.pluggedLabel, style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
        }
        LevelBar(battery.levelPercent / 100f, color = if (battery.levelPercent <= 15) colors.critical else colors.primary)
        val sign = if (battery.isCharging) "+" else "−"
        battery.currentMa?.let { MetricRow("Courant", sign + Formats.milliAmps(it)) }
        battery.powerW?.let { MetricRow("Puissance", sign + Formats.watts(it)) }
        MetricRow("Tension", Formats.volts(battery.voltageMv))
        MetricRow(
            "Température",
            Formats.celsius(battery.temperatureC),
            valueColor = when {
                battery.temperatureC >= 42f -> colors.critical
                battery.temperatureC >= 38f -> colors.warning
                else -> Color.Unspecified
            },
        )
        battery.chargeCounterMah?.let { MetricRow("Charge restante", "${Formats.number(it.toLong())} mAh") }
        MetricRow("Cycles de charge", battery.cycleCount?.toString() ?: "Non communiqué")
        MetricRow("État (système)", battery.healthLabel)
        when {
            battery.stateOfHealthPercent != null -> MetricRow("Santé (capacité restante)", "${battery.stateOfHealthPercent} %")
            !hasBatteryStats -> MissingAccessHint(
                "La santé de la batterie et ses dates demandent la permission BATTERY_STATS.",
                "Accorder",
                onOpenAccess,
            )
            else -> MetricRow("Santé (capacité restante)", "Non communiquée par le téléphone")
        }
        battery.manufacturingDate?.let { MetricRow("Fabrication", DATE.format(it)) }
        battery.firstUseDate?.let { MetricRow("Première utilisation", DATE.format(it)) }
    }
}

@Composable
private fun ThermalCard(thermal: ThermalSnapshot, shizukuReady: Boolean, onOpenAccess: () -> Unit, onOpen: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val statusColor = when {
        thermal.status.level >= ThermalStatus.SEVERE.level -> colors.critical
        thermal.status.level >= ThermalStatus.LIGHT.level -> colors.warning
        else -> colors.ok
    }
    SectionCard("Thermique", Icons.Outlined.Thermostat, trailing = { StatusPill(thermal.status.label, statusColor) }, onClick = onOpen) {
        thermal.headroom?.let { headroom ->
            MetricRow("Marge avant bridage sévère", Formats.percent(1f - headroom.coerceIn(0f, 1f)))
            LevelBar(headroom, color = if (headroom >= 0.85f) colors.critical else if (headroom >= 0.6f) colors.warning else colors.primary)
        } ?: MetricRow("Marge thermique", "Indisponible")
        val hal = thermal.hal
        if (hal == null) {
            MissingAccessHint(
                if (shizukuReady) "Lecture des capteurs en cours…" else "Les températures des capteurs (CPU, GPU, surface, modem…) demandent Shizuku.",
                if (shizukuReady) null else "Configurer",
                onOpenAccess,
            )
        } else {
            hal.temperatures.filter { TemperatureTypes.isTemperature(it.type) }.sortedByDescending { it.celsius }.forEach { sensor ->
                val throttleAt = hal.thresholdFor(sensor)?.firstThrottle
                MetricRow(
                    "${sensor.name} · ${TemperatureTypes.label(sensor.type)}",
                    Formats.celsius(sensor.celsius) + (throttleAt?.let { " / ${Formats.celsius(it)}" } ?: ""),
                    valueColor = when {
                        sensor.status >= ThermalStatus.MODERATE.level -> colors.critical
                        sensor.status >= ThermalStatus.LIGHT.level -> colors.warning
                        else -> Color.Unspecified
                    },
                )
            }
            if (hal.thresholds.isNotEmpty()) {
                Text(
                    "Valeur / seuil de premier bridage quand le système le publie.",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun CpuCard(cpu: CpuSnapshot, shizukuReady: Boolean, onOpenAccess: () -> Unit) {
    SectionCard(
        "Processeur",
        Icons.Outlined.DeveloperBoard,
        trailing = { cpu.totalLoad?.let { StatusPill("Charge ${Formats.percent(it)}", MaterialTheme.colorScheme.primary) } },
    ) {
        if (cpu.source == CpuSnapshot.Source.NONE) {
            MissingAccessHint("Les fréquences ne sont pas lisibles sans Shizuku sur cet appareil.", "Configurer", onOpenAccess)
        }
        cpu.clusters.forEach { cluster ->
            Text(
                cluster.name + (cluster.maxKhz?.let { " · max ${Formats.frequencyKhz(it)}" } ?: ""),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 4.dp),
            )
            cluster.cores.forEach { core ->
                val load = cpu.load[core.index]
                val label = "Cœur ${core.index}"
                val current = core.currentKhz
                val value = when {
                    !core.online -> "Éteint"
                    current != null -> Formats.frequencyKhz(current) + (load?.let { " · ${Formats.percent(it)}" } ?: "")
                    else -> "—"
                }
                MetricRow(label, value)
                val fraction = load ?: core.currentKhz?.let { cur -> core.maxKhz?.let { max -> cur.toFloat() / max } }
                fraction?.let { LevelBar(it, Modifier.padding(bottom = 2.dp)) }
            }
        }
        if (cpu.load.isEmpty()) {
            MissingAccessHint(
                if (shizukuReady) "Calcul de la charge…" else "La charge réelle du CPU (/proc/stat) demande Shizuku ; les barres montrent la fréquence.",
                if (shizukuReady) null else "Configurer",
                onOpenAccess,
            )
        }
    }
}

@Composable
private fun MemoryCard(memory: MemorySnapshot) {
    SectionCard("Mémoire", Icons.Outlined.Memory) {
        MetricRow("Utilisée", "${Formats.bytes(memory.usedBytes)} sur ${Formats.bytes(memory.totalBytes)}")
        LevelBar(memory.usedFraction, color = if (memory.lowMemory) MaterialTheme.colorScheme.critical else MaterialTheme.colorScheme.primary)
        MetricRow("Disponible", Formats.bytes(memory.availableBytes))
        MetricRow("Seuil de mémoire basse", Formats.bytes(memory.thresholdBytes))
        if (memory.lowMemory) MetricRow("État", "Mémoire basse", valueColor = MaterialTheme.colorScheme.critical)
        memory.details?.let { details ->
            details.cachedBytes?.let { MetricRow("En cache", Formats.bytes(it)) }
            val swapTotal = details.swapTotalBytes
            val swapUsed = details.swapUsedBytes
            if (swapTotal != null && swapTotal > 0 && swapUsed != null) {
                MetricRow("Swap (zRAM)", "${Formats.bytes(swapUsed)} sur ${Formats.bytes(swapTotal)}")
                LevelBar(swapUsed.toFloat() / swapTotal, Modifier.fillMaxWidth())
            }
        }
    }
}
