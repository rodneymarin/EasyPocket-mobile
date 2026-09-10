package com.easypocket.mobile.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.easypocket.mobile.ui.theme.LocalAppColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

enum class ToastType { SUCCESS, ERROR, INFO, WARNING, DESTRUCTIVE }

const val TOAST_DURATION_MS = 3000L
const val DESTRUCTIVE_TOAST_DURATION_MS = 4000L

data class ToastItem(
    val id: Long,
    val message: String,
    val type: ToastType,
    val actionLabel: String? = null,
    val onAction: (suspend () -> Unit)? = null,
    val durationMs: Long = if (type == ToastType.DESTRUCTIVE) DESTRUCTIVE_TOAST_DURATION_MS else TOAST_DURATION_MS,
)

private data class ToastStyle(val background: Color, val icon: ImageVector, val content: Color)

// Destructive toasts match the app's delete buttons exactly: the themed
// destructive background with destructiveBorder text in both themes.
@Composable
private fun toastStyle(type: ToastType): ToastStyle = when (type) {
    ToastType.SUCCESS -> ToastStyle(Color(0xFF4CAF50), Icons.Default.CheckCircle, Color.White)
    ToastType.ERROR -> ToastStyle(Color(0xFFF44336), Icons.Default.Error, Color.White)
    ToastType.INFO -> ToastStyle(Color(0xFF2196F3), Icons.Default.Info, Color.White)
    ToastType.WARNING -> ToastStyle(Color(0xFFFF9800), Icons.Default.Warning, Color.White)
    ToastType.DESTRUCTIVE -> {
        val appColors = LocalAppColors.current
        ToastStyle(appColors.destructive, Icons.Default.Delete, appColors.destructiveBorder)
    }
}

@Stable
class ToastState {
    private val _toasts = mutableStateListOf<ToastItem>()
    val toasts: List<ToastItem> get() = _toasts
    private var nextId = 1L

    fun show(
        message: String,
        type: ToastType = ToastType.SUCCESS,
        actionLabel: String? = null,
        onAction: (suspend () -> Unit)? = null,
    ) {
        _toasts.add(ToastItem(id = nextId++, message = message, type = type, actionLabel = actionLabel, onAction = onAction))
    }

    fun dismiss(id: Long) {
        _toasts.removeAll { it.id == id }
    }
}

val LocalToastState = staticCompositionLocalOf { ToastState() }

@Composable
fun ToastHost(state: ToastState = LocalToastState.current) {
    Box(Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 60.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            state.toasts.forEach { item ->
                key(item.id) {
                    ToastView(item = item, onDismiss = { state.dismiss(item.id) })
                }
            }
        }
    }
}

@Composable
private fun ToastView(item: ToastItem, onDismiss: () -> Unit) {
    val transition = remember { MutableTransitionState(false) }
    val scope = rememberCoroutineScope()
    val dismissed = remember { mutableStateOf(false) }
    val style = toastStyle(item.type)

    fun dismiss() {
        if (dismissed.value) return
        dismissed.value = true
        scope.launch {
            transition.targetState = false
            delay(300)
            onDismiss()
        }
    }

    LaunchedEffect(item.id) {
        transition.targetState = true
        delay(item.durationMs)
        dismiss()
    }

    AnimatedVisibility(
        visibleState = transition,
        enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
    ) {
        Surface(
            color = style.background,
            shape = RoundedCornerShape(12.dp),
            shadowElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth()
                .pointerInput(item.id) {
                    var drag = 0f
                    detectVerticalDragGestures(
                        onDragEnd = { if (drag < -60) dismiss() },
                    ) { change, dragAmount ->
                        change.consume()
                        drag += dragAmount
                    }
                },
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    style.icon,
                    contentDescription = null,
                    tint = style.content,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    item.message,
                    color = style.content,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.weight(1f),
                )
                if (item.actionLabel != null && item.onAction != null) {
                    Text(
                        text = item.actionLabel,
                        color = style.content,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .padding(start = 12.dp)
                            .clip(RoundedCornerShape(999.dp))
                            .background(style.content.copy(alpha = 0.15f))
                            .clickable {
                                dismiss()
                                scope.launch { item.onAction?.invoke() }
                            }
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                    )
                }
            }
        }
    }
}
