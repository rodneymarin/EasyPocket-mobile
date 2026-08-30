package com.easypocket.mobile.ui.history

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.easypocket.mobile.i18n.LocalLanguage
import com.easypocket.mobile.i18n.t
import com.easypocket.mobile.ui.theme.LocalAppColors
import com.patrykandpatrick.vico.compose.common.Fill
import com.patrykandpatrick.vico.compose.common.Insets
import com.patrykandpatrick.vico.compose.common.component.TextComponent
import com.patrykandpatrick.vico.compose.pie.PieChart
import com.patrykandpatrick.vico.compose.pie.PieChartHost
import com.patrykandpatrick.vico.compose.pie.PieSize
import com.patrykandpatrick.vico.compose.pie.data.PieChartModelProducer
import com.patrykandpatrick.vico.compose.pie.data.PieValueFormatter
import com.patrykandpatrick.vico.compose.pie.data.pieModel
import com.patrykandpatrick.vico.compose.pie.rememberPieChart
import java.util.Locale

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

    val colors = slices.mapIndexed { index, slice ->
        if (slice.name == null) appColors.textSecondary else Color(CategoryPalette[index % CategoryPalette.size])
    }
    val noCategoryLabel = t("history.noCategory", language)

    val modelProducer = remember { PieChartModelProducer() }
    LaunchedEffect(slices) {
        modelProducer.runTransaction {
            pieModel { series(slices.map { it.total }) }
        }
    }

    val sliceLabelStyle = TextStyle(color = appColors.text, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
    val sliceComponents = slices.mapIndexed { index, slice ->
        PieChart.Slice(
            fill = Fill(colors[index]),
            label =
                PieChart.SliceLabel.Outside(
                    textComponent = TextComponent(textStyle = sliceLabelStyle, margins = Insets(horizontal = 4.dp)),
                    lineColor = appColors.textSecondary.copy(alpha = 0.55f),
                ),
        )
    }

    Column(modifier.fillMaxWidth().padding(top = 16.dp, bottom = 20.dp)) {
        PieChartHost(
            chart =
                rememberPieChart(
                    sliceProvider = PieChart.SliceProvider.series(sliceComponents),
                    innerSize = PieSize.Inner.fixed(64.dp),
                    spacing = 3.dp,
                    valueFormatter =
                        PieValueFormatter { _, value, _ ->
                            "${(value / grandTotal * 100).toInt()}%"
                        },
                ),
            modelProducer = modelProducer,
            modifier = Modifier.fillMaxWidth().height(260.dp),
        )
        Legend(slices = slices, colors = colors, grandTotal = grandTotal, noCategoryLabel = noCategoryLabel)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Legend(
    slices: List<HistoryCategorySlice>,
    colors: List<Color>,
    grandTotal: Double,
    noCategoryLabel: String,
) {
    val appColors = LocalAppColors.current
    FlowRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        slices.forEachIndexed { index, slice ->
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.width(150.dp)) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(colors[index]),
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = slice.name ?: noCategoryLabel,
                    color = appColors.text,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    text = "$${String.format(Locale.US, "%.2f", slice.total)}",
                    color = appColors.textSecondary,
                    fontSize = 11.sp,
                )
            }
        }
    }
}
