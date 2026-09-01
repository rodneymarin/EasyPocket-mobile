package com.easypocket.mobile.ui.products

import androidx.lifecycle.ViewModel
import com.easypocket.mobile.data.repository.CategoryRepository
import com.easypocket.mobile.data.repository.ProductRepository
import com.easypocket.mobile.data.repository.StoreRepository
import com.easypocket.mobile.domain.Category
import com.easypocket.mobile.domain.ListLogic
import com.easypocket.mobile.domain.Product
import com.easypocket.mobile.domain.Store
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class ProductListUiState(
    val products: List<Product> = emptyList(),
    val categories: List<Category> = emptyList(),
    val stores: List<Store> = emptyList(),
    val selectedCategoryId: String? = null,
    val search: String = "",
    val filtered: List<Product> = emptyList(),
    val selection: Set<String> = emptySet(),
    val isLoading: Boolean = true,
) {
    val isSelectionMode: Boolean get() = selection.isNotEmpty()
}

@HiltViewModel
class ProductListViewModel @Inject constructor(
    private val productsRepository: ProductRepository,
    private val categoriesRepository: CategoryRepository,
    private val storesRepository: StoreRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProductListUiState())
    val uiState: StateFlow<ProductListUiState> = _uiState.asStateFlow()

    suspend fun refresh() {
        try {
            val products = productsRepository.getAll()
            val categories = categoriesRepository.getAll()
            val stores = storesRepository.getAll()
            _uiState.value = _uiState.value.copy(
                products = products,
                categories = categories,
                stores = stores,
                filtered = filter(products, _uiState.value.search, _uiState.value.selectedCategoryId),
                isLoading = false,
            )
        } catch (t: Throwable) {
            _uiState.value = _uiState.value.copy(isLoading = false)
        }
    }

    fun setSearch(query: String) {
        _uiState.update {
            it.copy(search = query, filtered = filter(it.products, query, it.selectedCategoryId))
        }
    }

    fun setCategoryFilter(categoryId: String?) {
        _uiState.update {
            it.copy(selectedCategoryId = categoryId, filtered = filter(it.products, it.search, categoryId))
        }
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

    suspend fun deleteSelected() {
        val ids = _uiState.value.selection
        if (ids.isEmpty()) return
        productsRepository.deleteAll(ids.toList())
        _uiState.update { it.copy(selection = emptySet()) }
        refresh()
    }

    private fun filter(products: List<Product>, query: String, categoryId: String?): List<Product> {
        val byCategory = if (categoryId == null) {
            products
        } else {
            products.filter { it.categoryId == categoryId }
        }
        val normalized = ListLogic.normalize(query.trim())
        if (normalized.isEmpty()) return byCategory
        return byCategory.filter { ListLogic.normalize(it.productName).contains(normalized) }
    }
}
