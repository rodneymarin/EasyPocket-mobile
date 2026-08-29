package com.easypocket.mobile.ui.history

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.easypocket.mobile.data.local.PurchaseHistoryWithItems
import com.easypocket.mobile.domain.ListLogic
import com.easypocket.mobile.i18n.Language
import com.easypocket.mobile.i18n.LocalLanguage
import com.easypocket.mobile.i18n.t
import com.easypocket.mobile.ui.components.AppBottomSheet
import com.easypocket.mobile.ui.components.AppHeader
import com.easypocket.mobile.ui.components.AppItemList
import com.easypocket.mobile.ui.components.ListIconCircle
import com.easypocket.mobile.ui.components.ListItemRow
import com.easypocket.mobile.ui.theme.LocalAppColors
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy")
private val ZONE: ZoneId = ZoneId.systemDefault()

@Composable
fun HistoryScreen(onMenuClick: () -> Unit) {
    val vm: HistoryViewModel = hiltViewModel()
    val uiState by vm.uiState.collectAsStateWithLifecycle()
    val language = LocalLanguage.current
    val appColors = LocalAppColors.current

    var selectedRecord by remember { mutableStateOf<PurchaseHistoryWithItems?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(appColors.background)
            .padding(top = 60.dp),
    ) {
        AppHeader(title = t("tab.history", language), onMenuClick = onMenuClick)
        when {
            uiState.isLoading -> Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text(t("common.loading", language), color = appColors.textSecondary, fontSize = 16.sp)
            }
            uiState.records.isEmpty() -> Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text(
                    t("history.empty", language),
                    color = appColors.textSecondary,
                    fontSize = 16.sp,
                    textAlign = TextAlign.Center,
                )
            }
            else -> {
                RangeChips(selected = uiState.rangeDays, onSelect = { vm.setRange(it) })
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(t("history.total", language), color = appColors.textSecondary, fontSize = 14.sp)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "$${formatAmount(uiState.grandTotal)}",
                        color = appColors.text,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
                if (uiState.filteredRecords.isEmpty()) {
                    Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text(
                            t("history.empty", language),
                            color = appColors.textSecondary,
                            fontSize = 16.sp,
                            textAlign = TextAlign.Center,
                        )
                    }
                } else {
                    HistoryAreaChart(
                        points = uiState.chartPoints,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    )
                    HistoryCategoryChart(
                        slices = uiState.categorySlices,
                    )
                    RecordsList(
                        records = uiState.filteredRecords,
                        language = language,
                        onRecordPress = { selectedRecord = it },
                    )
                }
            }
        }
    }

    RecordDetailSheet(
        record = selectedRecord,
        categoryNameOf = { code ->
            uiState.categories.firstOrNull { it.id == code }?.name
                ?: t("history.noCategory", language)
        },
        language = language,
        onDismiss = { selectedRecord = null },
    )
}

@Composable
private fun RangeChips(selected: Int?, onSelect: (Int?) -> Unit) {
    val appColors = LocalAppColors.current
    val language = LocalLanguage.current
    val options = listOf(
        7 to "history.range.7",
        30 to "history.range.30",
        90 to "history.range.90",
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        options.forEach { (days, key) ->
            val isSelected = selected == days
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(if (isSelected) appColors.primary else appColors.surface)
                    .clickable { onSelect(days) }
                    .padding(horizontal = 14.dp, vertical = 6.dp),
            ) {
                Text(
                    text = t(key, language),
                    color = if (isSelected) Color.White else appColors.textSecondary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                )
            }
        }
    }
}

@Composable
private fun RecordsList(
    records: List<PurchaseHistoryWithItems>,
    language: Language,
    onRecordPress: (PurchaseHistoryWithItems) -> Unit,
) {
    AppItemList(
        footerText = t("history.showingCount", language, mapOf("count" to records.size.toString())),
    ) {
        itemsIndexed(records, key = { _, entry -> entry.record.id }) { index, record ->
            ListItemRow(
                onClick = { onRecordPress(record) },
                isFirst = index == 0,
                isLast = index == records.lastIndex,
            ) {
                RecordCardContent(record = record)
            }
        }
    }
}

@Composable
private fun RecordCardContent(record: PurchaseHistoryWithItems) {
    val appColors = LocalAppColors.current
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        ListIconCircle(icon = record.record.listIcon, modifier = Modifier.size(40.dp))
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = record.record.listTitle,
                color = appColors.text,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = formatDate(record.record.date) + " · " +
                    t("history.itemsCount", LocalLanguage.current, mapOf("count" to record.record.itemCount.toString())),
                color = appColors.textSecondary,
                fontSize = 12.sp,
            )
        }
        Text(
            text = "$${formatAmount(record.record.totalAmount)}",
            color = appColors.text,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun RecordDetailSheet(
    record: PurchaseHistoryWithItems?,
    categoryNameOf: (String?) -> String,
    language: Language,
    onDismiss: () -> Unit,
) {
    val appColors = LocalAppColors.current
    AppBottomSheet(
        visible = record != null,
        onDismiss = onDismiss,
        heightFraction = 0.7f,
        title = record?.record?.listTitle ?: "",
    ) {
        val current = record ?: return@AppBottomSheet
        Text(formatDate(current.record.date), color = appColors.textSecondary, fontSize = 13.sp)
        Spacer(Modifier.height(12.dp))
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            current.items.forEach { item ->
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(item.productName, color = appColors.text, fontSize = 15.sp)
                        Text(
                            text = listOfNotNull(
                                item.storeName ?: t("listDetail.noStore", language),
                                categoryNameOf(item.categoryCode),
                                "${ListLogic.trimQuantity(item.quantity)} x $${formatAmount(item.unitPrice)}",
                            ).joinToString(" · "),
                            color = appColors.textSecondary,
                            fontSize = 12.sp,
                        )
                    }
                    Text(
                        text = "$${formatAmount(item.totalPrice)}",
                        color = appColors.text,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            Text(t("history.total", language), color = appColors.textSecondary, fontSize = 15.sp)
            Spacer(Modifier.width(8.dp))
            Text(
                text = "$${formatAmount(current.record.totalAmount)}",
                color = appColors.text,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

private fun formatDate(date: Long): String =
    Instant.ofEpochMilli(date).atZone(ZONE).toLocalDate().format(DATE_FORMAT)

private fun formatAmount(value: Double): String = String.format(Locale.US, "%.2f", value)
