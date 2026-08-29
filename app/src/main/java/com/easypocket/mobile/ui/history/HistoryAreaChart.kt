package com.easypocket.mobile.ui.history

import android.graphics.Typeface
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
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
                .height(160.dp)
                .padding(horizontal = 4.dp),
        ) {
            val progress = riseProgress.value
            val labelHeight = 18.dp.toPx()
            val chartBottom = size.height - labelHeight
            fun animatedY(point: HistoryChartPoint): Float {
                val target = chartBottom * (1f - (point.total / maxValue).toFloat())
                return chartBottom + (target - chartBottom) * progress
            }
            val positions = points.mapIndexed { index, point ->
                val x = if (points.size == 1) size.width / 2f else index * size.width / (points.size - 1)
                Offset(x, animatedY(point))
            }
            val linePath = Path()
            positions.forEachIndexed { index, position ->
                if (index == 0) {
                    linePath.moveTo(position.x, position.y)
                } else {
                    val prev = positions[index - 1]
                    val midX = (prev.x + position.x) / 2f
                    linePath.cubicTo(midX, prev.y, midX, position.y, position.x, position.y)
                }
            }
            val fillPath = Path().apply {
                addPath(linePath)
                lineTo(size.width, chartBottom)
                lineTo(0f, chartBottom)
                close()
            }
            drawPath(
                fillPath,
                brush = Brush.verticalGradient(
                    colors = listOf(appColors.primary.copy(alpha = 0.35f), Color.Transparent),
                ),
            )
            drawPath(linePath, color = appColors.primary, style = Stroke(width = 2.dp.toPx()))

            val gridColor = appColors.border.copy(alpha = 0.7f)
            positions.forEach { position ->
                drawLine(gridColor, Offset(position.x, position.y), Offset(position.x, chartBottom), strokeWidth = 1.dp.toPx())
            }
            positions.forEach { position ->
                drawCircle(color = appColors.background, radius = 4.dp.toPx(), center = position)
                drawCircle(color = appColors.primary, radius = 3.dp.toPx(), center = position)
            }

            val datePaint = android.graphics.Paint().apply {
                isAntiAlias = true
                textSize = 10.sp.toPx()
                color = appColors.textSecondary.toArgb()
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            }
            val native = drawContext.canvas.nativeCanvas
            val lastIndex = positions.lastIndex
            positions.forEachIndexed { index, position ->
                datePaint.textAlign = when (index) {
                    0 -> android.graphics.Paint.Align.LEFT
                    lastIndex -> android.graphics.Paint.Align.RIGHT
                    else -> android.graphics.Paint.Align.CENTER
                }
                native.drawText(
                    points[index].date.format(DATE_LABEL_FORMAT),
                    position.x,
                    size.height - 4.dp.toPx(),
                    datePaint,
                )
            }
        }
    }
}
