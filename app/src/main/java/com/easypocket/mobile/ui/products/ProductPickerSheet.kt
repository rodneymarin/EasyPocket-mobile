package com.easypocket.mobile.ui.products

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.easypocket.mobile.domain.ListLogic
import com.easypocket.mobile.domain.Product
import com.easypocket.mobile.ui.components.AppBottomSheet
import com.easypocket.mobile.ui.components.SearchInput
import com.easypocket.mobile.ui.theme.LocalAppColors
import kotlinx.coroutines.delay

@Composable
fun ProductPickerSheet(
    visible: Boolean,
    products: List<Product>,
    searchPlaceholder: String,
    noResultsMessage: String,
    selectedProductId: String?,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val appColors = LocalAppColors.current
    var searchQuery by remember { mutableStateOf("") }
    var debouncedQuery by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }

    val filtered = remember(products, debouncedQuery) {
        if (debouncedQuery.isBlank()) products
        else products.filter { ListLogic.normalize(it.productName).contains(ListLogic.normalize(debouncedQuery)) }
    }

    AppBottomSheet(visible = visible, onDismiss = onDismiss, heightFraction = 0.6f) {
        LaunchedEffect(visible) {
            if (visible) {
                searchQuery = ""
                debouncedQuery = ""
                focusRequester.requestFocus()
            }
        }
        LaunchedEffect(searchQuery) {
            delay(300)
            debouncedQuery = searchQuery
        }
        SearchInput(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = searchPlaceholder,
            focusRequester = focusRequester,
        )
        Spacer(Modifier.height(12.dp))
        if (filtered.isEmpty() && debouncedQuery.isNotBlank()) {
            Box(Modifier.fillMaxWidth().padding(top = 40.dp), contentAlignment = Alignment.Center) {
                Text(noResultsMessage, color = appColors.textSecondary, fontSize = 15.sp)
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                items(filtered, key = { it.id }) { product ->
                    val selected = product.id == selectedProductId
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(if (selected) appColors.surface else Color.Transparent)
                            .clickable { onSelect(product.id) }
                            .padding(vertical = 12.dp, horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            product.productName,
                            color = appColors.text,
                            fontSize = 15.sp,
                            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }
    }
}
