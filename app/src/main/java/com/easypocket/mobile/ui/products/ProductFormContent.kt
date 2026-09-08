package com.easypocket.mobile.ui.products

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.easypocket.mobile.domain.Store
import com.easypocket.mobile.domain.UnitOfMeasurement
import com.easypocket.mobile.i18n.LocalLanguage
import com.easypocket.mobile.i18n.t
import com.easypocket.mobile.ui.components.AppButton
import com.easypocket.mobile.ui.components.AppHeader
import com.easypocket.mobile.ui.components.ButtonVariant
import com.easypocket.mobile.ui.components.ConfirmSheet
import com.easypocket.mobile.ui.components.AppBottomSheet
import com.easypocket.mobile.ui.components.FormTextField
import com.easypocket.mobile.ui.components.IconButtonCircle
import com.easypocket.mobile.ui.components.ListItemGroup
import com.easypocket.mobile.ui.components.ListItemRow
import com.easypocket.mobile.ui.components.SelectField
import com.easypocket.mobile.ui.components.SelectOption
import com.easypocket.mobile.ui.theme.AppColors
import com.easypocket.mobile.ui.theme.LocalAppColors
import kotlinx.coroutines.launch

@Composable
fun ProductFormContent(
    vm: ProductFormViewModel,
    isSheet: Boolean = false,
    autoFocusName: Boolean = false,
    onSaved: () -> Unit,
    onDeleted: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by vm.uiState.collectAsStateWithLifecycle()
    val language = LocalLanguage.current
    val appColors = LocalAppColors.current
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var rootCoordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }
    var nameFieldCoordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }
    val clearFocusOnOtherTap = Modifier
        .onGloballyPositioned { rootCoordinates = it }
        .pointerInput(Unit) {
            awaitEachGesture {
                val down = awaitFirstDown(pass = PointerEventPass.Initial, requireUnconsumed = false)
                val root = rootCoordinates
                val field = nameFieldCoordinates
                val tappedNameField = root != null && field != null &&
                    runCatching { root.localBoundingBoxOf(field) }.getOrNull()
                        ?.contains(down.position) == true
                if (!tappedNameField) focusManager.clearFocus()
            }
        }

    ConfirmSheet(
        visible = showDeleteConfirm,
        title = t("products.deleteModal.title", language),
        message = t("products.deleteModal.confirmMessage", language, mapOf("product" to state.name)),
        warning = t("products.deleteModal.warning", language),
        confirmLabel = t("products.deleteModal.confirm", language),
        onConfirm = {
            showDeleteConfirm = false
            scope.launch { vm.delete(onDeleted) }
        },
        onDismiss = { showDeleteConfirm = false },
    )

    if (isSheet) {
        Column(
            modifier = modifier
                .then(clearFocusOnOtherTap)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .imePadding(),
        ) {
            ProductFormFields(
                state = state,
                vm = vm,
                language = language,
                appColors = appColors,
                autoFocusName = autoFocusName,
                onNameFieldPositioned = { nameFieldCoordinates = it },
            )
            Spacer(Modifier.height(24.dp))
            ProductFormActions(
                state = state,
                vm = vm,
                onSaved = onSaved,
                onDeleted = onDeleted,
                onCancel = onCancel,
                onDeleteRequest = { showDeleteConfirm = true },
                language = language,
                showCancel = false,
            )
        }
    } else {
        Column(
            modifier = modifier
                .then(clearFocusOnOtherTap)
                .fillMaxSize()
                .background(appColors.background)
                .padding(top = 60.dp),
        ) {
            AppHeader(
                title = t(if (state.isEdit) "products.editTitle" else "products.addTitle", language),
                onBack = onCancel,
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp),
            ) {
                ProductFormFields(
                    state = state,
                    vm = vm,
                    language = language,
                    appColors = appColors,
                    autoFocusName = autoFocusName,
                    onNameFieldPositioned = { nameFieldCoordinates = it },
                )
            }
            Column(
                Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 16.dp),
            ) {
                ProductFormActions(
                    state = state,
                    vm = vm,
                    onSaved = onSaved,
                    onDeleted = onDeleted,
                    onCancel = onCancel,
                    onDeleteRequest = { showDeleteConfirm = true },
                    language = language,
                    showCancel = true,
                )
            }
        }
    }
}

