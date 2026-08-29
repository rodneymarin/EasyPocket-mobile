package com.easypocket.mobile.ui.products

import androidx.lifecycle.ViewModel
import com.easypocket.mobile.data.repository.CategoryRepository
import com.easypocket.mobile.data.repository.ProductRepository
import com.easypocket.mobile.data.repository.StoreRepository
import com.easypocket.mobile.domain.Category
import com.easypocket.mobile.domain.ListLogic
import com.easypocket.mobile.domain.Price
import com.easypocket.mobile.domain.Product
import com.easypocket.mobile.domain.Store
import com.easypocket.mobile.domain.UnitOfMeasurement
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class ProductPriceRow(
    val storeId: String,
    val storeName: String,
    val value: String,
)

data class ProductFormUiState(
    val productId: String? = null,
    val name: String = "",
    val unit: UnitOfMeasurement = UnitOfMeasurement.UNIT,
    val categoryId: String? = null,
    val prices: List<ProductPriceRow> = emptyList(),
    val availableStores: List<Store> = emptyList(),
    val availableCategories: List<Category> = emptyList(),
    val nameError: Boolean = false,
    val isEdit: Boolean = false,
)

@HiltViewModel
class ProductFormViewModel @Inject constructor(
    private val productsRepository: ProductRepository,
    private val storesRepository: StoreRepository,
    private val categoriesRepository: CategoryRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProductFormUiState())
    val uiState: StateFlow<ProductFormUiState> = _uiState.asStateFlow()

    private var allStores: List<Store> = emptyList()

    suspend fun load(productId: String?) {
        val stores = storesRepository.getAll()
        allStores = stores
        val categories = categoriesRepository.getAll()
        if (productId == null) {
            _uiState.value = ProductFormUiState(availableStores = stores, availableCategories = categories)
            return
        }
        val product = productsRepository.getAll().firstOrNull { it.id == productId }
        if (product != null) {
            _uiState.value = ProductFormUiState(
                productId = product.id,
                name = product.productName,
                unit = product.unitOfMeasurement,
                categoryId = product.categoryId,
                prices = product.prices.map { ProductPriceRow(it.storeId, storeNameOf(it.storeId), ListLogic.trimQuantity(it.value)) },
                availableStores = stores,
                availableCategories = categories,
                isEdit = true,
            )
            recomputeAvailable()
        } else {
            _uiState.value = ProductFormUiState(
                isEdit = true,
                productId = productId,
                availableStores = stores,
                availableCategories = categories,
            )
        }
    }

    fun setName(name: String) {
        _uiState.update { it.copy(name = name, nameError = false) }
    }

    fun setUnit(unit: UnitOfMeasurement) {
        _uiState.update { it.copy(unit = unit) }
    }

    fun setCategoryId(categoryId: String?) {
        _uiState.update { it.copy(categoryId = categoryId) }
    }

    fun addPrice(storeId: String, value: String) {
        val parsed = value.toDoubleOrNull() ?: return
        if (parsed < 0) return
        if (_uiState.value.prices.any { it.storeId == storeId }) return
        val row = ProductPriceRow(storeId, storeNameOf(storeId), value)
        _uiState.update { it.copy(prices = it.prices + row) }
        recomputeAvailable()
    }

    fun updatePrice(storeId: String, value: String) {
        val parsed = value.toDoubleOrNull() ?: return
        if (parsed < 0) return
        _uiState.update { state ->
            state.copy(prices = state.prices.map { if (it.storeId == storeId) it.copy(value = value) else it })
        }
    }

    fun removePrice(storeId: String) {
        _uiState.update { it.copy(prices = it.prices.filter { p -> p.storeId != storeId }) }
        recomputeAvailable()
    }

    suspend fun save(onSaved: () -> Unit) {
        val state = _uiState.value
        val name = state.name.trim()
        if (name.isEmpty()) {
            _uiState.update { it.copy(nameError = true) }
            return
        }
        if (productsRepository.findDuplicate(name, state.productId) != null) {
            _uiState.update { it.copy(nameError = true) }
            return
        }
        val prices = state.prices.mapNotNull { r -> r.value.toDoubleOrNull()?.let { Price(r.storeId, it) } }
        val saved = if (state.isEdit && state.productId != null) {
            val product = Product(state.productId, name, state.unit, prices, state.categoryId)
            productsRepository.update(product)
            product
        } else {
            productsRepository.create(name, state.unit, prices, state.categoryId)
        }
        _uiState.update { it.copy(productId = saved.id, nameError = false) }
        onSaved()
    }

    suspend fun delete(onDeleted: () -> Unit) {
        val id = _uiState.value.productId ?: return
        productsRepository.deleteAll(listOf(id))
        onDeleted()
    }

    fun toProduct(): Product = Product(
        id = _uiState.value.productId ?: "",
        productName = _uiState.value.name.trim(),
        unitOfMeasurement = _uiState.value.unit,
        prices = _uiState.value.prices.mapNotNull { r -> r.value.toDoubleOrNull()?.let { Price(r.storeId, it) } },
    )

    private fun storeNameOf(storeId: String): String =
        allStores.firstOrNull { it.id == storeId }?.description ?: storeId

    private fun recomputeAvailable() {
        val pricedIds = _uiState.value.prices.map { it.storeId }.toSet()
        _uiState.update { it.copy(availableStores = allStores.filter { s -> s.id !in pricedIds }) }
    }
}
