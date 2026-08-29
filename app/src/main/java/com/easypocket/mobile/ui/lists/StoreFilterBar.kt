package com.easypocket.mobile.ui.lists

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.easypocket.mobile.domain.Store
import com.easypocket.mobile.i18n.LocalLanguage
import com.easypocket.mobile.i18n.t
import com.easypocket.mobile.ui.components.DropdownItem
import com.easypocket.mobile.ui.components.DropdownMenu
import com.easypocket.mobile.ui.theme.LocalAppColors

private val MORE_TOGGLE_WIDTH = 40.dp

@Composable
fun StoreFilterBar(
    stores: List<Store>,
    activeStoreId: String?,
    onSelectStore: (String?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val language = LocalLanguage.current
    val appColors = LocalAppColors.current
    var menuOpen by remember { mutableStateOf(false) }
    val textMeasurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val measureChipWidth: (String) -> Dp = { label ->
        with(density) {
            textMeasurer
                .measure(
                    text = AnnotatedString(label),
                    style = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Medium),
                    maxLines = 1,
                )
                .size.width
                .toDp() + 22.dp
        }
    }

    BoxWithConstraints(modifier) {
        val allLabel = t("listDetail.allStores", language)
        val (visible, hidden) = splitFilters(
            barWidth = maxWidth,
            stores = stores,
            activeStoreId = activeStoreId,
            allLabel = allLabel,
            measureWidth = measureChipWidth,
        )

        Row(verticalAlignment = Alignment.CenterVertically) {
            AllStoresChip(
                label = allLabel,
                selected = activeStoreId == null,
                onClick = { onSelectStore(null) },
            )
            visible.forEach { store ->
                Spacer(Modifier.width(6.dp))
                StoreChip(
                    store = store,
                    selected = activeStoreId == store.id,
                    onClick = { onSelectStore(store.id) },
                )
            }
            if (hidden.isNotEmpty()) {
                Spacer(Modifier.width(6.dp))
                Box {
                    Box(
                        modifier = Modifier
                            .width(MORE_TOGGLE_WIDTH)
                            .border(1.dp, appColors.border, RoundedCornerShape(999.dp))
                            .background(Color.Transparent, RoundedCornerShape(999.dp))
                            .clickable { menuOpen = true }
                            .padding(vertical = 4.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "⋯${hidden.size}",
                            color = appColors.text,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                        )
                    }
                    DropdownMenu(
                        expanded = menuOpen,
                        onDismiss = { menuOpen = false },
                    ) {
                        hidden.forEach { store ->
                            DropdownItem(
                                label = store.description,
                                onClick = {
                                    menuOpen = false
                                    onSelectStore(store.id)
                                },
                            )
                        }
                        DropdownItem(
                            label = allLabel,
                            onClick = {
                                menuOpen = false
                                onSelectStore(null)
                            },
                        )
                    }
                }
            }
        }
    }
}

private fun splitFilters(
    barWidth: Dp,
    stores: List<Store>,
    activeStoreId: String?,
    allLabel: String,
    measureWidth: (String) -> Dp,
): Pair<List<Store>, List<Store>> {
    if (barWidth == Dp.Unspecified || stores.isEmpty()) {
        return stores to emptyList()
    }
    val allWidth = measureWidth(allLabel)
    var remaining = barWidth - 16.dp - allWidth - 6.dp
    val visible = mutableListOf<Store>()
    val hidden = mutableListOf<Store>()

    for (store in stores) {
        val w = measureWidth(store.description)
        if (w + MORE_TOGGLE_WIDTH + 6.dp <= remaining) {
            visible.add(store)
            remaining -= w + 6.dp
        } else {
            hidden.add(store)
        }
    }

    val activeInHidden = hidden.firstOrNull { it.id == activeStoreId }
    if (activeStoreId != null && activeInHidden != null) {
        val newHidden = hidden.filterTo(mutableListOf()) { it.id != activeStoreId }
        val promotedWidth = measureWidth(activeInHidden.description)
        var visRemaining = barWidth - 16.dp - allWidth - 6.dp - promotedWidth - 6.dp
        val reordered = mutableListOf(activeInHidden)
        for (s in visible) {
            val w = measureWidth(s.description)
            if (w + MORE_TOGGLE_WIDTH + 6.dp <= visRemaining) {
                reordered.add(s)
                visRemaining -= w + 6.dp
            } else {
                newHidden.add(s)
            }
        }
        return reordered to newHidden
    }

    return visible to hidden
}

@Composable
private fun AllStoresChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    FilterChip(label = label, selected = selected, onClick = onClick)
}

@Composable
private fun StoreChip(
    store: Store,
    selected: Boolean,
    onClick: () -> Unit,
) {
    FilterChip(label = store.description, selected = selected, onClick = onClick)
}

@Composable
private fun FilterChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val appColors = LocalAppColors.current
    val bg = if (selected) appColors.primary else Color.Transparent
    val border = if (selected) appColors.primary else appColors.border
    val textColor = if (selected) Color.White else appColors.text
    Box(
        modifier = Modifier
            .border(1.dp, border, RoundedCornerShape(999.dp))
            .background(bg, RoundedCornerShape(999.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 4.dp),
    ) {
        Text(label, color = textColor, fontSize = 14.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}
