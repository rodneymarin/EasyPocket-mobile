package com.easypocket.mobile.ui.lists

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.lifecycle.ViewModel
import com.easypocket.mobile.data.repository.ProductRepository
import com.easypocket.mobile.data.repository.ShoppingListRepository
import com.easypocket.mobile.data.repository.StoreRepository
import com.easypocket.mobile.domain.ListLogic
import com.easypocket.mobile.domain.Product
import com.easypocket.mobile.domain.ShoppingList
import com.easypocket.mobile.domain.ShoppingListItem
import com.easypocket.mobile.domain.Store
import com.easypocket.mobile.i18n.Language
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class ListDetailUiState(
    val list: ShoppingList? = null,
    val stores: List<Store> = emptyList(),
    val productsById: Map<String, Product> = emptyMap(),
    val storeFilter: String? = null,
    val selection: Set<Long> = emptySet(),
    val isLoading: Boolean = true,
) {
    val isSelectionMode: Boolean get() = selection.isNotEmpty()

    val allSelectedPinned: Boolean
        get() {
            val current = list ?: return false
            if (selection.isEmpty()) return false
            return selection.all { id -> current.items.firstOrNull { it.id == id }?.pinned == true }
        }

    val visibleItems: List<ShoppingListItem>
        get() = list?.items?.filter { storeFilter == null || it.storeId == storeFilter } ?: emptyList()

    val orderedItems: List<ShoppingListItem>
        get() = list?.let { ListLogic.orderedItems(it.items, productsById) } ?: emptyList()

    val pendingItems: List<ShoppingListItem>
        get() = orderedItems.filter { !it.done }

    val doneItems: List<ShoppingListItem>
        get() = orderedItems.filter { it.done }

    val visibleTotal: Double
        get() = list?.let { ListLogic.totalAmount(it, productsById, storeFilter) } ?: 0.0

    val cartTotal: Double
        get() = list?.let { ListLogic.cartAmount(it, productsById, storeFilter) } ?: 0.0

    val hasItems: Boolean get() = list?.items?.isNotEmpty() == true

    val hasDoneItems: Boolean get() = doneItems.isNotEmpty()

    val filterStores: List<Store>
        get() {
            val usedIds = list?.items?.mapNotNull { it.storeId }?.toSet() ?: emptySet()
            return stores.filter { it.id in usedIds }.sortedBy { ListLogic.normalize(it.description) }
        }
}

@HiltViewModel
class ListDetailViewModel @Inject constructor(
    private val listsRepository: ShoppingListRepository,
    private val storesRepository: StoreRepository,
    private val productsRepository: ProductRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ListDetailUiState())
    val uiState: StateFlow<ListDetailUiState> = _uiState.asStateFlow()

    suspend fun load(listId: String) {
        _uiState.value = _uiState.value.copy(isLoading = true)
        try {
            val list = listsRepository.getById(listId)
            val products = productsRepository.getAll()
            val stores = storesRepository.getAll()
            _uiState.value = _uiState.value.copy(
                list = list,
                productsById = products.associateBy { it.id },
                stores = stores,
                isLoading = false,
            )
        } catch (t: Throwable) {
            _uiState.value = _uiState.value.copy(isLoading = false)
        }
    }

    fun setStoreFilter(storeId: String?) {
        _uiState.value = _uiState.value.copy(storeFilter = storeId)
    }

    suspend fun toggleDone(item: ShoppingListItem) {
        val current = _uiState.value.list ?: return
        val updated = item.copy(done = !item.done)
        _uiState.value = _uiState.value.copy(
            list = current.copy(items = current.items.map { if (it.id == item.id) updated else it }),
        )
        try {
            listsRepository.toggleItemDone(item.id, updated.done)
        } catch (t: Throwable) {
            _uiState.value = _uiState.value.copy(
                list = current,
            )
        }
    }

    suspend fun renameList(title: String) {
        val listId = _uiState.value.list?.id ?: return
        listsRepository.updateTitle(listId, title)
        val current = _uiState.value.list ?: return
        _uiState.value = _uiState.value.copy(list = current.copy(title = title))
    }

    suspend fun uncheckAll() {
        val current = _uiState.value.list ?: return
        _uiState.value = _uiState.value.copy(list = current.copy(items = current.items.map { it.copy(done = false) }))
        listsRepository.uncheckAll(current.id)
    }

    suspend fun removeCompleted() {
        val current = _uiState.value.list ?: return
        val doneIds = current.items.filter { it.done }.map { it.id }
        if (doneIds.isEmpty()) return
        _uiState.value = _uiState.value.copy(list = current.copy(items = current.items.filter { !it.done }))
        listsRepository.removeItems(doneIds)
        resetFilterIfEmpty()
    }

    fun copyToClipboard(context: Context, language: Language) {
        val current = _uiState.value.list ?: return
        val text = ListLogic.clipboardText(current, _uiState.value.productsById, language)
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("shopping_list", text))
    }

    fun setSelection(ids: Set<Long>) {
        _uiState.value = _uiState.value.copy(selection = ids)
    }

    fun clearSelection() {
        _uiState.value = _uiState.value.copy(selection = emptySet())
    }

    suspend fun deleteSelected() {
        val ids = _uiState.value.selection
        if (ids.isEmpty()) return
        val current = _uiState.value.list ?: return
        _uiState.value = _uiState.value.copy(
            list = current.copy(items = current.items.filter { it.id !in ids }),
            selection = emptySet(),
        )
        listsRepository.removeItems(ids.toList())
        resetFilterIfEmpty()
    }

    suspend fun moveSelected(toListId: String) {
        val ids = _uiState.value.selection
        if (ids.isEmpty()) return
        val current = _uiState.value.list ?: return
        _uiState.value = _uiState.value.copy(
            list = current.copy(items = current.items.filter { it.id !in ids }),
            selection = emptySet(),
        )
        listsRepository.moveItems(ids.toList(), toListId)
        resetFilterIfEmpty()
    }

    suspend fun pinSelected(pinned: Boolean) {
        val ids = _uiState.value.selection
        if (ids.isEmpty()) return
        val current = _uiState.value.list ?: return
        _uiState.value = _uiState.value.copy(
            list = current.copy(items = current.items.map { if (it.id in ids) it.copy(pinned = pinned) else it }),
            selection = emptySet(),
        )
        listsRepository.pinItems(ids.toList(), pinned)
    }

    suspend fun deleteList() {
        val listId = _uiState.value.list?.id ?: return
        listsRepository.delete(listId)
    }

    suspend fun moveTargetLists(): List<ShoppingList> {
        val currentId = _uiState.value.list?.id ?: return emptyList()
        return listsRepository.getAll().filter { it.id != currentId }
    }

    private fun resetFilterIfEmpty() {
        val current = _uiState.value.list ?: return
        val filter = _uiState.value.storeFilter ?: return
        val hasItemsInStore = current.items.any { it.storeId == filter }
        if (!hasItemsInStore) {
            _uiState.value = _uiState.value.copy(storeFilter = null)
        }
    }
}
