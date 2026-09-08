package com.easypocket.mobile.ui.products

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.easypocket.mobile.domain.ListLogic
import com.easypocket.mobile.domain.Product
import com.easypocket.mobile.domain.Store
import com.easypocket.mobile.i18n.Language
import com.easypocket.mobile.i18n.LocalLanguage
import com.easypocket.mobile.i18n.t
import com.easypocket.mobile.ui.components.AppButton
import com.easypocket.mobile.ui.components.AppFab
import com.easypocket.mobile.ui.components.AppHeader
import com.easypocket.mobile.ui.components.ButtonVariant
import com.easypocket.mobile.ui.components.ConfirmSheet
import com.easypocket.mobile.ui.components.IconButtonCircle
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import com.easypocket.mobile.ui.components.AppItemList
import com.easypocket.mobile.ui.components.rememberHighlightedNewItemId
import com.easypocket.mobile.ui.components.ListItemRow
import com.easypocket.mobile.ui.components.SubList
import com.easypocket.mobile.ui.components.SubListRow
import com.easypocket.mobile.ui.components.LocalToastState
import com.easypocket.mobile.ui.components.SearchInput
import com.easypocket.mobile.ui.components.Tag
import com.easypocket.mobile.ui.components.TagSize
import com.easypocket.mobile.ui.components.ToastType
import com.easypocket.mobile.ui.theme.LocalAppColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val SEARCH_DEBOUNCE_MS = 300L

@Composable
fun ProductsScreen(navController: NavController, onMenuClick: () -> Unit, refreshTick: Int = 0) {
    val vm: ProductListViewModel = hiltViewModel()
    val uiState by vm.uiState.collectAsStateWithLifecycle()
    val language = LocalLanguage.current
    val toast = LocalToastState.current
    val appColors = LocalAppColors.current
    val scope = rememberCoroutineScope()

    var searchText by rememberSaveable { mutableStateOf("") }
    var showDeleteSheet by rememberSaveable { mutableStateOf(false) }
    val backStackEntry by navController.currentBackStackEntryAsState()

    LaunchedEffect(refreshTick) { vm.refresh() }

    LaunchedEffect(searchText) {
        delay(SEARCH_DEBOUNCE_MS)
        vm.setSearch(searchText)
    }

    val isSelectionMode = uiState.isSelectionMode

    BackHandler(enabled = isSelectionMode) { vm.clearSelection() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(appColors.background)
            .padding(top = 60.dp),
    ) {
        AppHeader(
            title = t("tab.products", language),
            onMenuClick = onMenuClick,
        )
        if (isSelectionMode) {
            SelectionHeader(
                selectedCount = uiState.selection.size,
                onExit = { vm.clearSelection() },
                onDelete = { showDeleteSheet = true },
                language = language,
            )
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SearchInput(
                    value = searchText,
                    onValueChange = { searchText = it },
                    placeholder = t("search.products", language),
                    modifier = Modifier.weight(1f),
                )
            }
        }
        Box(Modifier.weight(1f).fillMaxWidth()) {
            when {
                uiState.isLoading -> LoadingState(language)
                uiState.filtered.isNotEmpty() -> ProductsList(
                    uiState = uiState,
                    language = language,
                    onProductPress = { id ->
                        if (isSelectionMode) vm.toggleSelection(id)
                        else navController.navigate("productForm/$id")
                    },
                    onProductLongPress = { id ->
                        if (!isSelectionMode) vm.setSelection(setOf(id))
                    },
                    navBackStackEntry = backStackEntry,
                )
                else -> EmptyState(
                    text = if (uiState.products.isEmpty()) t("products.empty", language) else t("common.noResults", language),
                )
            }
            if (!isSelectionMode) {
                AppFab(
                    icon = Icons.Filled.Add,
                    onClick = { navController.navigate("productForm/new") },
                    modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
                )
            }
        }
    }

    ConfirmSheet(
        visible = showDeleteSheet,
        title = t("products.deleteSelected.title", language),
        message = t("products.deleteSelected.confirmMessage", language, mapOf("count" to uiState.selection.size.toString())),
        warning = t("products.deleteSelected.warning", language),
        confirmLabel = t("products.deleteSelected.confirm", language),
        onConfirm = {
            scope.launch {
                vm.deleteSelected()
                showDeleteSheet = false
                toast.show(t("toast.productsDeleted", language), ToastType.SUCCESS)
            }
        },
        onDismiss = { showDeleteSheet = false },
    )
}

