package com.easypocket.mobile.ui.categories

import androidx.lifecycle.ViewModel
import com.easypocket.mobile.data.repository.DeletedCategories
import com.easypocket.mobile.data.repository.CategoryRepository
import com.easypocket.mobile.domain.Category
import com.easypocket.mobile.domain.ListLogic
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class CategoryListUiState(
    val categories: List<Category> = emptyList(),
    val search: String = "",
    val filtered: List<Category> = emptyList(),
    val selection: Set<String> = emptySet(),
    val isLoading: Boolean = true,
) {
    val isSelectionMode: Boolean get() = selection.isNotEmpty()
}

@HiltViewModel
class CategoryListViewModel @Inject constructor(
    private val categoriesRepository: CategoryRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(CategoryListUiState())
    val uiState: StateFlow<CategoryListUiState> = _uiState.asStateFlow()

    suspend fun refresh() {
        try {
            val categories = categoriesRepository.getAll()
            _uiState.value = _uiState.value.copy(
                categories = categories,
                filtered = filter(categories, _uiState.value.search),
                isLoading = false,
            )
        } catch (t: Throwable) {
            _uiState.value = _uiState.value.copy(isLoading = false)
        }
    }

    fun setSearch(query: String) {
        _uiState.update { it.copy(search = query, filtered = filter(it.categories, query)) }
    }

    fun setSelection(ids: Set<String>) {
        _uiState.update { it.copy(selection = ids) }
    }

    fun clearSelection() {
        _uiState.update { it.copy(selection = emptySet()) }
    }

    fun toggleSelection(id: String) {
        _uiState.update { it.copy(selection = if (id in it.selection) it.selection - id else it.selection + id) }
    }

    suspend fun deleteSelected(): DeletedCategories? {
        val ids = _uiState.value.selection
        if (ids.isEmpty()) return null
        val deleted = categoriesRepository.deleteAll(ids.toList())
        _uiState.update { it.copy(selection = emptySet()) }
        refresh()
        return deleted
    }

    suspend fun restore(deletion: DeletedCategories) {
        categoriesRepository.restore(deletion)
        refresh()
    }

    private fun filter(categories: List<Category>, query: String): List<Category> {
        val normalized = ListLogic.normalize(query.trim())
        if (normalized.isEmpty()) return categories
        return categories.filter { ListLogic.normalize(it.name).contains(normalized) }
    }
}
