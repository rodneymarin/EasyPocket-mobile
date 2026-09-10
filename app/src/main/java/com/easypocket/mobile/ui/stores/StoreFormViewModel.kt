package com.easypocket.mobile.ui.stores

import androidx.lifecycle.ViewModel
import com.easypocket.mobile.data.repository.DeletedStores
import com.easypocket.mobile.data.repository.StoreRepository
import com.easypocket.mobile.domain.Store
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class StoreFormUiState(
    val store: Store? = null,
    val name: String = "",
    val color: Int = 0,
    val isEdit: Boolean = false,
    val nameError: Boolean = false,
)

@HiltViewModel
class StoreFormViewModel @Inject constructor(
    private val storesRepository: StoreRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(StoreFormUiState())
    val uiState: StateFlow<StoreFormUiState> = _uiState.asStateFlow()

    suspend fun load(storeId: String?) {
        if (storeId == null) {
            _uiState.value = StoreFormUiState()
            return
        }
        val store = storesRepository.getAll().firstOrNull { it.id == storeId }
        if (store != null) {
            _uiState.value = StoreFormUiState(store = store, name = store.description, color = store.color, isEdit = true)
        } else {
            _uiState.value = StoreFormUiState(isEdit = true)
        }
    }

    fun setName(name: String) {
        _uiState.update { it.copy(name = name, nameError = false) }
    }

    fun setColor(color: Int) {
        _uiState.update { it.copy(color = color) }
    }

    suspend fun save(onSaved: (String?) -> Unit) {
        val state = _uiState.value
        val name = state.name.trim()
        if (name.isEmpty()) {
            _uiState.update { it.copy(nameError = true) }
            return
        }
        val saved = if (state.isEdit && state.store != null) {
            val updated = state.store.copy(description = name, color = state.color)
            storesRepository.update(updated)
            updated
        } else {
            val created = storesRepository.create(name)
            val withColor = created.copy(color = state.color)
            storesRepository.update(withColor)
            withColor
        }
        _uiState.update { it.copy(store = saved, nameError = false) }
        onSaved(if (state.isEdit) null else saved.id)
    }

    suspend fun delete(): DeletedStores? {
        val id = _uiState.value.store?.id ?: return null
        return storesRepository.deleteAll(listOf(id))
    }

    suspend fun restore(deletion: DeletedStores) {
        storesRepository.restore(deletion)
    }
}
