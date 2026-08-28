package com.easypocket.mobile.ui.lists

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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
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
import com.easypocket.mobile.ui.components.IconButtonCircle
import com.easypocket.mobile.ui.components.AppItemList
import com.easypocket.mobile.ui.components.ListItemRow
import com.easypocket.mobile.ui.components.LocalToastState
import com.easypocket.mobile.ui.components.Tag
import com.easypocket.mobile.ui.components.TagSize
import com.easypocket.mobile.ui.components.ToastType
import com.easypocket.mobile.ui.theme.LocalAppColors
import com.easypocket.mobile.ui.theme.LocalIsDark
import com.easypocket.mobile.ui.theme.StoreColors
import java.util.Locale
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
    var showRename by rememberSaveable { mutableStateOf(false) }
    var renameInput by remember { mutableStateOf("") }
    var showRemoveCompleted by rememberSaveable { mutableStateOf(false) }
    var showDeleteSelected by rememberSaveable { mutableStateOf(false) }
    var showMove by rememberSaveable { mutableStateOf(false) }
    var moveLists by remember { mutableStateOf(listOf<ShoppingList>()) }

    LaunchedEffect(listId) { vm.load(listId) }

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
            onTitleClick = if (uiState.isSelectionMode) null else {
                {
                    renameInput = uiState.list?.title ?: ""
                    showRename = true
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
                    onSelectStore = { vm.setStoreFilter(it) },
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
                )

                val list = uiState.list ?: return@Column
                Box(Modifier.weight(1f).fillMaxWidth()) {
                    ItemsList(
                        uiState = uiState,
                        list = list,
                        language = language,
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
                            modifier = Modifier.align(Alignment.BottomEnd).padding(end = 16.dp, bottom = 36.dp),
                        )
                    }
                }
            }
        }
    }

    RenameSheet(
        visible = showRename,
        initialTitle = uiState.list?.title ?: "",
        input = renameInput,
        onInputChange = { renameInput = it },
        language = language,
        onSave = {
            scope.launch {
                val title = renameInput.trim()
                if (title.isNotEmpty()) {
                    vm.renameList(title)
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
    onSelectStore: (String?) -> Unit,
    onCloseSelection: () -> Unit,
    onMove: () -> Unit,
    onPin: () -> Unit,
    onDelete: () -> Unit,
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
        if (shouldShowFilterBar(uiState)) {
            StoreFilterBar(
                stores = uiState.filterStores,
                activeStoreId = uiState.storeFilter,
                onSelectStore = onSelectStore,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 12.dp, top = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TotalsBlock(total = uiState.visibleTotal, cartTotal = uiState.cartTotal, language = language)
            Spacer(Modifier.weight(1f))
            Box {
                IconButtonCircle(
                    icon = Icons.Default.MoreVert,
                    onClick = { if (uiState.hasItems) onShowMenu() },
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
                }
            }
        }
    }
}

private fun shouldShowFilterBar(uiState: ListDetailUiState): Boolean {
    val items = uiState.list?.items ?: return false
    val usedStoreCount = uiState.filterStores.size
    val hasStoreless = items.any { it.storeId == null }
    val hasStored = items.any { it.storeId != null }
    return usedStoreCount > 1 || (hasStoreless && hasStored)
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
    onItemPress: (ShoppingListItem) -> Unit,
    onItemLongPress: (ShoppingListItem) -> Unit,
    onToggleDone: (ShoppingListItem) -> Unit,
) {
    val appColors = LocalAppColors.current
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
        when {
            list.items.isEmpty() -> item {
                EmptyMessage(t("listDetail.empty", language))
            }
            uiState.pendingItems.isEmpty() && uiState.doneItems.isEmpty() -> item {
                EmptyMessage(t("listDetail.noFilterMatch", language))
            }
            else -> {
                itemsIndexed(uiState.pendingItems, key = { _, item -> item.id }) { index, item ->
                    ListItemRow(
                        onClick = { onItemPress(item) },
                        onLongClick = { onItemLongPress(item) },
                        isFirst = index == 0,
                        isLast = index == uiState.pendingItems.lastIndex,
                        backgroundColor = if (item.id in uiState.selection) appColors.surface else null,
                    ) {
                        DetailItemCardContent(
                            item = item,
                            productsById = uiState.productsById,
                            stores = uiState.stores,
                            isSelectionMode = uiState.isSelectionMode,
                            isSelected = item.id in uiState.selection,
                            language = language,
                            onToggleDone = { onToggleDone(item) },
                        )
                    }
                }
                if (hasDone) {
                    item(key = "done-section") {
                        DoneSectionHeader(language = language)
                    }
                    itemsIndexed(uiState.doneItems, key = { _, item -> item.id }) { index, item ->
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
                                onToggleDone = { onToggleDone(item) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DoneSectionHeader(language: Language) {
    val appColors = LocalAppColors.current
    Column(Modifier.fillMaxWidth()) {
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
private fun EmptyMessage(text: String) {
    val appColors = LocalAppColors.current
    Box(Modifier.fillMaxWidth().padding(top = 40.dp), contentAlignment = Alignment.Center) {
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
                    modifier = Modifier.align(Alignment.TopEnd).size(16.dp),
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
                        if (store != null) {
                            Tag(
                                text = store.description,
                                color = StoreColors.get(store.color, isDark),
                                size = TagSize.SM,
                            )
                        } else {
                            StorelessTag(text = t("listDetail.noStore", language), appColors = appColors)
                        }
                        Spacer(Modifier.weight(1f))
                        if (price * item.quantity > 0) {
                            Tag(text = "$${formatAmount(price * item.quantity)}", size = TagSize.SM)
                            Spacer(Modifier.width(6.dp))
                        }
                        Tag(text = "${ListLogic.trimQuantity(item.quantity)} $unitLabel", size = TagSize.SM)
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
            .background(if (done) textColor else Color.Transparent)
            .border(if (done) 0.dp else 2.dp, if (done) textColor else appColors.textSecondary, CircleShape)
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
    input: String,
    onInputChange: (String) -> Unit,
    language: Language,
    onSave: () -> Unit,
    onDismiss: () -> Unit,
) {
    LaunchedEffect(visible, initialTitle) {
        if (visible) onInputChange(initialTitle)
    }
    AppBottomSheet(
        visible = visible,
        onDismiss = onDismiss,
        heightFraction = 0.75f,
        title = t("listForm.editTitle", language),
    ) {
        DetailTitleInput(
            value = input,
            onValueChange = onInputChange,
            placeholder = t("listForm.placeholder", language),
        )
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
) {
    FormTextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = placeholder,
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
