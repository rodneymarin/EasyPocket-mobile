package com.easypocket.mobile.ui.lists

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.easypocket.mobile.data.repository.CategoryRepository
import com.easypocket.mobile.data.repository.ProductLastCategoryRepository
import com.easypocket.mobile.data.repository.ProductRepository
import com.easypocket.mobile.data.repository.ShoppingListRepository
import com.easypocket.mobile.data.repository.StoreRepository
import com.easypocket.mobile.domain.Alphabet
import com.easypocket.mobile.domain.Category
import com.easypocket.mobile.domain.ListLogic
import com.easypocket.mobile.domain.Price
import com.easypocket.mobile.domain.Product
import com.easypocket.mobile.domain.ShoppingListItem
import com.easypocket.mobile.domain.Store
import com.easypocket.mobile.domain.UnitOfMeasurement
import com.easypocket.mobile.settings.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

private val QUANTITY_REGEX = Regex("^\\d*\\.?\\d*$")

data class ItemFormUiState(
    val isEdit: Boolean = false,
    val productId: String? = null,
    val storeId: String? = null,
    val categoryId: String? = null,
    val quantityText: String = "1",
    val products: List<Product> = emptyList(),
    val stores: List<Store> = emptyList(),
    val categories: List<Category> = emptyList(),
    val unitLabelKey: String? = null,
    val unitPrice: Double? = null,
    val totalPrice: Double? = null,
    val isLoading: Boolean = true,
) {
    val quantity: Double get() = quantityText.toDoubleOrNull() ?: 0.0

    val isQuantityValid: Boolean
        get() {
            if (!quantityText.matches(QUANTITY_REGEX)) return false
            val q = quantityText.toDoubleOrNull() ?: return false
            return q > 0.0
        }
}

@HiltViewModel
class ItemFormViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val productsRepository: ProductRepository,
    private val storesRepository: StoreRepository,
    private val listsRepository: ShoppingListRepository,
    private val categoryRepository: CategoryRepository,
    private val lastCategoryRepository: ProductLastCategoryRepository,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ItemFormUiState())
    val uiState: StateFlow<ItemFormUiState> = _uiState.asStateFlow()

    private var listId: String? = null
    private var originalItem: ShoppingListItem? = null
    private var rememberedCategories: Map<String, String> = emptyMap()

    suspend fun load(listId: String, itemId: Long) {
        this.listId = listId
        val isEdit = itemId > 0
        _uiState.update { it.copy(isLoading = true, isEdit = isEdit) }
        try {
            val products = productsRepository.getAll()
            val stores = storesRepository.getAll()
            val categories = categoryRepository.getAll()
            rememberedCategories = lastCategoryRepository.getAll().associate { it.productId to it.categoryId }
            var state = _uiState.value.copy(products = products, stores = stores, categories = categories)
            if (isEdit) {
                val item = listsRepository.getById(listId)?.items?.firstOrNull { it.id == itemId }
                if (item != null) {
                    originalItem = item
                    state = state.copy(
                        productId = item.productId,
                        storeId = item.storeId,
                        categoryId = item.categoryId,
                        quantityText = ListLogic.trimQuantity(item.quantity),
                    )
                }
            } else {
                originalItem = null
                val rememberedStore = settingsRepository.getLastStore()
                val storeId = rememberedStore?.takeIf { id -> stores.any { it.id == id } }
                if (rememberedStore != null && storeId == null) {
                    settingsRepository.setLastStore(null)
                }
                state = state.copy(productId = null, storeId = storeId, categoryId = null, quantityText = "1")
            }
            _uiState.value = state.copy(isLoading = false)
            recompute()
        } catch (t: Throwable) {
            _uiState.update { it.copy(isLoading = false) }
        }
    }

    fun selectProduct(productId: String?) {
        _uiState.update { state ->
            state.copy(
                productId = productId,
                // Pre-select the last category the user used for this product, if any.
                categoryId = productId?.let { id -> rememberedCategories[id] },
            )
        }
        recompute()
    }

    fun setStore(storeId: String?) {
        _uiState.update { it.copy(storeId = storeId) }
        recompute()
    }

    fun setCategory(categoryId: String?) {
        _uiState.update { it.copy(categoryId = categoryId) }
    }

    fun setQuantity(text: String) {
        if (text.matches(QUANTITY_REGEX)) {
            _uiState.update { it.copy(quantityText = text) }
            recompute()
        }
    }

    fun clearForm() {
        originalItem = null
        _uiState.update { it.copy(isEdit = false, productId = null, storeId = null, categoryId = null, quantityText = "1") }
        recompute()
    }

    suspend fun save(onSaved: (Long?) -> Unit) {
        val state = _uiState.value
        if (!state.isQuantityValid || state.productId == null) return
        val listId = this.listId ?: return
        if (state.products.none { it.id == state.productId }) {
            _uiState.update { it.copy(productId = null) }
            return
        }
        val quantity = state.quantityText.toDouble()
        val storeId = state.storeId
        val categoryId = state.categoryId
        val savedId = if (state.isEdit && originalItem != null) {
            listsRepository.updateItem(
                originalItem!!.copy(
                    productId = state.productId,
                    storeId = storeId,
                    categoryId = categoryId,
                    quantity = quantity,
                )
            )
            null
        } else {
            listsRepository.addItem(listId, state.productId, storeId, quantity, categoryId)
        }
        // Remember the category used for this product so it is pre-selected
        // the next time the product is added to a list.
        lastCategoryRepository.set(state.productId, categoryId)
        if (storeId != null) {
            settingsRepository.setLastStore(storeId)
        }
        onSaved(savedId)
    }

    suspend fun delete(onDeleted: () -> Unit) {
        val itemId = originalItem?.id ?: return
        listsRepository.removeItems(listOf(itemId))
        onDeleted()
    }

    suspend fun createProduct(name: String, unit: UnitOfMeasurement, prices: List<Price>): String? {
        val trimmed = name.trim()
        if (productsRepository.findDuplicate(trimmed, null) != null) return null
        val product = productsRepository.create(trimmed, unit, prices)
        registerProduct(product)
        return product.id
    }

    suspend fun updateProduct(product: Product) {
        productsRepository.update(product)
        registerProduct(product)
    }

    fun registerProduct(product: Product) {
        val current = _uiState.value.products
        val updated = if (current.any { it.id == product.id }) {
            current.map { if (it.id == product.id) product else it }
        } else {
            current + product
        }
        _uiState.update {
            it.copy(
                products = updated.sortedWith(Alphabet.comparator { it.productName }),
                productId = product.id,
            )
        }
        recompute()
    }

    private fun recompute() {
        val state = _uiState.value
        val product = state.products.firstOrNull { it.id == state.productId }
        val unit = product?.unitOfMeasurement ?: UnitOfMeasurement.UNIT
        val unitLabelKey = ListLogic.unitLabelKey(unit, state.quantity)
        val unitPrice = state.storeId?.let { sid -> product?.prices?.firstOrNull { it.storeId == sid }?.value }
        val totalPrice = unitPrice?.let { it * state.quantity }
        _uiState.update {
            it.copy(unitLabelKey = unitLabelKey, unitPrice = unitPrice, totalPrice = totalPrice)
        }
    }
}
