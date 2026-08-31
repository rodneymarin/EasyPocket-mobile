package com.easypocket.mobile.ui.categories

import androidx.lifecycle.ViewModel
import com.easypocket.mobile.data.repository.CategoryRepository
import com.easypocket.mobile.domain.Category
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class CategoryFormUiState(
    val category: Category? = null,
    val name: String = "",
    val isEdit: Boolean = false,
    val nameError: Boolean = false,
)

@HiltViewModel
class CategoryFormViewModel @Inject constructor(
    private val categoriesRepository: CategoryRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(CategoryFormUiState())
    val uiState: StateFlow<CategoryFormUiState> = _uiState.asStateFlow()

    suspend fun load(categoryId: String?) {
        if (categoryId == null) {
            _uiState.value = CategoryFormUiState()
            return
        }
        val category = categoriesRepository.getAll().firstOrNull { it.id == categoryId }
        if (category != null) {
            _uiState.value = CategoryFormUiState(category = category, name = category.name, isEdit = true)
        } else {
            _uiState.value = CategoryFormUiState(isEdit = true)
        }
    }

    fun setName(name: String) {
        _uiState.update { it.copy(name = name, nameError = false) }
    }

    suspend fun save(onSaved: (String?) -> Unit) {
        val state = _uiState.value
        val name = state.name.trim()
        if (name.isEmpty()) {
            _uiState.update { it.copy(nameError = true) }
            return
        }
        if (categoriesRepository.findDuplicate(name, state.category?.id) != null) {
            _uiState.update { it.copy(nameError = true) }
            return
        }
        var savedId: String? = null
        if (state.isEdit && state.category != null) {
            val updated = state.category.copy(name = name)
            categoriesRepository.update(updated)
            _uiState.update { it.copy(category = updated, nameError = false) }
        } else {
            val created = categoriesRepository.create(name)
            _uiState.update { it.copy(category = created, nameError = false) }
            savedId = created.id
        }
        onSaved(savedId)
    }

    suspend fun delete(onDeleted: () -> Unit) {
        val id = _uiState.value.category?.id ?: return
        categoriesRepository.deleteAll(listOf(id))
        onDeleted()
    }
}
