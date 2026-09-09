package com.easypocket.mobile.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.easypocket.mobile.domain.Category
import com.easypocket.mobile.ui.theme.LocalAppColors

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CategoryChipSelector(
    categories: List<Category>,
    selectedCategoryId: String?,
    onSelect: (String?) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val appColors = LocalAppColors.current
    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        categories.forEach { category ->
            val selected = category.id == selectedCategoryId
            val shape = RoundedCornerShape(999.dp)
            Box(
                modifier = Modifier
                    .border(2.dp, if (selected) appColors.primary else appColors.border, shape)
                    .padding(2.dp)
                    .clip(shape)
                    .background(
                        if (selected) {
                            appColors.primary.copy(alpha = if (enabled) 0.15f else 0.10f)
                        } else {
                            appColors.inputBackground
                        }
                    )
                    .clickable(enabled = enabled) { onSelect(if (selected) null else category.id) }
                    .padding(horizontal = 14.dp, vertical = 7.dp),
            ) {
                Text(
                    text = "${category.icon} ${category.name}",
                    color = when {
                        selected -> appColors.primary.copy(alpha = if (enabled) 1f else 0.8f)
                        enabled -> appColors.text
                        else -> appColors.textSecondary
                    },
                    fontSize = 13.sp,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                )
            }
        }
    }
}
