package fr.tflag.droidboost.widget

import android.content.Context
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Column
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import fr.tflag.droidboost.DroidBoostApp
import fr.tflag.droidboost.MainActivity
import fr.tflag.droidboost.core.format.Formats

/** Home-screen widget with the key metrics; refreshed by the monitoring service every minute. */
class StatsWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val lines = lines(context)
        provideContent {
            GlanceTheme {
                Column(
                    GlanceModifier
                        .fillMaxSize()
                        .background(GlanceTheme.colors.widgetBackground)
                        .cornerRadius(16.dp)
                        .padding(12.dp)
                        .clickable(actionStartActivity<MainActivity>()),
                ) {
                    Text("DroidBoost", style = TextStyle(color = GlanceTheme.colors.primary, fontWeight = FontWeight.Bold, fontSize = 14.sp))
                    lines.forEach { line ->
                        Text(line, style = TextStyle(color = GlanceTheme.colors.onSurface, fontSize = 13.sp))
                    }
                }
            }
        }
    }

    private suspend fun lines(context: Context): List<String> {
        val container = (context.applicationContext as DroidBoostApp).container
        val battery = container.battery.read()
        val thermal = container.newThermalReader().read(includeHal = false)
        val memory = container.newMemoryReader().read()
        val storage = container.storage.totals()
        return listOfNotNull(
            battery?.let { b ->
                listOfNotNull(
                    "${b.levelPercent} %",
                    b.powerW?.let { (if (b.isCharging) "+" else "−") + Formats.watts(it) },
                    Formats.celsius(b.temperatureC),
                ).joinToString(" · ")
            },
            "Thermique ${thermal.status.label.lowercase()}" + (thermal.headroom?.let { " · ${Formats.percent(it)}" } ?: ""),
            "RAM libre ${Formats.bytes(memory.availableBytes)}",
            storage?.let { "Stockage libre ${Formats.bytes(it.freeBytes)}" },
        )
    }
}

class StatsWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = StatsWidget()
}
