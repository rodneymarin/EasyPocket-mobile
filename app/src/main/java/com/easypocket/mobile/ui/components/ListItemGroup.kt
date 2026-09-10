package com.easypocket.mobile.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.easypocket.mobile.ui.theme.LocalAppColors
import com.easypocket.mobile.ui.theme.LocalIsDark

private val ListItemOuterCorner = 16.dp
private val ListItemInnerCorner = 4.dp

@Composable
fun ListItemGroup(
    modifier: Modifier = Modifier,
    verticalArrangement: Arrangement.Vertical = Arrangement.spacedBy(2.dp),
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = verticalArrangement, content = content)
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ListItemRow(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onLongClick: (() -> Unit)? = null,
    isFirst: Boolean = false,
    isLast: Boolean = false,
    backgroundColor: Color? = null,
    highlighted: Boolean = false,
    minHeight: Dp = 56.dp,
    contentPadding: PaddingValues = PaddingValues(horizontal = 12.dp, vertical = 7.5.dp),
    outerCorner: Dp = ListItemOuterCorner,
    innerCorner: Dp = ListItemInnerCorner,
    content: @Composable RowScope.() -> Unit,
) {
    val appColors = LocalAppColors.current
    val isDark = LocalIsDark.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val highlightBase by animateFloatAsState(
        targetValue = if (highlighted) 1f else 0f,
        animationSpec = tween(durationMillis = if (highlighted) 105 else 210, easing = FastOutSlowInEasing),
        label = "newItemHighlightBase",
    )
    val pulseFactor = if (highlighted) {
        rememberInfiniteTransition(label = "newItemPulse").animateFloat(
            initialValue = 0.35f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 175, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse,
            ),
            label = "newItemPulseFactor",
        ).value
    } else {
        1f
    }
    val highlightBackground = appColors.primary.copy(alpha = 0.28f * highlightBase * pulseFactor)

    val overlay by animateColorAsState(
        targetValue = if (isPressed) {
            Color(if (isDark) 0x08FFFFFF.toInt() else 0x0F000000.toInt())
        } else Color.Transparent,
        label = "listItemRowOverlay",
    )

    val shape = listRowShape(isFirst, isLast, outerCorner, innerCorner)
    val rowBackground = backgroundColor ?: appColors.cardBackground

    Box {
        Row(
            modifier = modifier
                .fillMaxWidth()
                .clip(shape)
                .background(rowBackground)
                .combinedClickable(
                    interactionSource = interactionSource,
                    indication = null,
                    enabled = true,
                    onClick = onClick,
                    onLongClick = onLongClick,
                )
                .heightIn(min = minHeight)
                .padding(contentPadding),
            verticalAlignment = Alignment.CenterVertically,
            content = content,
        )
        Box(Modifier.matchParentSize().clip(shape).background(highlightBackground))
        Box(Modifier.matchParentSize().clip(shape).background(overlay))
    }
}

private fun listRowShape(isFirst: Boolean, isLast: Boolean, outer: Dp, inner: Dp): RoundedCornerShape {
    return when {
        isFirst && isLast -> RoundedCornerShape(outer)
        isFirst -> RoundedCornerShape(topStart = outer, topEnd = outer, bottomStart = inner, bottomEnd = inner)
        isLast -> RoundedCornerShape(topStart = inner, topEnd = inner, bottomStart = outer, bottomEnd = outer)
        else -> RoundedCornerShape(inner)
    }
}