@Composable
private fun SelectionHeader(
    selectedCount: Int,
    onExit: () -> Unit,
    onDelete: () -> Unit,
    language: Language,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButtonCircle(
            icon = Icons.Filled.Close,
            onClick = onExit,
            variant = ButtonVariant.SECONDARY,
        )
        Text(
            text = "$selectedCount ${t("common.selected", language)}",
            color = LocalAppColors.current.text,
            fontSize = 17.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f).padding(start = 8.dp),
        )
        Spacer(Modifier.width(8.dp))
        AppButton(
            text = "${t("products.deleteSelected.confirm", language)} ($selectedCount)",
            onClick = onDelete,
            variant = ButtonVariant.DESTRUCTIVE,
        )
    }
}

@Composable
private fun ProductsList(
    uiState: ProductListUiState,
    language: Language,
    onProductPress: (String) -> Unit,
    onProductLongPress: (String) -> Unit,
    navBackStackEntry: NavBackStackEntry?,
) {
    val listState = rememberLazyListState()
    val highlightedId = rememberHighlightedNewItemId(
        navBackStackEntry = navBackStackEntry,
        displayedIds = uiState.filtered.map { it.id },
        listState = listState,
    )
    AppItemList(
        footerText = t("products.showingCount", language, mapOf("count" to uiState.filtered.size.toString())),
        listState = listState,
    ) {
        itemsIndexed(uiState.filtered, key = { _, product -> product.id }) { index, product ->
            var expanded by rememberSaveable(product.id) { mutableStateOf(false) }
            ListItemRow(
                onClick = { onProductPress(product.id) },
                onLongClick = { onProductLongPress(product.id) },
                isFirst = index == 0,
                isLast = index == uiState.filtered.lastIndex,
                highlighted = product.id == highlightedId,
            ) {
                Column(Modifier.fillMaxWidth()) {
                    ProductCardContent(
                        product = product,
                        isSelectionMode = uiState.isSelectionMode,
                        isSelected = product.id in uiState.selection,
                        language = language,
                        expanded = expanded,
                        onToggleExpanded = { expanded = !expanded },
                    )
                    AnimatedVisibility(
                        visible = expanded && !uiState.isSelectionMode,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut(),
                    ) {
                        ProductPricesList(product = product, stores = uiState.stores)
                    }
                }
            }
        }
    }
}

@Composable
private fun ProductCardContent(
    product: Product,
    isSelectionMode: Boolean,
    isSelected: Boolean,
    language: Language,
    expanded: Boolean,
    onToggleExpanded: () -> Unit,
) {
    val appColors = LocalAppColors.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (isSelectionMode) {
            SelectionCircle(isSelected = isSelected, appColors = appColors)
            Spacer(Modifier.width(8.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(
                text = product.productName,
                color = appColors.text,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(4.dp))
            Row {
                Tag(
                    text = t(ListLogic.unitLabelKey(product.unitOfMeasurement, 1.0), language),
                    size = TagSize.SM,
                )
            }
        }
        if (!isSelectionMode && product.prices.isNotEmpty()) {
            val rotation by animateFloatAsState(
                targetValue = if (expanded) 180f else 0f,
                animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing),
                label = "productChevronRotation",
            )
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .clickable(onClick = onToggleExpanded),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Filled.KeyboardArrowDown,
                    contentDescription = null,
                    tint = appColors.textSecondary,
                    modifier = Modifier.size(24.dp).graphicsLayer(rotationZ = rotation),
                )
            }
        }
    }
}

@Composable
private fun ProductPricesList(product: Product, stores: List<Store>) {
    val appColors = LocalAppColors.current
    SubList(modifier = Modifier.padding(top = 8.dp)) {
        product.prices.forEachIndexed { index, price ->
            val storeName = stores.firstOrNull { it.id == price.storeId }?.description ?: price.storeId
            SubListRow(
                onClick = {},
                isFirst = index == 0,
                isLast = index == product.prices.lastIndex,
            ) {
                Text(
                    text = storeName,
                    color = appColors.text,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = "$${formatAmount(price.value)}",
                    color = appColors.text,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}

private fun formatAmount(value: Double): String = String.format(java.util.Locale.US, "%.2f", value)

@Composable
private fun SelectionCircle(isSelected: Boolean, appColors: com.easypocket.mobile.ui.theme.AppColors) {
    Box(
        modifier = Modifier
            .size(24.dp)
            .clip(CircleShape)
            .background(if (isSelected) appColors.primary else androidx.compose.ui.graphics.Color.Transparent)
            .border(2.dp, appColors.textSecondary, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        if (isSelected) {
            Icon(
                Icons.Filled.Check,
                contentDescription = null,
                tint = androidx.compose.ui.graphics.Color.White,
                modifier = Modifier.size(14.dp),
            )
        }
    }
}

@Composable
private fun LoadingState(language: Language) {
    val appColors = LocalAppColors.current
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(t("common.loading", language), color = appColors.textSecondary, fontSize = 16.sp)
    }
}

@Composable
private fun EmptyState(text: String) {
    val appColors = LocalAppColors.current
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(text, color = appColors.textSecondary, fontSize = 16.sp)
    }
}
