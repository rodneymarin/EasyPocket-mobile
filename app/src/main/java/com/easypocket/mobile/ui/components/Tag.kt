package com.easypocket.mobile.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.easypocket.mobile.ui.theme.LocalAppColors
import com.easypocket.mobile.ui.theme.LocalIsDark
import com.easypocket.mobile.ui.theme.StoreColors

enum class TagSize { SM, MD }

@Composable
fun Tag(
    text: String,
    modifier: Modifier = Modifier,
    color: Color? = null,
    size: TagSize = TagSize.MD,
    leading: (@Composable () -> Unit)? = null,
) {
    val appColors = LocalAppColors.current
    val isDark = LocalIsDark.current
    val isSmall = size == TagSize.SM

    val resolvedColor = color

    if (resolvedColor != null) {
        val textColor = StoreColors.shade(resolvedColor, if (isDark) 0.75f else -0.55f)
        val bgAlpha = if (isDark) 0.2f else 0.35f
        Box(
            modifier = modifier
                .background(resolvedColor.copy(alpha = bgAlpha), RoundedCornerShape(999.dp))
                .padding(horizontal = if (isSmall) 10.dp else 14.dp, vertical = if (isSmall) 1.dp else 3.dp),
        ) {
            TagContent(text, textColor, isSmall, leading)
        }
    } else {
        Box(
            modifier = modifier
                .background(appColors.surface, RoundedCornerShape(999.dp))
                .padding(horizontal = if (isSmall) 10.dp else 14.dp, vertical = if (isSmall) 1.dp else 3.dp),
        ) {
            TagContent(text, appColors.surfaceText, isSmall, leading)
        }
    }
}

@Composable
private fun TagContent(text: String, textColor: Color, isSmall: Boolean, leading: (@Composable () -> Unit)?) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        if (leading != null) {
            leading()
            Spacer(Modifier.width(4.dp))
        }
        Text(text, color = textColor, fontSize = if (isSmall) 11.sp else 13.sp)
    }
}
