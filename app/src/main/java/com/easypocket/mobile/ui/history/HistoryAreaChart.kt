package com.easypocket.mobile.ui.history

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.axis.HorizontalAxis
import com.patrykandpatrick.vico.compose.cartesian.axis.VerticalAxis
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberAxisGuidelineComponent
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberAxisLabelComponent
import com.patrykandpatrick.vico.compose.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.compose.cartesian.data.CartesianValueFormatter
import com.patrykandpatrick.vico.compose.cartesian.data.lineModel
import com.patrykandpatrick.vico.compose.cartesian.layer.LineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLine
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.compose.cartesian.rememberVicoScrollState
import com.patrykandpatrick.vico.compose.common.Fill
import com.patrykandpatrick.vico.compose.common.component.rememberLineComponent
import com.patrykandpatrick.vico.compose.common.data.ExtraStore
import com.easypocket.mobile.i18n.LocalLanguage
import com.easypocket.mobile.ui.components.formatAmountCompact
import com.easypocket.mobile.ui.theme.LocalAppColors
import com.easypocket.mobile.ui.theme.LocalIsDark
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private val DATE_LABEL_FORMAT = DateTimeFormatter.ofPattern("dd/MM")

@Composable
fun HistoryAreaChart(points: List<HistoryChartPoint>, modifier: Modifier = Modifier) {
    val appColors = LocalAppColors.current
    val isDark = LocalIsDark.current
    val language = LocalLanguage.current
    val maxValue = points.maxOfOrNull { it.total }?.coerceAtLeast(1.0) ?: return
    val lineColor = appColors.primary
    val guidelineColor = if (isDark) Color.White.copy(alpha = 0.10f) else Color.Black.copy(alpha = 0.10f)

    val modelProducer = remember { CartesianChartModelProducer() }
    val xToDateKey = remember { ExtraStore.Key<Map<Float, LocalDate>>() }
    LaunchedEffect(points) {
        val dates = points.mapIndexed { index, point -> index.toFloat() to point.date }.toMap()
        modelProducer.runTransaction {
            lineModel { series(dates.keys.toList(), points.map { it.total.toFloat() }) }
            extras { store -> store[xToDateKey] = dates }
        }
    }

    val dateFormatter = remember(xToDateKey) {
        CartesianValueFormatter { context, value, _ ->
            context.model.extraStore[xToDateKey][value.toFloat()]?.format(DATE_LABEL_FORMAT) ?: ""
        }
    }

    val amountFormatter = remember(language) {
        CartesianValueFormatter { _, value, _ ->
            formatAmountCompact(language, value)
        }
    }

    Column(modifier.fillMaxWidth()) {
        Text(
            text = formatAmountCompact(language, maxValue),
            color = appColors.textSecondary,
            fontSize = 11.sp,
            modifier = Modifier.align(Alignment.End).padding(end = 4.dp),
        )
        CartesianChartHost(
            chart =
                rememberCartesianChart(
                    rememberLineCartesianLayer(
                        lineProvider =
                            LineCartesianLayer.LineProvider.series(
                                LineCartesianLayer.rememberLine(
                                    fill = LineCartesianLayer.LineFill.single(Fill(lineColor)),
                                    areaFill =
                                        LineCartesianLayer.AreaFill.single(
                                            Fill(
                                                Brush.verticalGradient(
                                                    listOf(lineColor.copy(alpha = 0.35f), Color.Transparent)
                                                )
                                            )
                                        ),
                                    interpolator = LineCartesianLayer.Interpolator.catmullRom(),
                                    pointProvider =
                                        LineCartesianLayer.PointProvider.single(
                                            LineCartesianLayer.Point(
                                                component =
                                                    rememberLineComponent(
                                                        fill = Fill(lineColor),
                                                        shape = CircleShape,
                                                        strokeFill = Fill(appColors.background),
                                                        strokeThickness = 2.dp,
                                                    ),
                                                size = 8.dp,
                                            )
                                        ),
                                )
                            ),
                    ),
                    startAxis =
                        VerticalAxis.rememberStart(
                            valueFormatter = amountFormatter,
                            label =
                                rememberAxisLabelComponent(
                                    style = TextStyle(color = appColors.textSecondary, fontSize = 11.sp),
                                ),
                            guideline =
                                rememberAxisGuidelineComponent(
                                    fill = Fill(guidelineColor),
                                    thickness = 1.dp,
                                ),
                        ),
                    bottomAxis =
                        HorizontalAxis.rememberBottom(
                            valueFormatter = dateFormatter,
                            itemPlacer = HorizontalAxis.ItemPlacer.aligned(),
                            label =
                                rememberAxisLabelComponent(
                                    style = TextStyle(color = appColors.text, fontSize = 11.sp),
                                ),
                            guideline =
                                rememberAxisGuidelineComponent(
                                    fill = Fill(guidelineColor),
                                    thickness = 1.dp,
                                ),
                        ),
                ),
            modelProducer = modelProducer,
            modifier = Modifier.fillMaxWidth().height(180.dp),
            scrollState = rememberVicoScrollState(scrollEnabled = false),
        )
    }
}
