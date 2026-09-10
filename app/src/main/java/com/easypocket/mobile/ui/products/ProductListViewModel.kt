package com.easypocket.mobile.ui.products

import androidx.lifecycle.ViewModel
import com.easypocket.mobile.data.repository.DeletedProducts
import com.easypocket.mobile.data.repository.ProductRepository
import com.easypocket.mobile.data.repository.StoreRepository
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
    val stores: List<Store> = emptyList(),
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
    private val storesRepository: StoreRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProductListUiState())
    val uiState: StateFlow<ProductListUiState> = _uiState.asStateFlow()

    suspend fun refresh() {
        try {
            val products = productsRepository.getAll()
            val stores = storesRepository.getAll()
            _uiState.value = _uiState.value.copy(
                products = products,
                stores = stores,
                filtered = filter(products, _uiState.value.search),
                isLoading = false,
            )
        } catch (t: Throwable) {
            _uiState.value = _uiState.value.copy(isLoading = false)
        }
    }

    fun setSearch(query: String) {
        _uiState.update {
            it.copy(search = query, filtered = filter(it.products, query))
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

    suspend fun deleteSelected(): DeletedProducts? {
        val ids = _uiState.value.selection
        if (ids.isEmpty()) return null
        val deleted = productsRepository.deleteAll(ids.toList())
        _uiState.update { it.copy(selection = emptySet()) }
        refresh()
        return deleted
    }

    suspend fun restore(deletion: DeletedProducts) {
        productsRepository.restore(deletion)
        refresh()
    }

    private fun filter(products: List<Product>, query: String): List<Product> {
        val normalized = ListLogic.normalize(query.trim())
        if (normalized.isEmpty()) return products
        return products.filter { ListLogic.normalize(it.productName).contains(normalized) }
    }
}
