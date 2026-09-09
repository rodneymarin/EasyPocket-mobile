package com.easypocket.mobile.ui.lists

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PushPin
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.easypocket.mobile.domain.Category
import com.easypocket.mobile.domain.ListIcon
import com.easypocket.mobile.domain.ListLogic
import com.easypocket.mobile.domain.Product
import com.easypocket.mobile.domain.ShoppingList
import com.easypocket.mobile.domain.ShoppingListItem
import com.easypocket.mobile.domain.Store
import com.easypocket.mobile.domain.UnitOfMeasurement
import com.easypocket.mobile.i18n.Language
import com.easypocket.mobile.i18n.LocalLanguage
import com.easypocket.mobile.i18n.t
import com.easypocket.mobile.ui.components.AppBottomSheet
import com.easypocket.mobile.ui.components.AppButton
import com.easypocket.mobile.ui.components.AppFab
import com.easypocket.mobile.ui.components.AppHeader
import com.easypocket.mobile.ui.components.ButtonVariant
import com.easypocket.mobile.ui.components.ConfirmSheet
import com.easypocket.mobile.ui.components.DropdownItem
import com.easypocket.mobile.ui.components.DropdownMenu
import com.easypocket.mobile.ui.components.FormTextField
import com.easypocket.mobile.ui.components.HeaderIconButton
import com.easypocket.mobile.ui.components.ListIconField
import com.easypocket.mobile.ui.components.IconButtonCircle
import com.easypocket.mobile.ui.components.AppItemList
import com.easypocket.mobile.ui.components.CategoryChipSelector
import com.easypocket.mobile.ui.components.ListItemRow
import com.easypocket.mobile.ui.components.inputContainerColor
import com.easypocket.mobile.ui.components.LocalToastState
import com.easypocket.mobile.ui.components.Tag
import com.easypocket.mobile.ui.components.TagSize
import com.easypocket.mobile.ui.components.ToastType
import com.easypocket.mobile.ui.components.rememberHighlightedNewItemId
import com.easypocket.mobile.ui.theme.LocalAppColors
import com.easypocket.mobile.ui.theme.LocalIsDark
import com.easypocket.mobile.ui.theme.StoreColors
import java.util.Locale
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun ListDetailScreen(navController: NavController, listId: String) {
    val vm: ListDetailViewModel = hiltViewModel()
    val uiState by vm.uiState.collectAsStateWithLifecycle()
    val language = LocalLanguage.current
    val appColors = LocalAppColors.current
    val toast = LocalToastState.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var showMenu by remember { mutableStateOf(false) }
    var showFilterSheet by rememberSaveable { mutableStateOf(false) }
    val backStackEntry by navController.currentBackStackEntryAsState()
    var showRename by rememberSaveable { mutableStateOf(false) }
    var renameInput by remember { mutableStateOf("") }
    var renameIcon by remember { mutableStateOf("") }
    var renameCategoryId by remember { mutableStateOf<String?>(null) }
    var showRemoveCompleted by rememberSaveable { mutableStateOf(false) }
    var showArchive by rememberSaveable { mutableStateOf(false) }
    var manualTotalText by rememberSaveable { mutableStateOf("") }
    var showDeleteSelected by rememberSaveable { mutableStateOf(false) }
    var showMove by rememberSaveable { mutableStateOf(false) }
    var moveLists by remember { mutableStateOf(listOf<ShoppingList>()) }
    var showQuickAdd by rememberSaveable { mutableStateOf(false) }
    var quickAddInput by remember { mutableStateOf("") }

    LaunchedEffect(listId) { vm.load(listId) }

    BackHandler(enabled = uiState.isSelectionMode) { vm.clearSelection() }

    fun enterSelection(itemId: Long) {
        vm.setSelection(setOf(itemId))
        scope.launch { moveLists = vm.moveTargetLists() }
    }

    val onBack: () -> Unit = { navController.popBackStack() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(appColors.background)
            .padding(top = 60.dp),
    ) {
        val currentList = uiState.list
        AppHeader(
            title = when {
                currentList != null -> currentList.title
                uiState.isLoading -> t("common.loading", language)
                else -> t("common.error", language)
            },
            onBack = { if (uiState.isSelectionMode) vm.clearSelection() else onBack() },
            trailing = if (uiState.isSelectionMode || currentList == null) {
                null
            } else {
                {
                    HeaderIconButton(
                        icon = Icons.Filled.Edit,
                        contentDescription = t("stores.edit", language),
                        onClick = {
                            renameInput = uiState.list?.title ?: ""
                            renameIcon = uiState.list?.icon ?: ""
                            renameCategoryId = uiState.list?.categoryId
                            showRename = true
                        },
                    )
                }
            },
            titleContent = if (uiState.isSelectionMode) {
                {
                    Text(
                        text = "${uiState.selection.size} ${t("common.selected", language)}",
                        color = appColors.text,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 52.dp),
                    )
                }
            } else null,
            leading = if (uiState.isSelectionMode) {
                {
                    HeaderCloseButton(onClick = { vm.clearSelection() })
                }
            } else null,
        )

        when {
            uiState.isLoading -> Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text(t("common.loading", language), color = appColors.textSecondary, fontSize = 16.sp)
            }
            uiState.list == null -> Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text(t("listDetail.notFound", language), color = appColors.textSecondary, fontSize = 16.sp)
            }
            else -> {
                ActionBar(
                    uiState = uiState,
                    hasAvailableMoveLists = moveLists.isNotEmpty(),
                    language = language,
                    showMenu = showMenu,
                    onShowMenu = { showMenu = true },
                    onDismissMenu = { showMenu = false },
                    onCopy = {
                        vm.copyToClipboard(context, language)
                        toast.show(t("toast.listCopied", language), ToastType.SUCCESS)
                    },
                    onUncheckAll = {
                        scope.launch {
                            vm.uncheckAll()
                            toast.show(t("toast.listUnchecked", language), ToastType.SUCCESS)
                        }
                    },
                    onRemoveCompleted = { showRemoveCompleted = true },
                    onArchive = { showArchive = true },
                    onShowFilter = { showFilterSheet = true },
                    onCloseSelection = { vm.clearSelection() },
                    onMove = {
                        scope.launch { moveLists = vm.moveTargetLists() }
                        showMove = true
                    },
                    onPin = {
                        scope.launch {
                            vm.pinSelected(!uiState.allSelectedPinned)
                        }
                    },
                    onDelete = { showDeleteSelected = true },
                    onQuickAdd = { showQuickAdd = true },
                )

                val list = uiState.list ?: return@Column
                Box(Modifier.weight(1f).fillMaxWidth()) {
                    ItemsList(
                        uiState = uiState,
                        list = list,
                        language = language,
                        navBackStackEntry = backStackEntry,
                        onItemPress = { item ->
                            if (uiState.isSelectionMode) {
                                vm.setSelection(uiState.selection.toggle(item.id))
                            } else {
                                navController.navigate("itemForm/$listId/${item.id}")
                            }
                        },
                        onItemLongPress = { item ->
                            if (!uiState.isSelectionMode) enterSelection(item.id)
                        },
                        onToggleDone = { item ->
                            scope.launch { vm.toggleDone(item) }
                        },
                    )
                    if (!uiState.isSelectionMode) {
                        AppFab(
                            icon = Icons.Default.Add,
                            onClick = { navController.navigate("itemForm/$listId/-1") },
                            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
                        )
                    }
                }
            }
        }
    }

    FilterSheet(
        visible = showFilterSheet,
        stores = uiState.filterStores,
        categories = uiState.filterCategories,
        activeStoreId = uiState.storeFilter,
        activeCategoryId = uiState.categoryFilter,
        language = language,
        onSelectStore = { vm.setStoreFilter(it) },
        onSelectCategory = { vm.setCategoryFilter(it) },
        onDismiss = { showFilterSheet = false },
    )

    RenameSheet(
        visible = showRename,
        initialTitle = uiState.list?.title ?: "",
        initialIcon = uiState.list?.icon ?: "",
        initialCategoryId = uiState.list?.categoryId,
        categories = uiState.categoriesById.values.toList(),
        input = renameInput,
        onInputChange = { renameInput = it },
        icon = renameIcon,
        onIconChange = { renameIcon = it },
        categoryId = renameCategoryId,
        onCategoryChange = { renameCategoryId = it },
        language = language,
        onSave = {
            scope.launch {
                val title = renameInput.trim()
                if (title.isNotEmpty()) {
                    vm.renameList(title, renameIcon.ifBlank { ListIcon.DEFAULT }, renameCategoryId)
                    showRename = false
                    toast.show(t("toast.listRenamed", language), ToastType.SUCCESS)
                }
            }
        },
        onDismiss = { showRename = false },
    )

    ConfirmSheet(
        visible = showRemoveCompleted,
        title = t("listDetail.removeCompleted", language),
        message = t("listDetail.removeCompletedConfirmMessage", language),
        confirmLabel = t("listDetail.removeConfirm", language),
        onConfirm = {
            scope.launch {
                vm.removeCompleted()
                showRemoveCompleted = false
                toast.show(t("toast.completedDeleted", language), ToastType.SUCCESS)
            }
        },
        onDismiss = { showRemoveCompleted = false },
    )

    ConfirmSheet(
        visible = showArchive,
        title = t("listDetail.archiveToHistory", language),
        message = t("listDetail.archiveConfirmMessage", language),
        confirmLabel = t("listDetail.archiveConfirm", language),
        heightFraction = if (uiState.doneItemsTotal == 0.0) 0.45f else 0.35f,
        extraContent = if (uiState.doneItemsTotal == 0.0) {
            {
                Column {
                    Text(
                        text = t("listDetail.archiveManualTotalLabel", language),
                        color = appColors.textSecondary,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(8.dp))
                    FormTextField(
                        value = manualTotalText,
                        onValueChange = { manualTotalText = it },
                        placeholder = t("listDetail.archiveManualTotalPlaceholder", language),
                        keyboardType = KeyboardType.Decimal,
                    )
                }
            }
        } else {
            null
        },
        onConfirm = {
            scope.launch {
                vm.archiveCompleted(manualTotal = manualTotalText.toDoubleOrNull()?.takeIf { it > 0.0 })
                showArchive = false
                manualTotalText = ""
                toast.show(t("toast.listArchived", language), ToastType.SUCCESS)
            }
        },
        onDismiss = {
            showArchive = false
            manualTotalText = ""
        },
    )

    ConfirmSheet(
        visible = showDeleteSelected,
        title = t("listDetail.confirmDeleteSelected", language),
        message = t("listDetail.confirmDeleteSelectedMessage", language, mapOf("count" to uiState.selection.size.toString())),
        confirmLabel = t("listDetail.removeConfirm", language),
        onConfirm = {
            scope.launch {
                vm.deleteSelected()
                showDeleteSelected = false
                toast.show(t("toast.itemsDeleted", language), ToastType.SUCCESS)
            }
        },
        onDismiss = { showDeleteSelected = false },
    )

    MoveItemsSheet(
        visible = showMove,
        lists = moveLists,
        language = language,
        onMoveToList = { targetId ->
            scope.launch {
                vm.moveSelected(targetId)
                showMove = false
                toast.show(t("toast.itemsMoved", language), ToastType.SUCCESS)
            }
        },
        onDismiss = { showMove = false },
    )

    QuickAddSheet(
        visible = showQuickAdd,
        input = quickAddInput,
        onInputChange = { quickAddInput = it },
        language = language,
        onAdd = {
            scope.launch {
                val added = vm.quickAdd(quickAddInput)
                showQuickAdd = false
                quickAddInput = ""
                if (added > 0) {
                    toast.show(
                        t("toast.quickAddCompleted", language, mapOf("count" to added.toString())),
                        ToastType.SUCCESS,
                    )
                } else {
                    toast.show(t("toast.quickAddNoMatch", language), ToastType.WARNING)
                }
            }
        },
        onDismiss = { showQuickAdd = false },
    )
}

