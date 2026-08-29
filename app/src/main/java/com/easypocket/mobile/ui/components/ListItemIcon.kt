package com.easypocket.mobile.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.easypocket.mobile.domain.ListIcon
import com.easypocket.mobile.ui.theme.LocalAppColors

@Composable
fun ListIconCircle(icon: String, modifier: Modifier = Modifier) {
    val appColors = LocalAppColors.current
    val glyph = icon.ifBlank { ListIcon.DEFAULT }
    val usePrimary = glyph == ListIcon.DEFAULT || glyph.firstOrNull()?.isLetterOrDigit() == true
    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(appColors.iconCircle),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = glyph,
            fontSize = 24.sp,
            color = if (usePrimary) appColors.primary else appColors.text,
        )
    }
}

@Composable
fun ListIconField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        ListIconCircle(icon = value, modifier = Modifier.size(40.dp))
        Spacer(Modifier.width(10.dp))
        FormTextField(
            value = value,
            onValueChange = { onValueChange(ListIcon.sanitize(it)) },
            placeholder = placeholder,
            modifier = Modifier.weight(1f),
        )
    }
}
