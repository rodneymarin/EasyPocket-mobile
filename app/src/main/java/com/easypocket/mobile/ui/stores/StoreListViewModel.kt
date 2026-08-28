package com.easypocket.mobile.ui.stores

import androidx.lifecycle.ViewModel
import com.easypocket.mobile.data.repository.StoreRepository
import com.easypocket.mobile.domain.ListLogic
import com.easypocket.mobile.domain.Store
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class StoreListUiState(
    val stores: List<Store> = emptyList(),
    val search: String = "",
    val filtered: List<Store> = emptyList(),
    val selection: Set<String> = emptySet(),
    val isLoading: Boolean = true,
) {
    val isSelectionMode: Boolean get() = selection.isNotEmpty()
}

@HiltViewModel
class StoreListViewModel @Inject constructor(
    private val storesRepository: StoreRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(StoreListUiState())
    val uiState: StateFlow<StoreListUiState> = _uiState.asStateFlow()

    suspend fun refresh() {
        try {
            val stores = storesRepository.getAll()
            _uiState.value = _uiState.value.copy(
                stores = stores,
                filtered = filter(stores, _uiState.value.search),
                isLoading = false,
            )
        } catch (t: Throwable) {
            _uiState.value = _uiState.value.copy(isLoading = false)
        }
    }

    fun setSearch(query: String) {
        _uiState.update { it.copy(search = query, filtered = filter(it.stores, query)) }
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
        storesRepository.deleteAll(ids.toList())
        _uiState.update { it.copy(selection = emptySet()) }
        refresh()
    }

    private fun filter(stores: List<Store>, query: String): List<Store> {
        val normalized = ListLogic.normalize(query.trim())
        if (normalized.isEmpty()) return stores
        return stores.filter { ListLogic.normalize(it.description).contains(normalized) }
    }
}
