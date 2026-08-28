package com.easypocket.mobile.ui.products

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.easypocket.mobile.i18n.LocalLanguage
import com.easypocket.mobile.i18n.t
import com.easypocket.mobile.ui.components.LocalToastState
import com.easypocket.mobile.ui.components.ToastType

@Composable
fun ProductFormScreen(navController: NavController, productId: String) {
    val vm: ProductFormViewModel = hiltViewModel()
    val language = LocalLanguage.current
    val toast = LocalToastState.current

    LaunchedEffect(productId) { vm.load(if (productId == "new") null else productId) }

    fun goBack() {
        navController.popBackStack()
    }

    ProductFormContent(
        vm = vm,
        isSheet = false,
        onSaved = {
            val wasEdit = vm.uiState.value.isEdit
            goBack()
            toast.show(
                t(if (wasEdit) "toast.productUpdated" else "toast.productCreated", language),
                ToastType.SUCCESS,
            )
        },
        onDeleted = {
            goBack()
            toast.show(t("toast.productDeleted", language), ToastType.SUCCESS)
        },
        onCancel = ::goBack,
    )
}
