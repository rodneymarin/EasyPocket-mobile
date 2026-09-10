package com.easypocket.mobile.ui.categories

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.easypocket.mobile.i18n.LocalLanguage
import com.easypocket.mobile.i18n.t
import com.easypocket.mobile.ui.components.AppButton
import com.easypocket.mobile.ui.components.AppHeader
import com.easypocket.mobile.ui.components.ButtonVariant
import com.easypocket.mobile.ui.components.ConfirmSheet
import com.easypocket.mobile.ui.components.KEY_NEWLY_ADDED_ID
import com.easypocket.mobile.ui.components.FormTextField
import com.easypocket.mobile.ui.components.ListIconField
import com.easypocket.mobile.ui.components.LocalToastState
import com.easypocket.mobile.ui.components.ToastType
import com.easypocket.mobile.ui.components.notifyDataRestored
import com.easypocket.mobile.ui.theme.LocalAppColors
import com.easypocket.mobile.data.repository.DeletedCategories
import kotlinx.coroutines.launch

@Composable
fun CategoryFormScreen(navController: NavController, categoryId: String) {
    val vm: CategoryFormViewModel = hiltViewModel()
    val language = LocalLanguage.current
    val toast = LocalToastState.current

    LaunchedEffect(categoryId) { vm.load(if (categoryId == "new") null else categoryId) }

    fun goBack() {
        navController.popBackStack()
    }

    CategoryFormContent(
        vm = vm,
        autoFocusName = categoryId == "new",
        onSaved = {
            val wasEdit = vm.uiState.value.isEdit
            if (!wasEdit) {
                navController.previousBackStackEntry
                    ?.savedStateHandle?.set(KEY_NEWLY_ADDED_ID, vm.uiState.value.category?.id)
            }
            goBack()
            toast.show(
                t(if (wasEdit) "toast.categoryUpdated" else "toast.categoryCreated", language),
                ToastType.SUCCESS,
            )
        },
        onDeleted = { deleted ->
            goBack()
            if (deleted != null) {
                toast.show(
                    t("toast.categoryDeleted", language),
                    ToastType.DESTRUCTIVE,
                    actionLabel = t("common.undo", language),
                    onAction = {
                        vm.restore(deleted)
                        navController.notifyDataRestored()
                    },
                )
            }
        },
        onCancel = ::goBack,
    )
}

@Composable
fun CategoryFormContent(
    vm: CategoryFormViewModel,
    autoFocusName: Boolean = false,
    onSaved: () -> Unit,
    onDeleted: (DeletedCategories?) -> Unit,
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
            .padding(top = 60.dp),
    ) {
        AppHeader(
            title = t(if (state.isEdit) "categories.editTitle" else "categories.addTitle", language),
            onBack = onCancel,
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
        ) {
            FormTextField(
                value = state.name,
                onValueChange = vm::setName,
                placeholder = t("categories.addModal.placeholder", language),
                autoFocus = autoFocusName,
            )
            Spacer(Modifier.height(16.dp))
            ListIconField(
                value = state.icon,
                onValueChange = vm::setIcon,
                placeholder = t("categories.icon", language),
            )
            Spacer(Modifier.height(24.dp))
        }
        Column(
            Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (state.isEdit) {
                    AppButton(
                        text = t("categories.delete", language),
                        onClick = { showDeleteSheet = true },
                        variant = ButtonVariant.DESTRUCTIVE,
                        modifier = Modifier.weight(1f),
                    )
                }
                AppButton(
                    text = t("categories.addModal.cancel", language),
                    onClick = onCancel,
                    variant = ButtonVariant.SECONDARY,
                    modifier = Modifier.weight(1f),
                )
                AppButton(
                    text = t("categories.addModal.save", language),
                    onClick = { scope.launch { vm.save { onSaved() } } },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }

    ConfirmSheet(
        visible = showDeleteSheet,
        title = t("categories.deleteModal.title", language),
        message = t("categories.deleteModal.confirmMessage", language, mapOf("category" to state.name)),
        warning = t("categories.deleteModal.warning", language),
        confirmLabel = t("categories.deleteModal.confirm", language),
        onConfirm = {
            scope.launch {
                val deleted = vm.delete()
                showDeleteSheet = false
                onDeleted(deleted)
            }
        },
        onDismiss = { showDeleteSheet = false },
    )
}
