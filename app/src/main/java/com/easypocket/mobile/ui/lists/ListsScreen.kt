package com.easypocket.mobile.ui.lists

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.easypocket.mobile.i18n.Language
import com.easypocket.mobile.i18n.LocalLanguage
import com.easypocket.mobile.i18n.t
import com.easypocket.mobile.ui.components.AppBottomSheet
import com.easypocket.mobile.ui.components.AppButton
import com.easypocket.mobile.ui.components.AppHeader
import com.easypocket.mobile.ui.components.ButtonVariant
import com.easypocket.mobile.ui.components.ConfirmSheet
import com.easypocket.mobile.ui.components.FormTextField
import com.easypocket.mobile.ui.components.IconButtonCircle
import com.easypocket.mobile.ui.components.LocalToastState
import com.easypocket.mobile.ui.components.PressableCard
import com.easypocket.mobile.ui.components.SearchInput
import com.easypocket.mobile.ui.components.Tag
import com.easypocket.mobile.ui.components.TagSize
import com.easypocket.mobile.ui.components.ToastType
import com.easypocket.mobile.ui.theme.LocalAppColors
import java.util.Locale
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val SEARCH_DEBOUNCE_MS = 300L

@Composable
fun ListsScreen(navController: NavController, onMenuClick: () -> Unit, refreshTick: Int = 0) {
    val vm: ListListViewModel = hiltViewModel()
    val uiState by vm.uiState.collectAsStateWithLifecycle()
    val language = LocalLanguage.current
    val toast = LocalToastState.current
    val appColors = LocalAppColors.current
    val scope = rememberCoroutineScope()

    var searchText by rememberSaveable { mutableStateOf("") }
    var newTitle by remember { mutableStateOf("") }
    var showCreateSheet by rememberSaveable { mutableStateOf(false) }
    var showDeleteSheet by rememberSaveable { mutableStateOf(false) }
    var listToDelete by remember { mutableStateOf<ListCardData?>(null) }

    LaunchedEffect(refreshTick) { vm.refresh() }

    LaunchedEffect(searchText) {
        delay(SEARCH_DEBOUNCE_MS)
        vm.setSearch(searchText)
    }

    fun openDetail(id: String) = navController.navigate("listDetail/$id")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(appColors.background)
            .padding(top = 60.dp),
    ) {
        AppHeader(
            title = t("tab.lists", language),
            onMenuClick = onMenuClick,
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SearchInput(
                value = searchText,
                onValueChange = { searchText = it },
                placeholder = t("search.lists", language),
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.width(10.dp))
            IconButtonCircle(
                icon = Icons.Filled.Add,
                onClick = {
                    newTitle = ""
                    showCreateSheet = true
                },
                variant = ButtonVariant.PRIMARY,
            )
        }
        Box(Modifier.weight(1f).fillMaxWidth()) {
            when {
                uiState.isLoading -> LoadingState(language)
                uiState.filtered.isNotEmpty() -> ListsContent(
                    uiState = uiState,
                    language = language,
                    onCardClick = ::openDetail,
                    onDelete = { listToDelete = it; showDeleteSheet = true },
                )
                else -> EmptyState(
                    text = if (uiState.lists.isEmpty()) t("lists.empty", language) else t("common.noResults", language),
                )
            }
        }
    }

    AppBottomSheet(
        visible = showCreateSheet,
        onDismiss = { showCreateSheet = false },
        heightFraction = 0.75f,
        title = t("listForm.newTitle", language),
    ) {
        ListTitleInput(
            value = newTitle,
            onValueChange = { newTitle = it },
            placeholder = t("listForm.placeholder", language),
        )
        Spacer(Modifier.height(16.dp))
        AppButton(
            text = t("common.create", language),
            onClick = {
                val title = newTitle.trim()
                if (title.isNotEmpty()) {
                    scope.launch {
                        val id = vm.createList(title)
                        if (id != null) {
                            showCreateSheet = false
                            newTitle = ""
                            toast.show(t("toast.listCreated", language), ToastType.SUCCESS)
                            openDetail(id)
                        }
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = newTitle.trim().isNotEmpty(),
        )
    }

    val toDelete = listToDelete
    ConfirmSheet(
        visible = showDeleteSheet && toDelete != null,
        title = t("lists.deleteModal.title", language),
        message = t("lists.deleteModal.confirmMessage", language, mapOf("list" to (toDelete?.list?.title ?: ""))),
        warning = t("lists.deleteModal.warning", language),
        confirmLabel = t("lists.deleteModal.confirm", language),
        onConfirm = {
            toDelete?.list?.id?.let { id ->
                scope.launch {
                    vm.deleteList(id)
                    showDeleteSheet = false
                    listToDelete = null
                    toast.show(t("toast.listDeleted", language), ToastType.SUCCESS)
                }
            }
        },
        onDismiss = {
            showDeleteSheet = false
            listToDelete = null
        },
    )
}

@Composable
private fun ListsContent(
    uiState: ListsUiState,
    language: Language,
    onCardClick: (String) -> Unit,
    onDelete: (ListCardData) -> Unit,
) {
    val appColors = LocalAppColors.current
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = 4.dp, bottom = 8.dp),
    ) {
        items(uiState.filtered, key = { it.list.id }) { card ->
            ListCard(
                card = card,
                language = language,
                onClick = { onCardClick(card.list.id) },
                onDelete = { onDelete(card) },
            )
        }
        item {
            Text(
                text = t("lists.showingCount", language, mapOf("count" to uiState.filtered.size.toString())),
                color = appColors.textSecondary,
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            )
        }
    }
}

@Composable
private fun ListCard(
    card: ListCardData,
    language: Language,
    onClick: () -> Unit,
    onDelete: () -> Unit,
) {
    val appColors = LocalAppColors.current
    PressableCard(onClick = onClick) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = card.list.title,
                    color = appColors.text,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Normal,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Tag(
                        text = t(
                            "list.items",
                            language,
                            mapOf(
                                "completed" to card.doneCount.toString(),
                                "count" to card.itemCount.toString(),
                            ),
                        ),
                        size = TagSize.SM,
                    )
                    Tag(
                        text = t("list.total", language, mapOf("amount" to formatAmount(card.total))),
                        size = TagSize.SM,
                    )
                }
            }
            Spacer(Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .clickableNoIndication(onClick = onDelete),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "delete list",
                    tint = appColors.textSecondary,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}

@Composable
private fun ListTitleInput(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
) {
    FormTextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = placeholder,
    )
}

@Composable
private fun LoadingState(language: Language) {
    val appColors = LocalAppColors.current
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(t("common.loading", language), color = appColors.textSecondary, fontSize = 16.sp)
    }
}

@Composable
private fun EmptyState(text: String) {
    val appColors = LocalAppColors.current
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(text, color = appColors.textSecondary, fontSize = 16.sp)
    }
}

private fun formatAmount(value: Double): String = String.format(Locale.US, "%.2f", value)

private fun Modifier.clickableNoIndication(onClick: () -> Unit): Modifier =
    this.then(Modifier.clip(RoundedCornerShape(8.dp)).clickable(onClick = onClick))