private fun Set<Long>.toggle(id: Long): Set<Long> =
    if (id in this) this - id else this + id

@Composable
private fun HeaderCloseButton(onClick: () -> Unit) {
    val appColors = LocalAppColors.current
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(appColors.surface)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            Icons.Default.Close,
            contentDescription = null,
            tint = appColors.text,
            modifier = Modifier.size(20.dp),
        )
    }
}

@Composable
private fun ActionBar(
    uiState: ListDetailUiState,
    hasAvailableMoveLists: Boolean,
    language: Language,
    showMenu: Boolean,
    onShowMenu: () -> Unit,
    onDismissMenu: () -> Unit,
    onCopy: () -> Unit,
    onUncheckAll: () -> Unit,
    onRemoveCompleted: () -> Unit,
    onArchive: () -> Unit,
    onShowFilter: () -> Unit,
    onCloseSelection: () -> Unit,
    onMove: () -> Unit,
    onPin: () -> Unit,
    onDelete: () -> Unit,
    onQuickAdd: () -> Unit,
) {
    if (uiState.isSelectionMode) {
        SelectionActionsBar(
            selectedCount = uiState.selection.size,
            allPinned = uiState.allSelectedPinned,
            hasAvailableLists = hasAvailableMoveLists,
            onClose = onCloseSelection,
            onMove = onMove,
            onPin = onPin,
            onDelete = onDelete,
            language = language,
        )
        return
    }

    Column {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 12.dp, top = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TotalsBlock(total = uiState.visibleTotal, cartTotal = uiState.cartTotal, language = language)
            Spacer(Modifier.weight(1f))
            val activeStore = uiState.storeFilter?.let { id -> uiState.stores.firstOrNull { it.id == id } }
            val activeCategory = uiState.categoryFilter?.let { id -> uiState.categoriesById[id] }
            if (activeStore != null || activeCategory != null) {
                FilterChipButton(
                    storeName = activeStore?.description,
                    categoryName = activeCategory?.name,
                    onClick = onShowFilter,
                )
                Spacer(Modifier.width(8.dp))
            } else {
                IconButtonCircle(
                    icon = Icons.Default.FilterList,
                    onClick = onShowFilter,
                    variant = ButtonVariant.SECONDARY,
                )
                Spacer(Modifier.width(8.dp))
            }
            Box {
                IconButtonCircle(
                    icon = Icons.Default.MoreVert,
                    onClick = onShowMenu,
                    variant = ButtonVariant.SECONDARY,
                )
                DropdownMenu(
                    expanded = showMenu,
                    onDismiss = onDismissMenu,
                ) {
                    DropdownItem(
                        label = t("listDetail.copyList", language),
                        onClick = {
                            onDismissMenu()
                            onCopy()
                        },
                    )
                    DropdownItem(
                        label = t("listDetail.quickAdd", language),
                        onClick = {
                            onDismissMenu()
                            onQuickAdd()
                        },
                    )
                    DropdownItem(
                        label = t("listDetail.menuUncheckAll", language),
                        onClick = {
                            onDismissMenu()
                            onUncheckAll()
                        },
                        enabled = uiState.hasDoneItems,
                    )
                    DropdownItem(
                        label = t("listDetail.removeCompleted", language),
                        onClick = {
                            onDismissMenu()
                            onRemoveCompleted()
                        },
                        enabled = uiState.hasDoneItems,
                    )
                    DropdownItem(
                        label = t("listDetail.archiveToHistory", language),
                        onClick = {
                            onDismissMenu()
                            onArchive()
                        },
                        enabled = uiState.hasDoneItems,
                    )
                }
            }
        }
    }
}

