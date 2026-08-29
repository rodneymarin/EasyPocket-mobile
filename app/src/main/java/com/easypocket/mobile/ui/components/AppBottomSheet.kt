package com.easypocket.mobile.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.easypocket.mobile.ui.theme.LocalAppColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppBottomSheet(
    visible: Boolean,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    heightFraction: Float = 0.6f,
    title: String? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val appColors = LocalAppColors.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val screenHeightDp = LocalConfiguration.current.screenHeightDp.dp
    val density = LocalDensity.current
    val imeHeightDp = with(density) { WindowInsets.ime.getBottom(density).toDp() }
    val statusBarHeightDp = with(density) { WindowInsets.statusBars.getTop(density).toDp() }
    val desiredHeight = screenHeightDp * heightFraction
    val maxSafeHeight = screenHeightDp - imeHeightDp - statusBarHeightDp - 24.dp
    val sheetHeight = if (maxSafeHeight > 0.dp) minOf(desiredHeight, maxSafeHeight) else desiredHeight

    if (visible) {
        ModalBottomSheet(
            onDismissRequest = onDismiss,
            sheetState = sheetState,
            modifier = modifier,
            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
            containerColor = appColors.cardBackground,
            contentColor = appColors.text,
            dragHandle = null,
        ) {
            CompositionLocalProvider(LocalInputBackground provides appColors.panelInputBackground) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(sheetHeight)
                        .padding(
                            start = 24.dp,
                            end = 24.dp,
                            top = if (title != null) 12.dp else 24.dp,
                            bottom = 24.dp,
                        ),
                ) {
                    if (title != null) {
                        Box(Modifier.fillMaxWidth()) {
                            Text(
                                title,
                                color = appColors.text,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.align(Alignment.Center),
                            )
                        }
                        Spacer(Modifier.height(12.dp))
                    }
                    content()
                }
            }
        }
    }
}