@Composable
private fun ProductFormFields(
    state: ProductFormUiState,
    vm: ProductFormViewModel,
    language: com.easypocket.mobile.i18n.Language,
    appColors: AppColors,
    autoFocusName: Boolean,
    onNameFieldPositioned: (LayoutCoordinates?) -> Unit,
) {
    ProductNameInput(
        value = state.name,
        onValueChange = vm::setName,
        placeholder = t("products.addModal.namePlaceholder", language),
        isError = state.nameError,
        errorMessage = t("toast.productNameExists", language),
        autoFocus = autoFocusName,
        onFieldPositioned = onNameFieldPositioned,
    )
    Spacer(Modifier.height(16.dp))
    FieldLabel(text = t("products.addModal.unitLabel", language))
    Spacer(Modifier.height(6.dp))
    val unitOptions = UnitOfMeasurement.entries.map { SelectOption(it.raw, t("unit.${it.raw}", language)) }
    SelectField(
        options = unitOptions,
        onSelect = { id -> UnitOfMeasurement.fromRaw(id)?.let(vm::setUnit) },
        selectedId = state.unit.raw,
        placeholder = t("products.addModal.unitLabel", language),
        sheetTitle = t("products.addModal.unitLabel", language),
    )

    Spacer(Modifier.height(20.dp))
    PricesSection(state = state, vm = vm, language = language, appColors = appColors)
}

@Composable
private fun ProductFormActions(
    state: ProductFormUiState,
    vm: ProductFormViewModel,
    onSaved: () -> Unit,
    onDeleted: () -> Unit,
    onCancel: () -> Unit,
    onDeleteRequest: () -> Unit,
    language: com.easypocket.mobile.i18n.Language,
    showCancel: Boolean = true,
) {
    val scope = rememberCoroutineScope()
    Column(Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (state.isEdit) {
                AppButton(
                    text = t("products.delete", language),
                    onClick = onDeleteRequest,
                    variant = ButtonVariant.DESTRUCTIVE,
                    modifier = Modifier.weight(1f),
                )
            }
            if (showCancel) {
                AppButton(
                    text = t("products.addModal.cancel", language),
                    onClick = onCancel,
                    variant = ButtonVariant.SECONDARY,
                    modifier = Modifier.weight(1f),
                )
            }
            AppButton(
                text = t("products.addModal.save", language),
                onClick = { scope.launch { vm.save { onSaved() } } },
                enabled = state.name.isNotBlank(),
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun ProductNameInput(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    isError: Boolean,
    errorMessage: String,
    autoFocus: Boolean,
    onFieldPositioned: (LayoutCoordinates?) -> Unit,
) {
    val appColors = LocalAppColors.current
    Column(Modifier.fillMaxWidth()) {
        FormTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = placeholder,
            isError = isError,
            autoFocus = autoFocus,
            modifier = Modifier.onGloballyPositioned { onFieldPositioned(it) },
        )
        if (isError) {
            Spacer(Modifier.height(6.dp))
            Text(errorMessage, color = appColors.destructiveBorder, fontSize = 13.sp)
        }
    }
}

@Composable
private fun FieldLabel(text: String) {
    val appColors = LocalAppColors.current
    Text(text, color = appColors.text, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
}

@Composable
private fun PricesSection(
    state: ProductFormUiState,
    vm: ProductFormViewModel,
    language: com.easypocket.mobile.i18n.Language,
    appColors: AppColors,
) {
    var showAddPrice by remember { mutableStateOf(false) }
    var editingStoreId by remember { mutableStateOf<String?>(null) }
    var pendingDeleteStoreId by remember { mutableStateOf<String?>(null) }
    var newStoreId by remember { mutableStateOf<String?>(null) }
    var newPriceText by remember { mutableStateOf("") }

    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            t("products.pricesSection", language),
            color = appColors.text,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1f),
        )
        if (state.availableStores.isNotEmpty()) {
            IconButtonCircle(
                icon = Icons.Default.Add,
                onClick = {
                    newStoreId = null
                    newPriceText = ""
                    showAddPrice = true
                },
                variant = ButtonVariant.SECONDARY,
            )
        }
    }
    Spacer(Modifier.height(8.dp))

    if (state.prices.isNotEmpty()) {
        ListItemGroup(modifier = Modifier.padding(top = 4.dp)) {
            state.prices.forEachIndexed { index, row ->
                ListItemRow(
                    onClick = {
                        editingStoreId = row.storeId
                        newPriceText = row.value
                    },
                    isFirst = index == 0,
                    isLast = index == state.prices.lastIndex,
                ) {
                    PriceCardContent(
                        storeName = row.storeName,
                        value = row.value,
                        onRemove = { pendingDeleteStoreId = row.storeId },
                    )
                }
            }
        }
    }

    val noStores = state.availableStores.isEmpty() && state.prices.isEmpty()
    val allStoresUsed = state.availableStores.isEmpty() && state.prices.isNotEmpty()
    when {
        noStores -> SectionMessage(t("products.noStoresAvailable", language), appColors)
        state.prices.isEmpty() -> SectionMessage(t("products.noPrices", language), appColors)
        allStoresUsed -> SectionMessage(t("products.allStoresUsed", language), appColors)
    }

    AppBottomSheet(
        visible = showAddPrice,
        onDismiss = { showAddPrice = false },
        title = t("products.addPrice", language),
    ) {
        AddPriceForm(
            availableStores = state.availableStores,
            selectedStoreId = newStoreId,
            onSelectStore = { newStoreId = it },
            priceText = newPriceText,
            onPriceTextChange = { if (it.matches(PRICE_REGEX)) newPriceText = it },
            onConfirm = {
                newStoreId?.let { vm.addPrice(it, newPriceText) }
                showAddPrice = false
            },
            language = language,
        )
    }

    val editingRow = editingStoreId?.let { id -> state.prices.firstOrNull { it.storeId == id } }
    if (editingRow != null) {
        AppBottomSheet(
            visible = true,
            onDismiss = { editingStoreId = null },
            title = editingRow.storeName,
        ) {
            PriceEditForm(
                storeName = editingRow.storeName,
                priceText = newPriceText,
                onPriceTextChange = { if (it.matches(PRICE_REGEX)) newPriceText = it },
                onConfirm = {
                    vm.updatePrice(editingRow.storeId, newPriceText)
                    editingStoreId = null
                },
                language = language,
            )
        }
    }

    val deletingRow = pendingDeleteStoreId?.let { id -> state.prices.firstOrNull { it.storeId == id } }
    if (deletingRow != null) {
        ConfirmSheet(
            visible = true,
            title = t("products.removePriceTitle", language),
            message = deletingRow.storeName,
            confirmLabel = t("products.delete", language),
            onConfirm = {
                vm.removePrice(deletingRow.storeId)
                pendingDeleteStoreId = null
            },
            onDismiss = { pendingDeleteStoreId = null },
        )
    }
}

