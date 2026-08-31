package com.easypocket.mobile.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.easypocket.mobile.ui.theme.LocalAppColors

private val ItemListTrailingSpace = 44.dp

@Composable
fun AppItemList(
    modifier: Modifier = Modifier,
    footerText: String? = null,
    listState: LazyListState = rememberLazyListState(),
    content: LazyListScope.() -> Unit,
) {
    val appColors = LocalAppColors.current
    Column(modifier = modifier.fillMaxSize()) {
        ListItemGroup(
            modifier = Modifier
                .weight(1f)
                .padding(top = 12.dp, start = 16.dp, end = 16.dp),
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(top = 4.dp, bottom = ItemListTrailingSpace),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                content()
            }
        }
        if (footerText != null) {
            Text(
                text = footerText,
                color = appColors.textSecondary,
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            )
        }
    }
}
