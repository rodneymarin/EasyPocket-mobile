package com.easypocket.mobile.ui.theme

import androidx.compose.ui.graphics.Color

data class AppColors(
    val background: Color, val cardBackground: Color, val text: Color,
    val textSecondary: Color, val border: Color, val surface: Color,
    val surfaceText: Color, val primary: Color, val destructive: Color,
    val destructiveBorder: Color, val tabBarInactive: Color,
    val panelBackground: Color, val panelText: Color, val panelBorder: Color,
    val placeholderText: Color,
)

val LightColors = AppColors(
    background = Color(0xFFFFFFFF), cardBackground = Color(0xFFFFFFFF),
    text = Color(0xFF000000), textSecondary = Color(0xFF666666),
    border = Color(0xFFE0E0E0), surface = Color(0xFFF0F0F0),
    surfaceText = Color(0xFF333333), primary = Color(0xFF4A5DF9),
    destructive = Color(0xFFFFE0E0), destructiveBorder = Color(0xFFE05555),
    tabBarInactive = Color(0xFF8E8E93), panelBackground = Color(0xFFFFFFFF),
    panelText = Color(0xFF333333), panelBorder = Color(0xFFE0E0E0),
    placeholderText = Color(0xFF999999),
)

val DarkColors = AppColors(
    background = Color(0xFF121212), cardBackground = Color(0xFF1C1C1C),
    text = Color(0xFFF5F5F5), textSecondary = Color(0xFFAAAAAA),
    border = Color(0xFF333333), surface = Color(0xFF2C2C2C),
    surfaceText = Color(0xFFCCCCCC), primary = Color(0xFF4A5DF9),
    destructive = Color(0xFF4A1C1C), destructiveBorder = Color(0xFFD95050),
    tabBarInactive = Color(0xFF636366), panelBackground = Color(0xFF1C1C1C),
    panelText = Color(0xFFF5F5F5), panelBorder = Color(0xFF38383A),
    placeholderText = Color(0xFF666666),
)
