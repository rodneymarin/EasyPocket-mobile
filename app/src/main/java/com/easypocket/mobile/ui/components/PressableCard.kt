package com.easypocket.mobile.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.easypocket.mobile.ui.theme.LocalIsDark

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PressableCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onLongClick: (() -> Unit)? = null,
    backgroundColor: Color? = null,
    content: @Composable () -> Unit,
) {
    val isDark = LocalIsDark.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val overlay by animateColorAsState(
        targetValue = if (isPressed) {
            Color(if (isDark) 0x08FFFFFF.toInt() else 0x0F000000.toInt())
        } else Color.Transparent,
        label = "pressableCardOverlay",
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(backgroundColor ?: Color.Transparent)
            .combinedClickable(
                interactionSource = interactionSource,
                indication = LocalIndication.current,
                enabled = true,
                onClick = onClick,
                onLongClick = onLongClick,
            )
            .padding(vertical = 12.dp, horizontal = 21.dp),
    ) {
        Box(Modifier.matchParentSize().background(overlay))
        content()
    }
}
