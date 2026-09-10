package com.easypocket.mobile.ui.lists

import androidx.lifecycle.ViewModel
import com.easypocket.mobile.data.repository.CategoryRepository
import com.easypocket.mobile.data.repository.ProductRepository
import com.easypocket.mobile.data.repository.ShoppingListRepository
import com.easypocket.mobile.domain.Category
import com.easypocket.mobile.domain.ListLogic
import com.easypocket.mobile.domain.ShoppingList
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class ListCardData(
    val list: ShoppingList,
    val itemCount: Int,
    val total: Double,
    val hasPricedItems: Boolean,
    val category: Category?,
)

data class ListsUiState(
    val lists: List<ListCardData> = emptyList(),
    val categories: List<Category> = emptyList(),
    val search: String = "",
    val filtered: List<ListCardData> = emptyList(),
    val isLoading: Boolean = true,
)

@HiltViewModel
class ListListViewModel @Inject constructor(
    private val listsRepository: ShoppingListRepository,
    private val productsRepository: ProductRepository,
    private val categoryRepository: CategoryRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ListsUiState())
    val uiState: StateFlow<ListsUiState> = _uiState.asStateFlow()

    suspend fun refresh() {
        try {
            val lists = listsRepository.getAll()
            val productsById = productsRepository.getAll().associateBy { it.id }
            val categoriesById = categoryRepository.getAll().associateBy { it.id }
            val cards = lists.map { list ->
                ListCardData(
                    list = list,
                    itemCount = list.items.size,
                    total = ListLogic.totalAmount(list, productsById, null),
                    hasPricedItems = list.items.any { ListLogic.itemTotal(it, productsById[it.productId]) > 0.0 },
                    category = list.categoryId?.let { categoriesById[it] },
                )
            }
            _uiState.value = _uiState.value.copy(
                lists = cards,
                categories = categoriesById.values.toList(),
                filtered = filter(cards, _uiState.value.search),
                isLoading = false,
            )
        } catch (t: Throwable) {
            _uiState.value = _uiState.value.copy(isLoading = false)
        }
    }

    fun setSearch(query: String) {
        _uiState.value = _uiState.value.copy(
            search = query,
            filtered = filter(_uiState.value.lists, query),
        )
    }

    suspend fun createList(title: String, icon: String, categoryId: String? = null): String? {
        if (title.isBlank()) return null
        val list = listsRepository.create(title.trim(), icon, categoryId)
        refresh()
        return list.id
    }

    suspend fun deleteList(id: String): ShoppingList? {
        val deleted = listsRepository.delete(id)
        refresh()
        return deleted
    }

    suspend fun restoreList(list: ShoppingList) {
        listsRepository.restoreList(list)
        refresh()
    }

    private fun filter(cards: List<ListCardData>, query: String): List<ListCardData> {
        val normalized = ListLogic.normalize(query.trim())
        if (normalized.isEmpty()) return cards
        return cards.filter { ListLogic.normalize(it.list.title).contains(normalized) }
    }
}
