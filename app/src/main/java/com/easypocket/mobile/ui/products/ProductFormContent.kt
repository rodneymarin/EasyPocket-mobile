package com.easypocket.mobile.ui.products

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
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
import com.easypocket.mobile.ui.components.ButtonVariant
import com.easypocket.mobile.ui.components.IconButtonCircle
import com.easypocket.mobile.ui.components.SelectField
import com.easypocket.mobile.ui.components.SelectOption
import com.easypocket.mobile.ui.theme.AppColors
import com.easypocket.mobile.ui.theme.LocalAppColors
import kotlinx.coroutines.launch

@Composable
fun ProductFormContent(
    vm: ProductFormViewModel,
    isSheet: Boolean = false,
    onSaved: () -> Unit,
    onDeleted: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by vm.uiState.collectAsStateWithLifecycle()
    val language = LocalLanguage.current
    val appColors = LocalAppColors.current
    val scope = rememberCoroutineScope()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .imePadding(),
    ) {
        ProductNameInput(
            value = state.name,
            onValueChange = vm::setName,
            placeholder = t("products.addModal.namePlaceholder", language),
            isError = state.nameError,
            errorMessage = t("toast.productNameExists", language),
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
        )

        Spacer(Modifier.height(20.dp))
        PricesSection(state = state, vm = vm, language = language, appColors = appColors)

        Spacer(Modifier.height(24.dp))
        Column(Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (state.isEdit) {
                    AppButton(
                        text = t("products.delete", language),
                        onClick = { scope.launch { vm.delete { onDeleted() } } },
                        variant = ButtonVariant.DESTRUCTIVE,
                        modifier = Modifier.weight(1f),
                    )
                }
                AppButton(
                    text = t("products.addModal.cancel", language),
                    onClick = onCancel,
                    variant = ButtonVariant.SECONDARY,
                    modifier = Modifier.weight(1f),
                )
            }
            Spacer(Modifier.height(8.dp))
            AppButton(
                text = t("products.addModal.save", language),
                onClick = { scope.launch { vm.save { onSaved() } } },
                modifier = Modifier.fillMaxWidth(),
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
) {
    val appColors = LocalAppColors.current
    Column(Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(appColors.background)
                .border(1.dp, if (isError) appColors.destructiveBorder else appColors.border, RoundedCornerShape(8.dp))
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.fillMaxWidth()) {
                if (value.isEmpty()) {
                    Text(placeholder, color = appColors.placeholderText, fontSize = 14.sp)
                }
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    singleLine = true,
                    textStyle = TextStyle(color = appColors.text, fontSize = 14.sp),
                    cursorBrush = SolidColor(appColors.primary),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                )
            }
        }
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
    Text(
        t("products.pricesSection", language).uppercase(),
        color = appColors.textSecondary,
        fontSize = 13.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 1.sp,
    )
    Spacer(Modifier.height(8.dp))

    var editingStoreId by remember { mutableStateOf<String?>(null) }
    var isAddingPrice by remember { mutableStateOf(false) }
    var newStoreId by remember { mutableStateOf<String?>(null) }
    var newPriceText by remember { mutableStateOf("") }

    fun resetForm() {
        editingStoreId = null
        isAddingPrice = false
        newStoreId = null
        newPriceText = ""
    }

    state.prices.forEach { row ->
        if (editingStoreId == row.storeId) {
            PriceEditForm(
                storeName = row.storeName,
                priceText = newPriceText,
                onPriceTextChange = { if (it.matches(PRICE_REGEX)) newPriceText = it },
                onConfirm = {
                    vm.updatePrice(row.storeId, newPriceText)
                    resetForm()
                },
                onCancel = ::resetForm,
            )
            Spacer(Modifier.height(8.dp))
        } else {
            PriceRow(
                storeName = row.storeName,
                value = row.value,
                onEdit = {
                    editingStoreId = row.storeId
                    newStoreId = row.storeId
                    newPriceText = row.value
                    isAddingPrice = false
                },
                onRemove = { vm.removePrice(row.storeId) },
            )
            Spacer(Modifier.height(8.dp))
        }
    }

    if (isAddingPrice && editingStoreId == null) {
        AddPriceForm(
            availableStores = state.availableStores,
            selectedStoreId = newStoreId,
            onSelectStore = { newStoreId = it },
            priceText = newPriceText,
            onPriceTextChange = { if (it.matches(PRICE_REGEX)) newPriceText = it },
            onConfirm = {
                newStoreId?.let { vm.addPrice(it, newPriceText) }
                if (newStoreId != null) resetForm()
            },
            onCancel = ::resetForm,
            language = language,
        )
        Spacer(Modifier.height(8.dp))
    }

    val allStoresUsed = state.availableStores.isEmpty() && state.prices.isNotEmpty()
    val noStores = state.availableStores.isEmpty() && state.prices.isEmpty()
    if (editingStoreId == null) {
        when {
            noStores -> Text(
                t("products.noStoresAvailable", language),
                color = appColors.textSecondary,
                fontSize = 13.sp,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            )
            allStoresUsed -> Text(
                t("products.allStoresUsed", language),
                color = appColors.textSecondary,
                fontSize = 13.sp,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            )
            !isAddingPrice -> AppButton(
                text = t("products.addPrice", language),
                onClick = {
                    editingStoreId = null
                    newStoreId = null
                    newPriceText = ""
                    isAddingPrice = true
                },
                variant = ButtonVariant.SECONDARY,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            )
        }
    }
}

