package com.easypocket.mobile.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.ViewInAr
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.List
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material.icons.outlined.ViewInAr
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.easypocket.mobile.ui.theme.LocalAppColors

private val unselectedIcons = listOf(
    Icons.Outlined.List, Icons.Outlined.ViewInAr, Icons.Outlined.Category, Icons.Outlined.Storefront, Icons.Outlined.History,
)

private val selectedIcons = listOf(
    Icons.Filled.List, Icons.Filled.ViewInAr, Icons.Filled.Category, Icons.Filled.Storefront, Icons.Filled.History,
)

@Composable
fun BottomBar(
    pageCount: Int,
    currentPage: Int,
    onPageSelected: (Int) -> Unit,
    labels: List<String>,
    modifier: Modifier = Modifier,
) {
    val appColors = LocalAppColors.current
    Surface(color = appColors.background, modifier = modifier.fillMaxWidth()) {
        Column(Modifier.navigationBarsPadding()) {
            Box(Modifier.fillMaxWidth().height(1.dp).background(appColors.border))
            Row(Modifier.fillMaxWidth()) {
                for (index in 0 until pageCount) {
                    val selected = index == currentPage
                    val icon: ImageVector = if (selected) selectedIcons[index % selectedIcons.size]
                    else unselectedIcons[index % unselectedIcons.size]
                    val tint = if (selected) appColors.primary else appColors.tabBarInactive
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clickable(onClick = { onPageSelected(index) })
                            .padding(vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(24.dp))
                        Spacer(Modifier.height(2.dp))
                        Text(
                            labels.getOrElse(index) { "" },
                            color = tint,
                            fontSize = 10.sp,
                            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                            maxLines = 1,
                        )
                    }
                }
            }
        }
    }
}
