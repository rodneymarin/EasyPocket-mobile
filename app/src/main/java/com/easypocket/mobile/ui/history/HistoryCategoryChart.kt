package com.easypocket.mobile.ui.history

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.easypocket.mobile.i18n.Language
import com.easypocket.mobile.i18n.LocalLanguage
import com.easypocket.mobile.i18n.t
import com.easypocket.mobile.ui.theme.LocalAppColors
import com.easypocket.mobile.ui.theme.LocalIsDark
import com.easypocket.mobile.ui.theme.StoreColors
import java.util.Locale

private const val SWEEP_DURATION_MS = 500
private const val START_ANGLE = -90f

@Composable
fun HistoryCategoryChart(slices: List<HistoryCategorySlice>, modifier: Modifier = Modifier) {
    val appColors = LocalAppColors.current
    val isDark = LocalIsDark.current
    val language = LocalLanguage.current
    val grandTotal = slices.sumOf { it.total }
    if (slices.isEmpty() || grandTotal <= 0.0) return

    val sweepProgress = remember(slices) { Animatable(0f) }
    LaunchedEffect(slices) {
        sweepProgress.animateTo(1f, tween(durationMillis = SWEEP_DURATION_MS, easing = FastOutSlowInEasing))
    }

    val colors = slices.mapIndexed { index, slice ->
        if (slice.name == null) appColors.textSecondary else StoreColors.get(index % StoreColors.light.size, isDark)
    }

    Row(
        modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(120.dp), contentAlignment = Alignment.Center) {
            Donut(
                slices = slices,
                colors = colors,
                grandTotal = grandTotal,
                progress = sweepProgress.value,
                modifier = Modifier.fillMaxSize(),
            )
            Text(
                text = "$${String.format(Locale.US, "%.2f", grandTotal)}",
                color = appColors.text,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
            )
        }
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            slices.forEachIndexed { index, slice ->
                LegendRow(
                    slice = slice,
                    color = colors[index],
                    grandTotal = grandTotal,
                    language = language,
                )
            }
        }
    }
}

@Composable
private fun Donut(
    slices: List<HistoryCategorySlice>,
    colors: List<Color>,
    grandTotal: Double,
    progress: Float,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier) {
        val stroke = 18.dp.toPx()
        val inset = stroke / 2 + 1.dp.toPx()
        val arcSize = Size(size.width - inset * 2, size.height - inset * 2)
        var startAngle = START_ANGLE
        slices.forEachIndexed { index, slice ->
            val sweep = (slice.total / grandTotal * 360.0).toFloat() * progress
            drawArc(
                color = colors[index],
                startAngle = startAngle,
                sweepAngle = sweep,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = arcSize,
                style = Stroke(width = stroke, cap = StrokeCap.Butt),
            )
            startAngle += sweep
        }
    }
}

@Composable
private fun LegendRow(
    slice: HistoryCategorySlice,
    color: Color,
    grandTotal: Double,
    language: Language,
) {
    val appColors = LocalAppColors.current
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(color),
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = slice.name ?: t("history.noCategory", language),
            color = appColors.text,
            fontSize = 13.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false),
        )
        Spacer(Modifier.weight(1f))
        Text(
            text = "$${String.format(Locale.US, "%.2f", slice.total)} · ${(slice.total / grandTotal * 100).toInt()}%",
            color = appColors.textSecondary,
            fontSize = 12.sp,
        )
    }
}
