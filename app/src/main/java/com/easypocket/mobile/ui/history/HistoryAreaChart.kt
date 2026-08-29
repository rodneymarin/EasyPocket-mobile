package com.easypocket.mobile.ui.history

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.easypocket.mobile.ui.theme.LocalAppColors
import java.time.format.DateTimeFormatter
import java.util.Locale

private val DATE_LABEL_FORMAT = DateTimeFormatter.ofPattern("dd/MM")

private const val RISE_DURATION_MS = 350

@Composable
fun HistoryAreaChart(points: List<HistoryChartPoint>, modifier: Modifier = Modifier) {
    val appColors = LocalAppColors.current
    val maxValue = points.maxOfOrNull { it.total }?.coerceAtLeast(1.0) ?: return
    val riseProgress = remember(points) { Animatable(0f) }
    LaunchedEffect(points) {
        riseProgress.animateTo(1f, tween(durationMillis = RISE_DURATION_MS, easing = FastOutSlowInEasing))
    }
    Column(modifier.fillMaxWidth()) {
        Text(
            text = "$${String.format(Locale.US, "%.2f", maxValue)}",
            color = appColors.textSecondary,
            fontSize = 11.sp,
            modifier = Modifier.align(Alignment.End).padding(end = 4.dp),
        )
        Canvas(
            Modifier
                .fillMaxWidth()
                .height(140.dp)
                .padding(horizontal = 4.dp),
        ) {
            val progress = riseProgress.value
            fun animatedY(point: HistoryChartPoint): Float {
                val target = size.height * (1f - (point.total / maxValue).toFloat())
                return size.height + (target - size.height) * progress
            }
            val linePath = Path()
            var prevX = 0f
            var prevY = 0f
            points.forEachIndexed { index, point ->
                val x = if (points.size == 1) size.width / 2f else index * size.width / (points.size - 1)
                val y = animatedY(point)
                if (index == 0) {
                    linePath.moveTo(x, y)
                } else {
                    val midX = (prevX + x) / 2f
                    linePath.cubicTo(midX, prevY, midX, y, x, y)
                }
                prevX = x
                prevY = y
            }
            val fillPath = Path().apply {
                addPath(linePath)
                lineTo(size.width, size.height)
                lineTo(0f, size.height)
                close()
            }
            drawPath(
                fillPath,
                brush = Brush.verticalGradient(
                    colors = listOf(appColors.primary.copy(alpha = 0.35f), Color.Transparent),
                ),
            )
            drawPath(linePath, color = appColors.primary, style = Stroke(width = 2.dp.toPx()))
        }
        Row(Modifier.fillMaxWidth()) {
            Text(
                text = points.first().date.format(DATE_LABEL_FORMAT),
                color = appColors.textSecondary,
                fontSize = 10.sp,
            )
            Spacer(Modifier.weight(1f))
            Text(
                text = points.last().date.format(DATE_LABEL_FORMAT),
                color = appColors.textSecondary,
                fontSize = 10.sp,
            )
        }
    }
}
