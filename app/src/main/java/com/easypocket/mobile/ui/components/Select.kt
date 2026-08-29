package com.easypocket.mobile.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.easypocket.mobile.ui.theme.LocalAppColors

data class SelectOption(
    val id: String,
    val label: String,
    val trailing: String? = null,
)

@Composable
fun SelectField(
    options: List<SelectOption>,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
    selectedId: String? = null,
    label: String? = null,
    placeholder: String = "",
) {
    val appColors = LocalAppColors.current
    var expanded by remember { mutableStateOf(false) }
    val selected = options.firstOrNull { it.id == selectedId }
    val displayText = selected?.label ?: label ?: placeholder

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(45.dp)
            .clip(RoundedCornerShape(999.dp))
            .background(inputContainerColor())
            .clickable { expanded = true }
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            displayText,
            color = if (selected != null) appColors.text else appColors.placeholderText,
            fontSize = 14.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        Icon(
            Icons.Default.KeyboardArrowDown,
            contentDescription = null,
            tint = appColors.text,
            modifier = Modifier.size(14.dp),
        )
    }

    if (expanded) {
        SelectOptionDialog(
            options = options,
            selectedId = selectedId,
            onSelect = { id ->
                onSelect(id)
                expanded = false
            },
            onDismiss = { expanded = false },
        )
    }
}

@Composable
private fun SelectOptionDialog(
    options: List<SelectOption>,
    selectedId: String?,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val appColors = LocalAppColors.current
    AppBottomSheet(
        visible = true,
        onDismiss = onDismiss,
        heightFraction = 0.5f,
    ) {
        Column(Modifier.verticalScroll(rememberScrollState())) {
            options.forEach { option ->
                val isSelected = option.id == selectedId
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(if (isSelected) appColors.surface else Color.Transparent)
                        .clickable { onSelect(option.id) }
                        .padding(vertical = 12.dp, horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        option.label,
                        color = appColors.text,
                        fontSize = 15.sp,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                        modifier = Modifier.weight(1f),
                    )
                    if (option.trailing != null) {
                        Text(
                            option.trailing,
                            color = appColors.textSecondary,
                            fontSize = 14.sp,
                        )
                    }
                    if (isSelected) {
                        Spacer(Modifier.width(8.dp))
                        Box(
                            Modifier
                                .size(8.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(appColors.textSecondary),
                        )
                    }
                }
            }
        }
    }
}
