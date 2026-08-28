package com.easypocket.mobile.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.rememberNavController
import com.easypocket.mobile.AppViewModel
import com.easypocket.mobile.i18n.LocalLanguage
import com.easypocket.mobile.i18n.t
import com.easypocket.mobile.ui.components.ConfirmSheet
import com.easypocket.mobile.ui.components.LocalToastState
import com.easypocket.mobile.ui.components.ToastHost
import com.easypocket.mobile.ui.components.ToastState
import com.easypocket.mobile.ui.components.ToastType
import com.easypocket.mobile.ui.mainmenu.AboutSheet
import com.easypocket.mobile.ui.mainmenu.MainMenu
import com.easypocket.mobile.ui.theme.LocalAppColors

@Composable
fun AppRoot(vm: AppViewModel, mainViewModelProvider: @Composable () -> Unit = {}) {
    val navController = rememberNavController()
    val toastState = remember { ToastState() }
    val language by vm.language.collectAsStateWithLifecycle()
    val themeMode by vm.themeMode.collectAsStateWithLifecycle()
    val fatalError by vm.fatalError.collectAsStateWithLifecycle()
    val isReady by vm.isReady.collectAsStateWithLifecycle()
    val menuVisible by vm.menuVisible.collectAsStateWithLifecycle()

    var showResetSheet by rememberSaveable { mutableStateOf(false) }
    var showAboutSheet by rememberSaveable { mutableStateOf(false) }

    CompositionLocalProvider(LocalToastState provides toastState, LocalLanguage provides language) {
        when {
            fatalError != null -> ErrorScreen(onRetry = { vm.init() })
            !isReady -> Box(Modifier.fillMaxSize().background(LocalAppColors.current.background))
            else -> Box(Modifier.fillMaxSize()) {
                mainViewModelProvider()
                AppNavHost(navController = navController, vm = vm)
                MainMenu(
                    visible = menuVisible,
                    language = language,
                    themeMode = themeMode,
                    onDismiss = { vm.closeMenu() },
                    onThemeSelected = { vm.setThemeMode(it) },
                    onLanguageSelected = { vm.setLanguage(it) },
                    onExport = { toastState.show(t("menu.exportData", language), ToastType.INFO) },
                    onImport = { toastState.show(t("menu.importData", language), ToastType.INFO) },
                    onReset = { showResetSheet = true },
                    onAbout = { showAboutSheet = true; vm.closeMenu() },
                )
                AboutSheet(visible = showAboutSheet, onDismiss = { showAboutSheet = false })
                ConfirmSheet(
                    visible = showResetSheet,
                    title = t("menu.resetToDefaultsTitle", language),
                    message = t("menu.resetToDefaultsMessage", language),
                    warning = t("menu.resetToDefaultsWarning", language),
                    confirmLabel = t("menu.reset", language),
                    onConfirm = {
                        showResetSheet = false
                        vm.resetToSeed()
                        toastState.show(t("toast.dataReset", language), ToastType.SUCCESS)
                        vm.closeMenu()
                    },
                    onDismiss = { showResetSheet = false },
                )
                ToastHost(toastState)
            }
        }
    }
}
