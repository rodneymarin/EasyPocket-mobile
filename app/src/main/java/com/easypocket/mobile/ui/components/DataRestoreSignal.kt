package com.easypocket.mobile.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

// Bumped on the current back stack entry whenever an undo re-inserts data in
// the database, so the screen behind a form can reload and show the restored
// data even if it already reloaded once when the form was popped.
const val KEY_DATA_RESTORED = "data_restored_tick"

fun NavController.notifyDataRestored() {
    val entry = currentBackStackEntry ?: return
    val previous = entry.savedStateHandle.get<Long>(KEY_DATA_RESTORED) ?: 0L
    entry.savedStateHandle[KEY_DATA_RESTORED] = previous + 1L
}

@Composable
fun rememberDataRestoredTick(entry: NavBackStackEntry?): State<Long> =
    remember(entry) {
        entry?.savedStateHandle?.getStateFlow(KEY_DATA_RESTORED, 0L) ?: MutableStateFlow(0L)
    }.collectAsStateWithLifecycle()
