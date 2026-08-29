package com.easypocket.mobile.ui.history

import android.graphics.Typeface
import android.text.TextUtils
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.easypocket.mobile.i18n.LocalLanguage
import com.easypocket.mobile.i18n.t
import com.easypocket.mobile.ui.theme.LocalAppColors
import java.util.Locale
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

private const val SWEEP_DURATION_MS = 500
private const val START_ANGLE = -90f
private const val MIN_LABEL_SPACING_DP = 26f

private val CategoryPalette = listOf(
    0xFF1E88E5.toInt(), // azul
    0xFFE53935.toInt(), // rojo
    0xFF43A047.toInt(), // verde
    0xFFFB8C00.toInt(), // naranja
    0xFF8E24AA.toInt(), // violeta
    0xFF00ACC1.toInt(), // cian
    0xFFD81B60.toInt(), // rosa
    0xFF7CB342.toInt(), // lima
    0xFFF9A825.toInt(), // ámbar
    0xFF5C6BC0.toInt(), // índigo
)

@Composable
fun HistoryCategoryChart(slices: List<HistoryCategorySlice>, modifier: Modifier = Modifier) {
    val appColors = LocalAppColors.current
    val language = LocalLanguage.current
    val grandTotal = slices.sumOf { it.total }
    if (slices.isEmpty() || grandTotal <= 0.0) return

    val sweepProgress = remember(slices) { Animatable(0f) }
    LaunchedEffect(slices) {
        sweepProgress.animateTo(1f, tween(durationMillis = SWEEP_DURATION_MS, easing = FastOutSlowInEasing))
    }

    val colors = slices.mapIndexed { index, slice ->
        if (slice.name == null) appColors.textSecondary else Color(CategoryPalette[index % CategoryPalette.size])
    }
    val noCategoryLabel = t("history.noCategory", language)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(300.dp)
            .padding(horizontal = 0.dp, vertical = 2.dp),
    ) {
        CategoryDonut(
            slices = slices,
            colors = colors,
            grandTotal = grandTotal,
            progress = sweepProgress.value,
            lineColor = appColors.textSecondary.copy(alpha = 0.55f),
            nameColor = appColors.text,
            amountColor = appColors.textSecondary,
            noCategoryLabel = noCategoryLabel,
            modifier = Modifier.fillMaxSize(),
        )
    }
}

private class SliceGeom(
    val color: Color,
    val anchor: Offset,
    val elbowX: Float,
    val label: String,
    val amount: String,
    val isRight: Boolean,
)

@Composable
private fun CategoryDonut(
    slices: List<HistoryCategorySlice>,
    colors: List<Color>,
    grandTotal: Double,
    progress: Float,
    lineColor: Color,
    nameColor: Color,
    amountColor: Color,
    noCategoryLabel: String,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier) {
        val centerX = size.width / 2f
        val centerY = size.height / 2f
        val labelMargin = 62.dp.toPx()
        val radius = min(size.width, size.height) / 2f - labelMargin
        val stroke = 26.dp.toPx()
        val elbowLength = 10.dp.toPx()

        val namePaint = android.graphics.Paint().apply {
            isAntiAlias = true
            textSize = 12.sp.toPx()
            color = nameColor.toArgb()
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val amountPaint = android.graphics.Paint().apply {
            isAntiAlias = true
            textSize = 10.sp.toPx()
            color = amountColor.toArgb()
        }

        val geoms = mutableListOf<SliceGeom>()
        var startAngle = START_ANGLE
        slices.forEachIndexed { index, slice ->
            val sweep = (slice.total / grandTotal * 360.0).toFloat() * progress
            drawArc(
                color = colors[index],
                startAngle = startAngle,
                sweepAngle = sweep,
                useCenter = false,
                topLeft = Offset(centerX - radius, centerY - radius),
                size = Size(radius * 2, radius * 2),
                style = Stroke(width = stroke, cap = StrokeCap.Butt),
            )

            val midAngle = startAngle + sweep / 2f
            val radians = Math.toRadians(midAngle.toDouble())
            val dirX = cos(radians).toFloat()
            val dirY = sin(radians).toFloat()
            geoms.add(
                SliceGeom(
                    color = colors[index],
                    anchor = Offset(centerX + dirX * (radius + 3.dp.toPx()), centerY + dirY * (radius + 3.dp.toPx())),
                    elbowX = centerX + dirX * (radius + 11.dp.toPx()),
                    label = slice.name ?: noCategoryLabel,
                    amount = "$${String.format(Locale.US, "%.2f", slice.total)} · ${(slice.total / grandTotal * 100).toInt()}%",
                    isRight = dirX >= 0f,
                )
            )
            startAngle += sweep
        }

        val minSpacing = MIN_LABEL_SPACING_DP.dp.toPx()
        val maxLabelY = size.height - 14.dp.toPx()
        val minLabelY = 18.dp.toPx()

        fun drawSide(isRight: Boolean) {
            val side = geoms.filter { it.isRight == isRight }.sortedBy { it.anchor.y }
            if (side.isEmpty()) return

            val ys = mutableListOf<Float>()
            side.forEach { geom ->
                var y = geom.anchor.y.coerceIn(minLabelY, maxLabelY)
                if (ys.isNotEmpty() && y - ys.last() < minSpacing) y = ys.last() + minSpacing
                ys.add(y)
            }
            val overflow = ys.last() - maxLabelY
            if (overflow > 0f) {
                for (i in ys.indices) ys[i] -= overflow
                for (i in 1 until ys.size) {
                    if (ys[i] - ys[i - 1] < minSpacing) ys[i] = ys[i - 1] + minSpacing
                }
            }

            side.forEachIndexed { i, geom ->
                val y = ys[i]
                val endX = geom.elbowX + (if (isRight) elbowLength else -elbowLength)
                drawLine(lineColor, geom.anchor, Offset(geom.elbowX, y), strokeWidth = 1.5.dp.toPx())
                drawLine(lineColor, Offset(geom.elbowX, y), Offset(endX, y), strokeWidth = 1.5.dp.toPx())

                val alignRight = !isRight
                namePaint.textAlign = if (alignRight) android.graphics.Paint.Align.RIGHT else android.graphics.Paint.Align.LEFT
                amountPaint.textAlign = namePaint.textAlign
                val textX = endX + (if (isRight) 6.dp.toPx() else -6.dp.toPx())
                val maxWidth = (if (isRight) size.width - textX - 8.dp.toPx() else textX - 8.dp.toPx()).coerceAtLeast(1f)

                val ellipsized = TextUtils.ellipsize(geom.label, android.text.TextPaint(namePaint), maxWidth, TextUtils.TruncateAt.END).toString()
                val native = drawContext.canvas.nativeCanvas
                native.drawText(ellipsized, textX, y - 3.dp.toPx(), namePaint)
                native.drawText(geom.amount, textX, y + 11.dp.toPx(), amountPaint)
            }
        }

        drawSide(isRight = true)
        drawSide(isRight = false)
    }
}
