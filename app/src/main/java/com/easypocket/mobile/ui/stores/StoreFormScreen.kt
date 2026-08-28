package com.easypocket.mobile.ui.stores

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.easypocket.mobile.i18n.LocalLanguage
import com.easypocket.mobile.i18n.t
import com.easypocket.mobile.ui.components.AppButton
import com.easypocket.mobile.ui.components.ButtonVariant
import com.easypocket.mobile.ui.components.ConfirmSheet
import com.easypocket.mobile.ui.components.LocalToastState
import com.easypocket.mobile.ui.components.ToastType
import com.easypocket.mobile.ui.theme.LocalAppColors
import com.easypocket.mobile.ui.theme.LocalIsDark
import com.easypocket.mobile.ui.theme.StoreColors
import kotlinx.coroutines.launch

@Composable
fun StoreFormScreen(navController: NavController, storeId: String) {
    val vm: StoreFormViewModel = hiltViewModel()
    val language = LocalLanguage.current
    val toast = LocalToastState.current

    LaunchedEffect(storeId) { vm.load(if (storeId == "new") null else storeId) }

    fun goBack() {
        navController.popBackStack()
    }

    StoreFormContent(
        vm = vm,
        onSaved = {
            val wasEdit = vm.uiState.value.isEdit
            goBack()
            toast.show(
                t(if (wasEdit) "toast.storeUpdated" else "toast.storeCreated", language),
                ToastType.SUCCESS,
            )
        },
        onDeleted = {
            goBack()
            toast.show(t("toast.storeDeleted", language), ToastType.SUCCESS)
        },
        onCancel = ::goBack,
    )
}

@Composable
fun StoreFormContent(
    vm: StoreFormViewModel,
    onSaved: () -> Unit,
    onDeleted: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by vm.uiState.collectAsStateWithLifecycle()
    val language = LocalLanguage.current
    val appColors = LocalAppColors.current
    val scope = rememberCoroutineScope()

    var showDeleteSheet by rememberSaveable { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(appColors.background)
            .imePadding(),
    ) {
        StoreFormHeader(
            title = t(if (state.isEdit) "stores.editTitle" else "stores.addTitle", language),
            onBack = onCancel,
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
        ) {
            StoreNameInput(
                value = state.name,
                onValueChange = vm::setName,
                placeholder = t("stores.addModal.placeholder", language),
            )
            Spacer(Modifier.height(20.dp))
            FieldLabel(text = t("stores.colorLabel", language))
            Spacer(Modifier.height(10.dp))
            ColorSwatches(selected = state.color, onSelect = vm::setColor)
            Spacer(Modifier.height(24.dp))
        }
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (state.isEdit) {
                    AppButton(
                        text = t("stores.delete", language),
                        onClick = { showDeleteSheet = true },
                        variant = ButtonVariant.DESTRUCTIVE,
                        modifier = Modifier.weight(1f),
                    )
                }
                AppButton(
                    text = t("stores.addModal.cancel", language),
                    onClick = onCancel,
                    variant = ButtonVariant.SECONDARY,
                    modifier = Modifier.weight(1f),
                )
            }
            Spacer(Modifier.height(8.dp))
            AppButton(
                text = t("stores.addModal.save", language),
                onClick = { scope.launch { vm.save { onSaved() } } },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }

    ConfirmSheet(
        visible = showDeleteSheet,
        title = t("stores.deleteModal.title", language),
        message = t("stores.deleteModal.confirmMessage", language, mapOf("store" to state.name)),
        warning = t("stores.deleteModal.warning", language),
        confirmLabel = t("stores.deleteModal.confirm", language),
        onConfirm = {
            scope.launch {
                vm.delete { onDeleted() }
                showDeleteSheet = false
            }
        },
        onDismiss = { showDeleteSheet = false },
    )
}

@Composable
private fun StoreFormHeader(title: String, onBack: () -> Unit) {
    val appColors = LocalAppColors.current
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 60.dp, bottom = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            title,
            color = appColors.text,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 52.dp),
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 12.dp)
                .size(36.dp)
                .clip(CircleShape)
                .background(appColors.surface)
                .clickable(onClick = onBack),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "back",
                tint = appColors.text,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

@Composable
private fun StoreNameInput(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
) {
    val appColors = LocalAppColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(appColors.background)
            .border(1.dp, appColors.border, RoundedCornerShape(8.dp))
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
}

@Composable
private fun FieldLabel(text: String) {
    val appColors = LocalAppColors.current
    Text(
        text.uppercase(),
        color = appColors.text,
        fontSize = 13.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 1.sp,
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ColorSwatches(selected: Int, onSelect: (Int) -> Unit) {
    val isDark = LocalIsDark.current
    val appColors = LocalAppColors.current
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        for (index in 0..StoreColors.light.lastIndex) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(StoreColors.get(index, isDark))
                    .then(
                        if (selected == index) {
                            Modifier.border(3.dp, appColors.primary, CircleShape)
                        } else {
                            Modifier
                        },
                    )
                    .clickable(onClick = { onSelect(index) }),
            )
        }
    }
}
