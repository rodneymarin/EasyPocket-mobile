package com.easypocket.mobile.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.easypocket.mobile.ui.theme.LocalAppColors

@Composable
fun ConfirmSheet(
    visible: Boolean,
    title: String,
    message: String,
    warning: String? = null,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AppBottomSheet(visible = visible, onDismiss = onDismiss, heightFraction = 0.35f) {
        ConfirmSheetContent(title, message, warning, confirmLabel, onConfirm)
    }
}

@Composable
private fun ColumnScope.ConfirmSheetContent(
    title: String,
    message: String,
    warning: String?,
    confirmLabel: String,
    onConfirm: () -> Unit,
) {
    val appColors = LocalAppColors.current
    Column(Modifier.fillMaxWidth()) {
        Text(
            title,
            color = appColors.text,
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(8.dp))
        Text(
            message,
            color = appColors.text,
            fontSize = 15.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        if (warning != null) {
            Spacer(Modifier.height(8.dp))
            Text(
                warning,
                color = appColors.textSecondary,
                fontSize = 13.sp,
                lineHeight = 18.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        Spacer(Modifier.height(20.dp))
        AppButton(
            text = confirmLabel,
            onClick = onConfirm,
            variant = ButtonVariant.DESTRUCTIVE,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
