package com.easypocket.mobile.ui.mainmenu

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.CloudDownload
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.PhoneAndroid
import androidx.compose.material.icons.outlined.RestartAlt
import androidx.compose.material.icons.outlined.UploadFile
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.easypocket.mobile.i18n.Language
import com.easypocket.mobile.i18n.t
import com.easypocket.mobile.settings.ThemeMode
import com.easypocket.mobile.ui.theme.LocalAppColors
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private const val PANEL_WIDTH_DP = 280
private const val DRAG_THRESHOLD_FRACTION = 0.35f

private data class ThemeOption(val mode: ThemeMode, val icon: ImageVector, val labelKey: String)

private val themeOptions = listOf(
    ThemeOption(ThemeMode.LIGHT, Icons.Outlined.LightMode, "menu.light"),
    ThemeOption(ThemeMode.DARK, Icons.Outlined.DarkMode, "menu.dark"),
    ThemeOption(ThemeMode.SYSTEM, Icons.Outlined.PhoneAndroid, "menu.system"),
)

private data class LanguageOption(val lang: Language, val labelKey: String)

private val languageOptions = listOf(
    LanguageOption(Language.ENGLISH, "menu.switchToEnglish"),
    LanguageOption(Language.SPANISH, "menu.switchToSpanish"),
)

@Composable
fun MainMenu(
    visible: Boolean,
    language: Language,
    themeMode: ThemeMode,
    onDismiss: () -> Unit,
    onThemeSelected: (ThemeMode) -> Unit,
    onLanguageSelected: (Language) -> Unit,
    onExport: () -> Unit,
    onImport: () -> Unit,
    onReset: () -> Unit,
    onAbout: () -> Unit,
) {
    val appColors = LocalAppColors.current
    val panelWidth = PANEL_WIDTH_DP.dp
    val panelWidthPx = with(LocalDensity.current) { panelWidth.toPx() }
    val scope = rememberCoroutineScope()
    val offset = remember { Animatable(panelWidthPx) }
    val backdropAlpha by animateFloatAsState(
        targetValue = if (visible) 0.5f else 0f,
        animationSpec = tween(durationMillis = 300),
        label = "mainMenuBackdrop",
    )

    LaunchedEffect(visible) {
        if (visible) offset.animateTo(0f, tween(durationMillis = 300))
        else offset.animateTo(panelWidthPx, tween(durationMillis = 250))
    }

    Box(Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { this.alpha = backdropAlpha }
                .background(Color.Black)
                .then(
                    if (visible) Modifier.pointerInput(Unit) { detectTapGestures { onDismiss() } }
                    else Modifier
                ),
        )
        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .offset { IntOffset(offset.value.roundToInt(), 0) }
                .width(panelWidth)
                .fillMaxHeight()
                .background(appColors.panelBackground, RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp))
                .pointerInput(Unit) {
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            if (offset.value > panelWidthPx * DRAG_THRESHOLD_FRACTION) onDismiss()
                            else scope.launch { offset.animateTo(0f, tween(durationMillis = 250)) }
                        },
                        onHorizontalDrag = { change, dragAmount ->
                            change.consume()
                            val target = (offset.value + dragAmount).coerceIn(0f, panelWidthPx)
                            scope.launch { offset.snapTo(target) }
                        },
                    )
                },
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(start = 20.dp, end = 20.dp, top = 48.dp, bottom = 40.dp),
            ) {
                Text(
                    "EasyPocket",
                    color = appColors.panelText,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.size(30.dp))

                SectionLabel(t("menu.theme", language))
                themeOptions.forEach { option ->
                    val selected = option.mode == themeMode
                    MenuRow(
                        icon = option.icon,
                        label = t(option.labelKey, language),
                        textColor = if (selected) appColors.primary else appColors.panelText,
                        iconTint = if (selected) appColors.primary else appColors.panelText,
                        onClick = {
                            if (!selected) onThemeSelected(option.mode)
                            onDismiss()
                        },
                        trailingCheck = selected,
                    )
                }

                MenuDivider()

                SectionLabel(t("menu.language", language))
                languageOptions.forEach { option ->
                    val selected = option.lang == language
                    MenuRow(
                        icon = Icons.Outlined.Language,
                        label = t(option.labelKey, language),
                        textColor = if (selected) appColors.primary else appColors.panelText,
                        iconTint = if (selected) appColors.primary else appColors.panelText,
                        onClick = {
                            if (!selected) onLanguageSelected(option.lang)
                            onDismiss()
                        },
                        trailingCheck = selected,
                    )
                }

                MenuDivider()

                MenuRow(
                    icon = Icons.Outlined.UploadFile,
                    label = t("menu.exportData", language),
                    textColor = appColors.panelText,
                    iconTint = appColors.panelText,
                    onClick = onExport,
                )
                MenuRow(
                    icon = Icons.Outlined.CloudDownload,
                    label = t("menu.importData", language),
                    textColor = appColors.panelText,
                    iconTint = appColors.panelText,
                    onClick = onImport,
                )
                MenuRow(
                    icon = Icons.Outlined.RestartAlt,
                    label = t("menu.resetToDefaults", language),
                    textColor = appColors.destructiveBorder,
                    iconTint = appColors.destructiveBorder,
                    onClick = onReset,
                )

                MenuDivider()

                MenuRow(
                    icon = Icons.Outlined.Info,
                    label = t("menu.about", language),
                    textColor = appColors.panelText,
                    iconTint = appColors.panelText,
                    onClick = onAbout,
                )
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    val appColors = LocalAppColors.current
    Text(
        text,
        color = appColors.textSecondary.copy(alpha = 0.7f),
        fontSize = 13.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(bottom = 4.dp),
    )
}

@Composable
private fun MenuDivider() {
    val appColors = LocalAppColors.current
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .height(1.dp)
            .background(appColors.panelBorder),
    )
}

@Composable
private fun MenuRow(
    icon: ImageVector,
    label: String,
    textColor: Color,
    iconTint: Color,
    onClick: () -> Unit,
    trailingCheck: Boolean = false,
) {
    val appColors = LocalAppColors.current
    val currentOnClick by rememberUpdatedState(onClick)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .pointerInput(label) { detectTapGestures { currentOnClick() } }
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(22.dp))
        Spacer(Modifier.size(14.dp))
        Text(
            label,
            color = textColor,
            fontSize = 16.sp,
            modifier = Modifier.weight(1f),
        )
        if (trailingCheck) {
            Icon(
                Icons.Filled.CheckCircle,
                contentDescription = null,
                tint = appColors.primary,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}
