package com.easypocket.mobile.ui.lists

import androidx.lifecycle.ViewModel
import com.easypocket.mobile.data.repository.ProductRepository
import com.easypocket.mobile.data.repository.ShoppingListRepository
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
    val doneCount: Int,
    val total: Double,
)

data class ListsUiState(
    val lists: List<ListCardData> = emptyList(),
    val search: String = "",
    val filtered: List<ListCardData> = emptyList(),
    val isLoading: Boolean = true,
)

@HiltViewModel
class ListListViewModel @Inject constructor(
    private val listsRepository: ShoppingListRepository,
    private val productsRepository: ProductRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ListsUiState())
    val uiState: StateFlow<ListsUiState> = _uiState.asStateFlow()

    suspend fun refresh() {
        try {
            val lists = listsRepository.getAll()
            val productsById = productsRepository.getAll().associateBy { it.id }
            val cards = lists.map { list ->
                ListCardData(
                    list = list,
                    itemCount = list.items.size,
                    doneCount = list.items.count { it.done },
                    total = ListLogic.totalAmount(list, productsById, null),
                )
            }
            _uiState.value = _uiState.value.copy(
                lists = cards,
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

    suspend fun createList(title: String): String? {
        if (title.isBlank()) return null
        val list = listsRepository.create(title.trim())
        refresh()
        return list.id
    }

    suspend fun deleteList(id: String) {
        listsRepository.delete(id)
        refresh()
    }

    private fun filter(cards: List<ListCardData>, query: String): List<ListCardData> {
        val normalized = ListLogic.normalize(query.trim())
        if (normalized.isEmpty()) return cards
        return cards.filter { ListLogic.normalize(it.list.title).contains(normalized) }
    }
}
