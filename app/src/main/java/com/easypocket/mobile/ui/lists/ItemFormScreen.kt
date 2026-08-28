package com.easypocket.mobile.ui.lists

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
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.easypocket.mobile.i18n.LocalLanguage
import com.easypocket.mobile.i18n.t
import com.easypocket.mobile.ui.components.AppBottomSheet
import com.easypocket.mobile.ui.components.AppButton
import com.easypocket.mobile.ui.components.AppHeader
import com.easypocket.mobile.ui.components.ButtonVariant
import com.easypocket.mobile.ui.components.ConfirmSheet
import com.easypocket.mobile.ui.components.FormTextField
import com.easypocket.mobile.ui.components.IconButtonCircle
import com.easypocket.mobile.ui.components.LocalToastState
import com.easypocket.mobile.ui.components.SelectField
import com.easypocket.mobile.ui.components.SelectOption
import com.easypocket.mobile.ui.components.ToastType
import com.easypocket.mobile.ui.products.ProductFormContent
import com.easypocket.mobile.ui.products.ProductFormViewModel
import com.easypocket.mobile.ui.products.ProductPickerSheet
import com.easypocket.mobile.ui.theme.LocalAppColors
import java.util.Locale
import kotlinx.coroutines.launch

@Composable
fun ItemFormScreen(navController: NavController, listId: String, itemId: Long) {
    val vm: ItemFormViewModel = hiltViewModel()
    val uiState by vm.uiState.collectAsStateWithLifecycle()
    val language = LocalLanguage.current
    val appColors = LocalAppColors.current
    val toast = LocalToastState.current
    val scope = rememberCoroutineScope()

    var showPicker by remember { mutableStateOf(false) }
    var showCreateProduct by remember { mutableStateOf(false) }
    var showEditProduct by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    val createProductVm: ProductFormViewModel = hiltViewModel(key = "itemFormCreateProduct")
    val editProductVm: ProductFormViewModel = hiltViewModel(key = "itemFormEditProduct")

    LaunchedEffect(listId, itemId) { vm.load(listId, itemId) }

    fun goBack() {
        navController.popBackStack()
    }

    val selectedProduct = uiState.products.firstOrNull { it.id == uiState.productId }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(appColors.background)
            .padding(top = 60.dp)
            .imePadding(),
    ) {
        AppHeader(
            title = if (uiState.isEdit) t("listItem.editTitle", language) else t("listItem.addTitle", language),
            onBack = ::goBack,
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
        ) {
            FieldLabel(t("listItem.productLabel", language))
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(45.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(appColors.inputBackground)
                        .clickable { showPicker = true }
                        .padding(horizontal = 16.dp),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            selectedProduct?.productName ?: t("listItem.productPlaceholder", language),
                            color = if (selectedProduct != null) appColors.text else appColors.placeholderText,
                            fontSize = 14.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f),
                        )
                        Icon(
                            Icons.Default.KeyboardArrowDown,
                            contentDescription = null,
                            tint = appColors.text,
                            modifier = Modifier.size(14.dp),
                        )
                    }
                }
                Spacer(Modifier.width(8.dp))
                IconButtonCircle(
                    icon = Icons.Default.Edit,
                    onClick = { if (selectedProduct != null) showEditProduct = true },
                    enabled = selectedProduct != null,
                )
                Spacer(Modifier.width(8.dp))
                IconButtonCircle(
                    icon = Icons.Default.Add,
                    onClick = { showCreateProduct = true },
                )
            }

            Spacer(Modifier.height(16.dp))
            FieldLabel(t("listItem.storeLabel", language))
            Spacer(Modifier.height(8.dp))
            val storeOptions = listOf(SelectOption("", t("listItem.storeNone", language))) + uiState.stores.map { store ->
                val price = selectedProduct?.prices?.firstOrNull { it.storeId == store.id }?.value
                SelectOption(store.id, store.description, trailing = price?.let { "$" + formatAmount(it) })
            }
            SelectField(
                options = storeOptions,
                onSelect = { id -> vm.setStore(if (id.isEmpty()) null else id) },
                selectedId = uiState.storeId ?: "",
                placeholder = t("listItem.storePlaceholder", language),
            )

            Spacer(Modifier.height(16.dp))
            FieldLabel(t("listItem.quantityLabel", language))
            Spacer(Modifier.height(8.dp))
            FormTextField(
                value = uiState.quantityText,
                onValueChange = vm::setQuantity,
                placeholder = "0",
                keyboardType = KeyboardType.Decimal,
                fontSize = 15,
                trailing = uiState.unitLabelKey?.let { key ->
                    t(key, language).let { label -> if (label != key) label else key.removePrefix("unit.") }
                },
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(20.dp))
            PriceSummary(
                unitPrice = uiState.unitPrice,
                totalPrice = uiState.totalPrice,
                language = language,
            )
            Spacer(Modifier.height(16.dp))
        }

        Column(
            Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 16.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                if (uiState.isEdit) {
                    AppButton(
                        text = t("listItem.delete", language),
                        onClick = { showDeleteConfirm = true },
                        variant = ButtonVariant.DESTRUCTIVE,
                        modifier = Modifier.weight(1f),
                    )
                }
                AppButton(
                    text = t("listItem.cancel", language),
                    onClick = ::goBack,
                    variant = ButtonVariant.SECONDARY,
                    modifier = Modifier.weight(1f),
                )
            }
            Spacer(Modifier.height(8.dp))
            AppButton(
                text = t("listItem.save", language),
                onClick = {
                    scope.launch {
                        vm.save {
                            goBack()
                            toast.show(
                                t(if (uiState.isEdit) "toast.itemUpdated" else "toast.itemAdded", language),
                                ToastType.SUCCESS,
                            )
                        }
                    }
                },
                enabled = uiState.isQuantityValid && uiState.productId != null,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }

    ProductPickerSheet(
        visible = showPicker,
        products = uiState.products,
        searchPlaceholder = t("search.products", language),
        noResultsMessage = t("common.noResults", language),
        selectedProductId = uiState.productId,
        onSelect = { id ->
            vm.selectProduct(id)
            showPicker = false
        },
        onDismiss = { showPicker = false },
    )

    AppBottomSheet(
        visible = showCreateProduct,
        onDismiss = { showCreateProduct = false },
        heightFraction = 0.8f,
        title = t("products.addTitle", language),
    ) {
        LaunchedEffect(showCreateProduct) { if (showCreateProduct) createProductVm.load(null) }
        ProductFormContent(
            vm = createProductVm,
            isSheet = true,
            onSaved = {
                vm.registerProduct(createProductVm.toProduct())
                showCreateProduct = false
                toast.show(t("toast.productCreated", language), ToastType.SUCCESS)
            },
            onDeleted = {},
            onCancel = { showCreateProduct = false },
        )
    }

    AppBottomSheet(
        visible = showEditProduct,
        onDismiss = { showEditProduct = false },
        heightFraction = 0.8f,
        title = t("products.editTitle", language),
    ) {
        LaunchedEffect(showEditProduct) { if (showEditProduct) editProductVm.load(uiState.productId) }
        ProductFormContent(
            vm = editProductVm,
            isSheet = true,
            onSaved = {
                vm.registerProduct(editProductVm.toProduct())
                showEditProduct = false
                toast.show(t("toast.productUpdated", language), ToastType.SUCCESS)
            },
            onDeleted = {
                showEditProduct = false
                toast.show(t("toast.productDeleted", language), ToastType.SUCCESS)
                vm.clearForm()
            },
            onCancel = { showEditProduct = false },
        )
    }

    ConfirmSheet(
        visible = showDeleteConfirm,
        title = t("listItem.deleteModal.title", language),
        message = t("listItem.deleteModal.confirmMessage", language),
        confirmLabel = t("listItem.deleteModal.confirm", language),
        onConfirm = {
            showDeleteConfirm = false
            scope.launch {
                vm.delete {
                    goBack()
                    toast.show(t("toast.itemDeleted", language), ToastType.SUCCESS)
                }
            }
        },
        onDismiss = { showDeleteConfirm = false },
    )
}

@Composable
private fun FieldLabel(text: String) {
    val appColors = LocalAppColors.current
    Text(text, color = appColors.text, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
}

@Composable
private fun PriceSummary(unitPrice: Double?, totalPrice: Double?, language: com.easypocket.mobile.i18n.Language) {
    val appColors = LocalAppColors.current
    val unitText = unitPrice?.let { "$" + formatAmount(it) } ?: "—"
    val totalText = totalPrice?.let { "$" + formatAmount(it) } ?: "—"
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                t("listItem.unitPrice", language) + ":",
                color = appColors.text,
                fontSize = 15.sp,
            )
            Spacer(Modifier.width(6.dp))
            Text(unitText, color = appColors.text, fontSize = 15.sp)
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                t("listItem.total", language) + ":",
                color = appColors.text,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.width(6.dp))
            Text(totalText, color = appColors.text, fontSize = 17.sp, fontWeight = FontWeight.Bold)
        }
    }
}

private fun formatAmount(value: Double): String = String.format(Locale.US, "%.2f", value)
