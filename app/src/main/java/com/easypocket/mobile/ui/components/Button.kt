package com.easypocket.mobile.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.easypocket.mobile.ui.theme.LocalAppColors

enum class ButtonVariant { PRIMARY, SECONDARY, DESTRUCTIVE }

@Composable
fun AppButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: ButtonVariant = ButtonVariant.PRIMARY,
    isLoading: Boolean = false,
    enabled: Boolean = true,
    icon: ImageVector? = null,
) {
    val appColors = LocalAppColors.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val isDisabled = !enabled || isLoading

    val baseColor = when (variant) {
        ButtonVariant.PRIMARY -> appColors.primary
        ButtonVariant.SECONDARY -> appColors.surface
        ButtonVariant.DESTRUCTIVE -> appColors.destructive
    }
    val borderColor = when (variant) {
        ButtonVariant.PRIMARY -> Color.Transparent
        ButtonVariant.SECONDARY -> appColors.border
        ButtonVariant.DESTRUCTIVE -> appColors.destructiveBorder
    }
    val contentColor = when (variant) {
        ButtonVariant.PRIMARY -> Color.White
        ButtonVariant.SECONDARY -> appColors.text
        ButtonVariant.DESTRUCTIVE -> appColors.destructiveBorder
    }

    val backgroundColor by animateColorAsState(
        if (isPressed) lerp(baseColor, Color.Black, 0.15f) else baseColor,
        label = "appButtonBackground",
    )

    Box(
        modifier = modifier
            .height(40.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(backgroundColor)
            .border(1.dp, borderColor, RoundedCornerShape(8.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = LocalIndication.current,
                enabled = !isDisabled,
                onClick = onClick,
            )
            .alpha(if (isDisabled) 0.35f else 1f)
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
            if (icon != null) {
                Icon(icon, contentDescription = null, tint = contentColor, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
            }
            if (isLoading) {
                CircularProgressIndicator(
                    color = contentColor,
                    strokeWidth = 2.dp,
                    modifier = Modifier.size(18.dp),
                )
            } else {
                Text(text, color = contentColor, fontSize = 15.sp, fontWeight = FontWeight.Medium)
            }
        }
    }
}

@Composable
fun IconButtonCircle(
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: ButtonVariant = ButtonVariant.SECONDARY,
) {
    val appColors = LocalAppColors.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val baseColor = when (variant) {
        ButtonVariant.PRIMARY -> appColors.primary
        ButtonVariant.SECONDARY -> appColors.surface
        ButtonVariant.DESTRUCTIVE -> appColors.destructive
    }
    val borderColor = when (variant) {
        ButtonVariant.PRIMARY -> Color.Transparent
        ButtonVariant.SECONDARY -> appColors.border
        ButtonVariant.DESTRUCTIVE -> appColors.destructiveBorder
    }
    val contentColor = when (variant) {
        ButtonVariant.PRIMARY -> Color.White
        ButtonVariant.SECONDARY -> appColors.text
        ButtonVariant.DESTRUCTIVE -> appColors.destructiveBorder
    }

    val backgroundColor by animateColorAsState(
        if (isPressed) lerp(baseColor, Color.Black, 0.15f) else baseColor,
        label = "iconButtonBackground",
    )

    Box(
        modifier = modifier
            .size(40.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(backgroundColor)
            .border(1.dp, borderColor, RoundedCornerShape(8.dp))
            .clickable(interactionSource = interactionSource, indication = LocalIndication.current, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = contentColor, modifier = Modifier.size(20.dp))
    }
}