private val PRICE_REGEX = Regex("^\\d*\\.?\\d*$")

@Composable
private fun PriceRow(
    storeName: String,
    value: String,
    onEdit: () -> Unit,
    onRemove: () -> Unit,
) {
    val appColors = LocalAppColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(appColors.surface)
            .border(1.dp, appColors.border, RoundedCornerShape(10.dp))
            .clickable(onClick = onEdit)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            storeName,
            color = appColors.text,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
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
                .size(28.dp)
                .clip(RoundedCornerShape(14.dp))
                .clickable(onClick = onRemove),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Default.Close, contentDescription = "remove price", tint = appColors.textSecondary, modifier = Modifier.size(16.dp))
        }
    }
}

@Composable
private fun PriceEditForm(
    storeName: String,
    priceText: String,
    onPriceTextChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
) {
    val appColors = LocalAppColors.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(appColors.surface)
            .border(1.dp, appColors.border, RoundedCornerShape(10.dp))
            .padding(12.dp),
    ) {
        Text(storeName, color = appColors.text, fontSize = 15.sp, fontWeight = FontWeight.Medium)
        Spacer(Modifier.height(8.dp))
        DecimalTextField(
            value = priceText,
            onValueChange = onPriceTextChange,
            placeholder = "",
        )
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            IconButtonCircle(
                icon = Icons.Default.Check,
                onClick = onConfirm,
                variant = ButtonVariant.PRIMARY,
                modifier = Modifier.weight(1f),
            )
            IconButtonCircle(
                icon = Icons.Default.Close,
                onClick = onCancel,
                variant = ButtonVariant.SECONDARY,
                modifier = Modifier.weight(1f),
            )
        }
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
    onCancel: () -> Unit,
    language: com.easypocket.mobile.i18n.Language,
) {
    val appColors = LocalAppColors.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(appColors.surface)
            .border(1.dp, appColors.border, RoundedCornerShape(10.dp))
            .padding(12.dp),
    ) {
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
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            IconButtonCircle(
                icon = Icons.Default.Check,
                onClick = onConfirm,
                variant = ButtonVariant.PRIMARY,
                modifier = Modifier.weight(1f),
            )
            IconButtonCircle(
                icon = Icons.Default.Close,
                onClick = onCancel,
                variant = ButtonVariant.SECONDARY,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun DecimalTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
) {
    val appColors = LocalAppColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(appColors.background)
            .border(1.dp, appColors.border, RoundedCornerShape(10.dp))
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.fillMaxWidth()) {
            if (value.isEmpty()) {
                Text(placeholder, color = appColors.placeholderText, fontSize = 14.sp)
            }
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                textStyle = TextStyle(color = appColors.text, fontSize = 14.sp),
                cursorBrush = SolidColor(appColors.primary),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            )
        }
    }
}

private fun trimPrice(value: String): String {
    val parsed = value.toDoubleOrNull() ?: return value
    return String.format(java.util.Locale.US, "%.2f", parsed)
}
