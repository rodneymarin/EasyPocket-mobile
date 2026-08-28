package com.easypocket.mobile.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import com.easypocket.mobile.settings.ThemeMode

val LocalAppColors = staticCompositionLocalOf { LightColors }

val LocalIsDark = staticCompositionLocalOf { false }

@Composable
fun EasyPocketTheme(themeMode: ThemeMode, content: @Composable () -> Unit) {
    val systemDark = isSystemInDarkTheme()
    val isDark = when (themeMode) { ThemeMode.LIGHT -> false; ThemeMode.DARK -> true; ThemeMode.SYSTEM -> systemDark }
    val appColors = if (isDark) DarkColors else LightColors
    val scheme = if (isDark) darkColorScheme(
        primary = appColors.primary, background = appColors.background,
        surface = appColors.surface, onSurface = appColors.surfaceText,
        error = appColors.destructiveBorder,
    ) else lightColorScheme(
        primary = appColors.primary, background = appColors.background,
        surface = appColors.surface, onSurface = appColors.surfaceText,
        error = appColors.destructiveBorder,
    )
    CompositionLocalProvider(LocalAppColors provides appColors, LocalIsDark provides isDark) {
        MaterialTheme(colorScheme = scheme, content = content)
    }
}
