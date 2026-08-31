package com.easypocket.mobile.ui.components

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavBackStackEntry
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow

const val KEY_NEWLY_ADDED_ID = "newly_added_id"
private const val HIGHLIGHT_DURATION_MS = 1670L

@Composable
fun <T> rememberHighlightedNewItemId(
    navBackStackEntry: NavBackStackEntry?,
    displayedIds: List<T>,
    listState: LazyListState,
): T? {
    val savedStateHandle = navBackStackEntry?.savedStateHandle
    val newlyAdded by remember(savedStateHandle) {
        savedStateHandle?.getStateFlow<T?>(KEY_NEWLY_ADDED_ID, null) ?: MutableStateFlow(null)
    }.collectAsStateWithLifecycle()
    var pendingId by remember { mutableStateOf<T?>(null) }
    var highlighted by remember { mutableStateOf<T?>(null) }

    LaunchedEffect(savedStateHandle, newlyAdded) {
        val id = newlyAdded ?: return@LaunchedEffect
        pendingId = id
        savedStateHandle?.set(KEY_NEWLY_ADDED_ID, null)
    }

    LaunchedEffect(pendingId, displayedIds) {
        val id = pendingId ?: return@LaunchedEffect
        val index = displayedIds.indexOf(id)
        if (index < 0) return@LaunchedEffect
        listState.animateScrollToItem(index)
        highlighted = id
        pendingId = null
    }

    LaunchedEffect(highlighted) {
        if (highlighted != null) {
            delay(HIGHLIGHT_DURATION_MS)
            highlighted = null
        }
    }
    return highlighted
}