@Composable
private fun SectionMessage(text: String, appColors: AppColors) {
    Text(
        text,
        color = appColors.textSecondary,
        fontSize = 13.sp,
        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
    )
}

private val PRICE_REGEX = Regex("^\\d*\\.?\\d*$")

@Composable
private fun PriceCardContent(
    storeName: String,
    value: String,
    onRemove: () -> Unit,
) {
    val appColors = LocalAppColors.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            storeName,
            color = appColors.text,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        Text(
            "$${trimPrice(value)}",
            color = appColors.text,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(Modifier.width(8.dp))
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(20.dp))
                .clickable(onClick = onRemove),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Default.Close, contentDescription = "remove price", tint = appColors.textSecondary, modifier = Modifier.size(24.dp))
        }
    }
}

@Composable
private fun PriceEditForm(
    storeName: String,
    priceText: String,
    onPriceTextChange: (String) -> Unit,
    onConfirm: () -> Unit,
    language: com.easypocket.mobile.i18n.Language,
) {
    Column(Modifier.fillMaxWidth()) {
        DecimalTextField(
            value = priceText,
            onValueChange = onPriceTextChange,
            placeholder = storeName,
            autoFocus = true,
        )
        Spacer(Modifier.height(16.dp))
        AppButton(
            text = t("products.addModal.save", language),
            onClick = onConfirm,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun AddPriceForm(
    availableStores: List<Store>,
    selectedStoreId: String?,
    onSelectStore: (String) -> Unit,
    priceText: String,
    onPriceTextChange: (String) -> Unit,
    onConfirm: () -> Unit,
    language: com.easypocket.mobile.i18n.Language,
) {
    Column(Modifier.fillMaxWidth()) {
        val options = availableStores.map { SelectOption(it.id, it.description) }
        SelectField(
            options = options,
            onSelect = onSelectStore,
            selectedId = selectedStoreId,
            placeholder = t("products.addModal.storePlaceholder", language),
        )
        Spacer(Modifier.height(8.dp))
        DecimalTextField(
            value = priceText,
            onValueChange = onPriceTextChange,
            placeholder = t("products.addModal.pricePlaceholder", language),
        )
        Spacer(Modifier.height(16.dp))
        AppButton(
            text = t("products.addModal.save", language),
            onClick = onConfirm,
            enabled = selectedStoreId != null && priceText.toDoubleOrNull() != null,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun DecimalTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    autoFocus: Boolean = false,
) {
    FormTextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = placeholder,
        keyboardType = KeyboardType.Decimal,
        selectAllOnFocus = true,
        autoFocus = autoFocus,
    )
}

private fun trimPrice(value: String): String {
    val parsed = value.toDoubleOrNull() ?: return value
    return String.format(java.util.Locale.US, "%.2f", parsed)
}
