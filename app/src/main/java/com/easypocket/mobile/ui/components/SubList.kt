package com.easypocket.mobile.ui.components

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.easypocket.mobile.ui.theme.LocalAppColors

private val SubListRowMinHeight = 32.dp
private val SubListRowContentPadding = PaddingValues(horizontal = 12.dp, vertical = 5.dp)
private val SubListRowOuterCorner = 12.dp

@Composable
fun SubList(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    ListItemGroup(modifier = modifier, content = content)
}

@Composable
fun SubListRow(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isFirst: Boolean = false,
    isLast: Boolean = false,
    backgroundColor: Color? = null,
    content: @Composable RowScope.() -> Unit,
) {
    ListItemRow(
        onClick = onClick,
        modifier = modifier,
        isFirst = isFirst,
        isLast = isLast,
        backgroundColor = backgroundColor ?: LocalAppColors.current.subItemBackground,
        minHeight = SubListRowMinHeight,
        contentPadding = SubListRowContentPadding,
        outerCorner = SubListRowOuterCorner,
        content = content,
    )
}
