package fr.tflag.droidboost.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import fr.tflag.droidboost.core.monitor.HistoryWindow
import fr.tflag.droidboost.data.SeriesPoint

/**
 * Time series drawn over [window], ending now. Gaps longer than three buckets (service stopped)
 * break the line instead of joining distant points.
 */
@Composable
fun LineChart(
    points: List<SeriesPoint>,
    window: HistoryWindow,
    formatValue: (Double) -> String,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
    fixedMin: Double? = null,
    fixedMax: Double? = null,
    /** Horizontal reference line, e.g. a throttling threshold. */
    reference: Double? = null,
    height: Dp = 140.dp,
) {
    if (points.size < 2) {
        Text(
            "Pas encore assez de données sur cette période.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = modifier,
        )
        return
    }
    val end = System.currentTimeMillis()
    val start = end - window.durationMs
    val dataMin = points.minOf { it.value }
    val dataMax = points.maxOf { it.value }
    val minY = fixedMin ?: dataMin
    val maxY = (fixedMax ?: dataMax).let { if (it - minY < 1e-9) minY + 1 else it }
    val grid = MaterialTheme.colorScheme.outlineVariant
    val referenceColor = MaterialTheme.colorScheme.error

    Column(modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            AxisLabel("max ${formatValue(dataMax)}")
            AxisLabel("moy. ${formatValue(points.map { it.value }.average())}")
            AxisLabel("min ${formatValue(dataMin)}")
        }
        Canvas(Modifier.fillMaxWidth().height(height)) {
            val w = size.width
            val h = size.height
            fun y(value: Double) = (h - ((value - minY) / (maxY - minY)).toFloat() * h).coerceIn(0f, h)
            for (i in 0..3) {
                val gy = h * i / 3
                drawLine(grid, Offset(0f, gy), Offset(w, gy), strokeWidth = 1f)
            }
            reference?.let {
                val ry = y(it)
                drawLine(
                    referenceColor,
                    Offset(0f, ry),
                    Offset(w, ry),
                    strokeWidth = 1.5f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f)),
                )
            }
            val path = Path()
            val maxGap = window.bucketMs * 3
            var previous: Long? = null
            points.forEach { point ->
                val x = ((point.bucket - start).toFloat() / window.durationMs * w).coerceIn(0f, w)
                val py = y(point.value)
                val last = previous
                if (last == null || point.bucket - last > maxGap) path.moveTo(x, py) else path.lineTo(x, py)
                previous = point.bucket
            }
            drawPath(path, color, style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            AxisLabel("il y a ${window.label}")
            AxisLabel("maintenant")
        }
    }
}

@Composable
private fun AxisLabel(text: String) {
    Text(text, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
fun WindowSelector(selected: HistoryWindow, onSelect: (HistoryWindow) -> Unit, modifier: Modifier = Modifier) {
    Row(modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        HistoryWindow.entries.forEach { window ->
            FilterChip(selected = window == selected, onClick = { onSelect(window) }, label = { Text(window.label) })
        }
    }
}