@Composable
private fun FilterChipButton(storeName: String?, categoryName: String?, onClick: () -> Unit) {
    val appColors = LocalAppColors.current
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(appColors.primary)
            .clickable(onClick = onClick)
            .height(IntrinsicSize.Min)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (storeName != null) {
            Text(
                text = storeName,
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (storeName != null && categoryName != null) {
            Box(
                Modifier
                    .padding(horizontal = 8.dp)
                    .fillMaxHeight()
                    .width(1.dp)
                    .background(Color.White.copy(alpha = 0.4f)),
            )
        }
        if (categoryName != null) {
            Text(
                text = categoryName,
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun FilterOptionChip(text: String, selected: Boolean, onClick: () -> Unit) {
    val appColors = LocalAppColors.current
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(if (selected) appColors.primary else Color.Transparent)
            .border(1.dp, if (selected) Color.Transparent else appColors.border, RoundedCornerShape(999.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 6.dp),
    ) {
        Text(
            text = text,
            color = if (selected) Color.White else appColors.textSecondary,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FilterSheet(
    visible: Boolean,
    stores: List<Store>,
    categories: List<Category>,
    activeStoreId: String?,
    activeCategoryId: String?,
    language: Language,
    onSelectStore: (String?) -> Unit,
    onSelectCategory: (String?) -> Unit,
    onDismiss: () -> Unit,
) {
    AppBottomSheet(visible = visible, onDismiss = onDismiss, title = t("listDetail.filterTitle", language)) {
        FilterSectionHeader(t("listDetail.filterStores", language))
        FlowRow(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            FilterOptionChip(
                text = t("listDetail.filterAllStores", language),
                selected = activeStoreId == null,
                onClick = { onSelectStore(null) },
            )
            stores.forEach { store ->
                FilterOptionChip(
                    text = store.description,
                    selected = activeStoreId == store.id,
                    onClick = { onSelectStore(store.id) },
                )
            }
        }
        FilterSectionHeader(t("listDetail.filterCategories", language), topSpacing = 16.dp)
        FlowRow(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            FilterOptionChip(
                text = t("listDetail.filterAllCategories", language),
                selected = activeCategoryId == null,
                onClick = { onSelectCategory(null) },
            )
            categories.forEach { category ->
                FilterOptionChip(
                    text = "${category.icon} ${category.name}",
                    selected = activeCategoryId == category.id,
                    onClick = { onSelectCategory(category.id) },
                )
            }
        }
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun FilterSectionHeader(text: String, topSpacing: Dp = 0.dp) {
    val appColors = LocalAppColors.current
    Column {
        Spacer(Modifier.height(topSpacing))
        Text(
            text = text,
            color = appColors.textSecondary,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 1.sp,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        )
    }
}

@Composable
private fun TotalsBlock(total: Double, cartTotal: Double, language: Language) {
    val appColors = LocalAppColors.current
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(t("listDetail.globalTotal", language), color = appColors.text, fontSize = 17.sp, fontWeight = FontWeight.Medium)
            Spacer(Modifier.width(6.dp))
            Text("$${formatAmount(total)}", color = appColors.text, fontSize = 17.sp, fontWeight = FontWeight.Medium)
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(t("listDetail.cartTotal", language), color = appColors.textSecondary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            Spacer(Modifier.width(6.dp))
            Text("$${formatAmount(cartTotal)}", color = appColors.textSecondary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
private fun SelectionActionsBar(
    selectedCount: Int,
    allPinned: Boolean,
    hasAvailableLists: Boolean,
    onClose: () -> Unit,
    onMove: () -> Unit,
    onPin: () -> Unit,
    onDelete: () -> Unit,
    language: Language,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (hasAvailableLists) {
            AppButton(
                text = t("listDetail.moveSelected", language),
                onClick = onMove,
            )
        }
        AppButton(
            text = t(if (allPinned) "listDetail.unpinSelected" else "listDetail.pinSelected", language),
            onClick = onPin,
        )
        AppButton(
            text = "${t("listDetail.removeConfirm", language)} ($selectedCount)",
            onClick = onDelete,
            variant = ButtonVariant.DESTRUCTIVE,
        )
    }
}

@Composable
private fun ItemsList(
    uiState: ListDetailUiState,
    list: ShoppingList,
    language: Language,
    navBackStackEntry: NavBackStackEntry?,
    onItemPress: (ShoppingListItem) -> Unit,
    onItemLongPress: (ShoppingListItem) -> Unit,
    onToggleDone: (ShoppingListItem) -> Unit,
) {
    val appColors = LocalAppColors.current
    val listState = rememberLazyListState()
    var flyingItemId by remember { mutableStateOf<Long?>(null) }
    LaunchedEffect(flyingItemId) {
        if (flyingItemId != null) {
            delay(900)
            flyingItemId = null
        }
    }
    val handleToggleDone: (ShoppingListItem) -> Unit = { item ->
        flyingItemId = item.id
        onToggleDone(item)
    }
    val placementSpec = spring(
        dampingRatio = 0.6f,
        stiffness = Spring.StiffnessMediumLow,
        visibilityThreshold = IntOffset.VisibilityThreshold,
    )
    val highlightedItemId = rememberHighlightedNewItemId(
        navBackStackEntry = navBackStackEntry,
        displayedIds = uiState.pendingItems.map { it.id },
        listState = listState,
    )
    AppItemList(
        footerText = t(
            "list.items",
            language,
            mapOf(
                "completed" to uiState.doneItems.size.toString(),
                "count" to uiState.visibleItems.size.toString(),
            ),
        ),
    ) {
        val hasDone = uiState.doneItems.isNotEmpty()
        val lockedCategory = list.categoryId?.let { id -> uiState.categoriesById[id] }
        when {
            list.items.isEmpty() -> item {
                lockedCategory?.let { category ->
                    CategorySectionHeader(
                        icon = category.icon,
                        name = category.name,
                        lockedSuffix = t("listDetail.categoryForWholeList", language),
                    )
                }
                EmptyMessage(t("listDetail.empty", language), topPadding = if (lockedCategory != null) 16.dp else 40.dp)
            }
            uiState.pendingItems.isEmpty() && uiState.doneItems.isEmpty() -> item {
                EmptyMessage(t("listDetail.noFilterMatch", language))
            }
            else -> {
                uiState.pendingSections.forEach { section ->
                    section.category?.let { category ->
                        item(key = "category_${category.id}") {
                            Box(Modifier.animateItem(fadeInSpec = tween(200), fadeOutSpec = tween(200), placementSpec = placementSpec)) {
                                CategorySectionHeader(
                                    icon = category.icon,
                                    name = category.name,
                                    lockedSuffix = if (category.id == list.categoryId) {
                                        t("listDetail.categoryForWholeList", language)
                                    } else null,
                                )
                            }
                        }
                    } ?: item(key = "category_none") {
                        Box(Modifier.animateItem(fadeInSpec = tween(200), fadeOutSpec = tween(200), placementSpec = placementSpec)) {
                            CategorySectionHeader(icon = null, name = t("listDetail.noCategory", language))
                        }
                    }
                    itemsIndexed(section.items, key = { _, item -> item.id }) { index, item ->
                        val isFlying = flyingItemId == item.id
                        FlyingItemContainer(
                            isFlying = isFlying,
                            modifier = Modifier
                                .zIndex(if (isFlying) 1f else 0f)
                                .animateItem(fadeInSpec = tween(200), fadeOutSpec = tween(200), placementSpec = placementSpec),
                        ) {
                            ListItemRow(
                                onClick = { onItemPress(item) },
                                onLongClick = { onItemLongPress(item) },
                                isFirst = index == 0,
                                isLast = index == section.items.lastIndex,
                                backgroundColor = if (item.id in uiState.selection) appColors.surface else null,
                                highlighted = item.id == highlightedItemId,
                            ) {
                                DetailItemCardContent(
                                    item = item,
                                    productsById = uiState.productsById,
                                    stores = uiState.stores,
                                    isSelectionMode = uiState.isSelectionMode,
                                    isSelected = item.id in uiState.selection,
                                    language = language,
                                    onToggleDone = { handleToggleDone(item) },
                                )
                            }
                        }
                    }
                }
                if (hasDone) {
                    item(key = "done-section") {
                        Box(Modifier.animateItem(fadeInSpec = tween(200), fadeOutSpec = tween(200), placementSpec = placementSpec)) {
                            DoneSectionHeader(language = language)
                        }
                    }
                    itemsIndexed(uiState.doneItems, key = { _, item -> item.id }) { index, item ->
                        val isFlying = flyingItemId == item.id
                        FlyingItemContainer(
                            isFlying = isFlying,
                            modifier = Modifier
                                .zIndex(if (isFlying) 1f else 0f)
                                .animateItem(fadeInSpec = tween(200), fadeOutSpec = tween(200), placementSpec = placementSpec),
                        ) {
                            ListItemRow(
                                onClick = { onItemPress(item) },
                                onLongClick = { onItemLongPress(item) },
                                isFirst = index == 0,
                                isLast = index == uiState.doneItems.lastIndex,
                                backgroundColor = if (item.id in uiState.selection) appColors.surface else null,
                            ) {
                                DetailItemCardContent(
                                    item = item,
                                    productsById = uiState.productsById,
                                    stores = uiState.stores,
                                    isSelectionMode = uiState.isSelectionMode,
                                    isSelected = item.id in uiState.selection,
                                    language = language,
                                    onToggleDone = { handleToggleDone(item) },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FlyingItemContainer(
    isFlying: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    val lift = remember { Animatable(0f) }
    LaunchedEffect(isFlying) {
        if (isFlying) {
            lift.animateTo(1f, tween(durationMillis = 180, easing = FastOutSlowInEasing))
            lift.animateTo(0f, spring(dampingRatio = 0.55f, stiffness = Spring.StiffnessMediumLow))
        } else if (lift.value > 0f) {
            lift.animateTo(0f, tween(durationMillis = 120))
        }
    }
    Box(
        modifier = modifier.graphicsLayer {
            val scale = 1f + 0.05f * lift.value
            scaleX = scale
            scaleY = scale
            shape = RoundedCornerShape(20.dp)
            shadowElevation = lift.value * 18.dp.toPx()
            clip = false
        },
        content = content,
    )
}

@Composable
private fun CategorySectionHeader(icon: String?, name: String, lockedSuffix: String? = null) {
    val appColors = LocalAppColors.current
    Column(Modifier.fillMaxWidth()) {
        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (icon != null) {
                Text(text = icon, fontSize = 13.sp)
                Spacer(Modifier.width(6.dp))
            }
            Text(
                text = name,
                color = appColors.textSecondary,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.sp,
            )
            if (lockedSuffix != null) {
                Spacer(Modifier.width(4.dp))
                Text(
                    text = lockedSuffix,
                    color = appColors.placeholderText,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Normal,
                )
            }
        }
        Spacer(Modifier.height(4.dp))
    }
}

@Composable
private fun DoneSectionHeader(language: Language) {
    val appColors = LocalAppColors.current
    Column(Modifier.fillMaxWidth()) {
        Spacer(Modifier.height(16.dp))
        Box(Modifier.fillMaxWidth().height(1.dp).background(appColors.border))
        Spacer(Modifier.height(8.dp))
        Text(
            text = t("listDetail.doneSection", language),
            color = appColors.textSecondary.copy(alpha = 0.8f),
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 1.sp,
            modifier = Modifier.padding(horizontal = 16.dp),
        )
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun EmptyMessage(text: String, topPadding: Dp = 40.dp) {
    val appColors = LocalAppColors.current
    Box(Modifier.fillMaxWidth().padding(top = topPadding), contentAlignment = Alignment.Center) {
        Text(text, color = appColors.textSecondary, fontSize = 16.sp, textAlign = TextAlign.Center)
    }
}

@Composable
private fun DetailItemCardContent(
    item: ShoppingListItem,
    productsById: Map<String, Product>,
    stores: List<Store>,
    isSelectionMode: Boolean,
    isSelected: Boolean,
    language: Language,
    onToggleDone: () -> Unit,
) {
    val appColors = LocalAppColors.current
    val isDark = LocalIsDark.current
    val product = productsById[item.productId]
    val name = product?.productName ?: t("common.unknown", language)
    val store = item.storeId?.let { sid -> stores.firstOrNull { it.id == sid } }
    val unit = product?.unitOfMeasurement ?: UnitOfMeasurement.UNIT
    val unitKey = ListLogic.unitLabelKey(unit, item.quantity)
    val unitLabel = t(unitKey, language).let { if (it != unitKey) it else unit.raw }
    val price = item.storeId?.let { sid -> product?.prices?.firstOrNull { it.storeId == sid }?.value } ?: 0.0
    val showPin = !item.done && item.pinned

    Box(Modifier.fillMaxWidth()) {
            if (showPin) {
                Icon(
                    Icons.Default.PushPin,
                    contentDescription = null,
                    tint = appColors.primary,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = 10.dp, y = (-3).dp)
                        .size(16.dp),
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (isSelectionMode) {
                    SelectionCircle(selected = isSelected)
                }
                Spacer(Modifier.width(if (isSelectionMode) 10.dp else 0.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        text = name,
                        color = if (item.done) appColors.placeholderText else appColors.text,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Normal,
                        textDecoration = if (item.done) TextDecoration.LineThrough else null,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(Modifier.weight(1f)) {
                            if (store != null) {
                                Tag(
                                    text = store.description,
                                    color = if (item.done) null else StoreColors.get(store.color, isDark),
                                    size = TagSize.SM,
                                )
                            } else {
                                StorelessTag(text = t("listDetail.noStore", language), appColors = appColors)
                            }
                        }
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = buildString {
                                if (price * item.quantity > 0) {
                                    append("$")
                                    append(formatAmount(price * item.quantity))
                                    append(" | ")
                                }
                                append(ListLogic.trimQuantity(item.quantity))
                                append(" ")
                                append(unitLabel)
                            },
                            color = if (item.done) appColors.placeholderText else appColors.text,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Normal,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                Spacer(Modifier.width(6.dp))
                if (!isSelectionMode) {
                    CheckCircle(done = item.done, onToggle = onToggleDone, textColor = appColors.primary)
                }
            }
    }
}

@Composable
private fun StorelessTag(text: String, appColors: com.easypocket.mobile.ui.theme.AppColors) {
    Box(
        modifier = Modifier
            .background(appColors.surface, RoundedCornerShape(999.dp))
            .padding(horizontal = 10.dp, vertical = 1.dp),
    ) {
        Text(text, color = appColors.textSecondary, fontSize = 11.sp, fontStyle = FontStyle.Italic)
    }
}

@Composable
private fun CheckCircle(done: Boolean, onToggle: () -> Unit, textColor: Color) {
    val appColors = LocalAppColors.current
    Box(
        modifier = Modifier
            .size(24.dp)
            .clip(CircleShape)
            .background(if (done) appColors.textSecondary.copy(alpha = 0.35f) else Color.Transparent)
            .border(if (done) 0.dp else 1.dp, if (done) Color.Transparent else appColors.primary, CircleShape)
            .clickable(onClick = onToggle),
        contentAlignment = Alignment.Center,
    ) {
        if (done) {
            Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
        }
    }
}

@Composable
private fun SelectionCircle(selected: Boolean) {
    val appColors = LocalAppColors.current
    Box(
        modifier = Modifier
            .size(24.dp)
            .clip(CircleShape)
            .background(if (selected) appColors.primary else Color.Transparent)
            .border(if (selected) 0.dp else 2.dp, appColors.textSecondary, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        if (selected) {
            Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
        }
    }
}

@Composable
private fun RenameSheet(
    visible: Boolean,
    initialTitle: String,
    initialIcon: String,
    initialCategoryId: String?,
    categories: List<Category>,
    input: String,
    onInputChange: (String) -> Unit,
    icon: String,
    onIconChange: (String) -> Unit,
    categoryId: String?,
    onCategoryChange: (String?) -> Unit,
    language: Language,
    onSave: () -> Unit,
    onDismiss: () -> Unit,
) {
    val appColors = LocalAppColors.current
    LaunchedEffect(visible, initialTitle, initialIcon, initialCategoryId) {
        if (visible) {
            onInputChange(initialTitle)
            onIconChange(initialIcon)
            onCategoryChange(initialCategoryId)
        }
    }
    AppBottomSheet(
        visible = visible,
        onDismiss = onDismiss,
        heightFraction = 0.85f,
        title = t("listForm.editTitle", language),
    ) {
        DetailTitleInput(
            value = input,
            onValueChange = onInputChange,
            placeholder = t("listForm.placeholder", language),
            autoFocus = true,
        )
        Spacer(Modifier.height(16.dp))
        ListIconField(
            value = icon,
            onValueChange = onIconChange,
            placeholder = t("listForm.icon", language),
        )
        if (categories.isNotEmpty()) {
            Spacer(Modifier.height(16.dp))
            Text(
                text = t("products.categoryLabel", language),
                color = appColors.text,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(8.dp))
            CategoryChipSelector(
                categories = categories,
                selectedCategoryId = categoryId,
                onSelect = onCategoryChange,
            )
        }
        Spacer(Modifier.height(16.dp))
        AppButton(
            text = t("common.save", language),
            onClick = onSave,
            modifier = Modifier.fillMaxWidth(),
            enabled = input.trim().isNotEmpty(),
        )
    }
}

@Composable
private fun DetailTitleInput(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    autoFocus: Boolean = false,
) {
    FormTextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = placeholder,
        autoFocus = autoFocus,
    )
}

@Composable
private fun QuickAddSheet(
    visible: Boolean,
    input: String,
    onInputChange: (String) -> Unit,
    language: Language,
    onAdd: () -> Unit,
    onDismiss: () -> Unit,
) {
    AppBottomSheet(
        visible = visible,
        onDismiss = onDismiss,
        heightFraction = 0.6f,
        title = t("listDetail.quickAdd", language),
    ) {
        QuickAddInput(
            value = input,
            onValueChange = onInputChange,
            placeholder = t("listDetail.quickAddPlaceholder", language),
            modifier = Modifier.weight(1f),
        )
        Spacer(Modifier.height(16.dp))
        AppButton(
            text = t("common.add", language),
            onClick = onAdd,
            enabled = input.trim().isNotEmpty(),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun QuickAddInput(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
) {
    val appColors = LocalAppColors.current
    val scrollState = rememberScrollState()
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        textStyle = TextStyle(color = appColors.text, fontSize = 14.sp),
        cursorBrush = SolidColor(appColors.primary),
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(inputContainerColor())
            .verticalScroll(scrollState)
            .heightIn(min = 26.dp * 8)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        decorationBox = { innerField ->
            Box {
                if (value.isEmpty()) {
                    Text(placeholder, color = appColors.placeholderText, fontSize = 14.sp)
                }
                innerField()
            }
        },
    )
}

@Composable
private fun MoveItemsSheet(
    visible: Boolean,
    lists: List<ShoppingList>,
    language: Language,
    onMoveToList: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val appColors = LocalAppColors.current
    AppBottomSheet(
        visible = visible,
        onDismiss = onDismiss,
        heightFraction = 0.6f,
        title = t("listDetail.moveSelectedTitle", language),
    ) {
        if (lists.isEmpty()) {
            Box(Modifier.fillMaxWidth().padding(vertical = 24.dp), contentAlignment = Alignment.Center) {
                Text(t("listDetail.moveNoLists", language), color = appColors.textSecondary, fontSize = 15.sp)
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                lists.forEach { target ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(appColors.surface)
                            .border(1.dp, appColors.border, RoundedCornerShape(10.dp))
                            .clickable { onMoveToList(target.id) }
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = target.title,
                            color = appColors.text,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.weight(1f),
                        )
                        Icon(
                            Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = null,
                            tint = appColors.textSecondary,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
            }
        }
    }
}

private fun formatAmount(value: Double): String = String.format(Locale.US, "%.2f", value)
