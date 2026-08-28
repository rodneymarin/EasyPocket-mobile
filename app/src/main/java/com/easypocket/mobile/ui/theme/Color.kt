package com.easypocket.mobile.ui.theme

import androidx.compose.ui.graphics.Color

data class AppColors(
    val background: Color, val cardBackground: Color, val text: Color,
    val textSecondary: Color, val border: Color, val surface: Color,
    val surfaceText: Color, val primary: Color, val destructive: Color,
    val destructiveBorder: Color, val tabBarInactive: Color,
    val panelBackground: Color, val panelText: Color, val panelBorder: Color,
    val placeholderText: Color, val inputBackground: Color, val secondaryButton: Color,
)

val LightColors = AppColors(
    background = Color(0xFFF2F2F6), cardBackground = Color(0xFFFFFFFF),
    text = Color(0xFF000000), textSecondary = Color(0xFF666666),
    border = Color(0xFFE0E0E0), surface = Color(0xFFE9E9EE),
    surfaceText = Color(0xFF333333), primary = Color(0xFF4A5DF9),
    destructive = Color(0xFFFFCFCF), destructiveBorder = Color(0xFFE05555),
    tabBarInactive = Color(0xFF8E8E93), panelBackground = Color(0xFFFFFFFF),
    panelText = Color(0xFF333333), panelBorder = Color(0xFFE0E0E0),
    placeholderText = Color(0xFF999999), inputBackground = Color(0xFFFFFFFF),
    secondaryButton = Color(0xFFD8D8DE),
)

val DarkColors = AppColors(
    background = Color(0xFF0C0C0E), cardBackground = Color(0xFF1A1A1D),
    text = Color(0xFFF5F5F5), textSecondary = Color(0xFFAAAAAA),
    border = Color(0xFF333333), surface = Color(0xFF1F1F23),
    surfaceText = Color(0xFFCCCCCC), primary = Color(0xFF4A5DF9),
    destructive = Color(0xFF4A1C1C), destructiveBorder = Color(0xFFD95050),
    tabBarInactive = Color(0xFF636366), panelBackground = Color(0xFF1C1C1C),
    panelText = Color(0xFFF5F5F5), panelBorder = Color(0xFF38383A),
    placeholderText = Color(0xFF666666), inputBackground = Color(0xFF1A1A1D),
    secondaryButton = Color(0xFF2C2C2C),
)
