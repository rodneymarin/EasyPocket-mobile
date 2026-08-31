package com.easypocket.mobile.ui.lists

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.easypocket.mobile.data.local.EasyPocketDatabase
import com.easypocket.mobile.data.repository.ProductRepository
import com.easypocket.mobile.data.repository.ShoppingListRepository
import com.easypocket.mobile.data.repository.StoreRepository
import com.easypocket.mobile.data.seed.Seeder
import com.easypocket.mobile.domain.Price
import com.easypocket.mobile.domain.UnitOfMeasurement
import com.easypocket.mobile.util.MainDispatcherRule
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ItemFormViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var db: EasyPocketDatabase
    private lateinit var listsRepository: ShoppingListRepository
    private lateinit var storesRepository: StoreRepository
    private lateinit var productsRepository: ProductRepository

    @After
    fun tearDown() {
        if (::db.isInitialized) db.close()
    }

    private suspend fun repos() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, EasyPocketDatabase::class.java)
            .allowMainThreadQueries().build()
        storesRepository = StoreRepository(db.storeDao())
        productsRepository = ProductRepository(db.productDao(), db.priceDao())
        listsRepository = ShoppingListRepository(db.listDao())
        Seeder(db).seedIfEmpty()
    }

    private fun vm() = ItemFormViewModel(SavedStateHandle(), productsRepository, storesRepository, listsRepository)

    @Test
    fun `quantity validation accepts decimals and rejects garbage`() = runTest {
        repos()
        val vm = vm()
        vm.load("0oasidu0as9dua0sd", -1)
        vm.setQuantity("2.5")
        assertTrue(vm.uiState.value.isQuantityValid)
        vm.setQuantity("abc")
        assertEquals("2.5", vm.uiState.value.quantityText)
        vm.setQuantity("0")
        assertFalse(vm.uiState.value.isQuantityValid)
        vm.setQuantity("")
        assertFalse(vm.uiState.value.isQuantityValid)
    }

    @Test
    fun `selecting product and store computes unit price and total`() = runTest {
        repos()
        val product = productsRepository.create("Special", UnitOfMeasurement.UNIT, listOf(Price("store-demo", 10.0)))
        val vm = vm()
        vm.load("0oasidu0as9dua0sd", -1)
        assertEquals(null, vm.uiState.value.unitPrice)
        assertEquals(null, vm.uiState.value.totalPrice)

        vm.selectProduct(product.id)
        assertEquals(null, vm.uiState.value.unitPrice)
        assertEquals(null, vm.uiState.value.totalPrice)

        vm.setStore("store-demo")
        assertEquals(10.0, vm.uiState.value.unitPrice!!, 0.001)
        assertEquals(10.0, vm.uiState.value.totalPrice!!, 0.001)

        vm.setQuantity("2")
        assertEquals(20.0, vm.uiState.value.totalPrice!!, 0.001)
    }

    @Test
    fun `unitLabelKey switches between singular and plural`() = runTest {
        repos()
        val vm = vm()
        vm.load("0oasidu0as9dua0sd", -1)
        vm.selectProduct("prod-001")
        vm.setQuantity("1")
        assertEquals("unit.kg", vm.uiState.value.unitLabelKey)
        vm.setQuantity("2")
        assertEquals("unit.kg.plural", vm.uiState.value.unitLabelKey)
    }

    @Test
    fun `save inserts a new item`() = runTest {
        repos()
        val vm = vm()
        vm.load("0oasidu0as9dua0sd", -1)
        vm.selectProduct("prod-001")
        vm.setStore("store-demo")
        vm.setQuantity("2")
        var savedId: Long? = null
        vm.save { savedId = it }
        assertNotNull(savedId)
        val list = listsRepository.getById("0oasidu0as9dua0sd")!!
        val inserted = list.items.first { it.id == savedId }
        assertEquals("prod-001", inserted.productId)
        assertEquals(2.0, inserted.quantity, 0.001)
        assertEquals("store-demo", inserted.storeId)
    }

    @Test
    fun `save updates an existing item`() = runTest {
        repos()
        val vm = vm()
        vm.load("0oasidu0as9dua0sd", -1)
        vm.selectProduct("prod-001")
        vm.setStore("store-demo")
        vm.setQuantity("3")
        var saved = false
        vm.save { saved = true }
        assertTrue(saved)
        val list = listsRepository.getById("0oasidu0as9dua0sd")!!
        val item = list.items.first { it.productId == "prod-001" }

        val editVm = vm()
        editVm.load("0oasidu0as9dua0sd", item.id)
        assertTrue(editVm.uiState.value.isEdit)
        editVm.setQuantity("7")
        var updatedId: Long? = null
        editVm.save { updatedId = it }
        assertNull(updatedId)
        val reloaded = listsRepository.getById("0oasidu0as9dua0sd")!!
        val updatedItem = reloaded.items.first { it.id == item.id }
        assertEquals(7.0, updatedItem.quantity, 0.001)
    }

    @Test
    fun `delete removes the item`() = runTest {
        repos()
        val vm = vm()
        vm.load("0oasidu0as9dua0sd", -1)
        vm.selectProduct("prod-001")
        vm.setStore("store-demo")
        vm.setQuantity("2")
        vm.save {}
        val item = listsRepository.getById("0oasidu0as9dua0sd")!!.items.first { it.productId == "prod-001" }

        val editVm = vm()
        editVm.load("0oasidu0as9dua0sd", item.id)
        var deleted = false
        editVm.delete { deleted = true }
        assertTrue(deleted)
        assertTrue(listsRepository.getById("0oasidu0as9dua0sd")!!.items.none { it.id == item.id })
    }

    @Test
    fun `save after edited product is deleted is a no-op and does not insert a dangling item`() = runTest {
        repos()
        val listId = "0oasidu0as9dua0sd"
        val vm = vm()
        vm.load(listId, -1)
        vm.selectProduct("prod-001")
        vm.setStore("store-demo")
        vm.setQuantity("2")
        vm.save {}
        val item = listsRepository.getById(listId)!!.items.first { it.productId == "prod-001" }

        val editVm = vm()
        editVm.load(listId, item.id)
        assertTrue(editVm.uiState.value.isEdit)
        assertEquals("prod-001", editVm.uiState.value.productId)

        productsRepository.deleteAll(listOf("prod-001"))
        assertTrue(listsRepository.getById(listId)!!.items.none { it.productId == "prod-001" })

        editVm.load(listId, item.id)
        assertTrue(editVm.uiState.value.isEdit)
        assertEquals("prod-001", editVm.uiState.value.productId)

        var saved = false
        editVm.save { saved = true }
        assertFalse(saved)
        assertNull(editVm.uiState.value.productId)
        assertTrue(listsRepository.getById(listId)!!.items.none { it.productId == "prod-001" })
    }

    @Test
    fun `clearForm resets selection so save is a no-op`() = runTest {
        repos()
        val listId = "0oasidu0as9dua0sd"
        val vm = vm()
        vm.load(listId, -1)
        vm.selectProduct("prod-001")
        vm.setStore("store-demo")
        vm.setQuantity("2")
        vm.save {}
        val item = listsRepository.getById(listId)!!.items.first { it.productId == "prod-001" }

        val editVm = vm()
        editVm.load(listId, item.id)
        assertTrue(editVm.uiState.value.isEdit)

        editVm.clearForm()

        assertNull(editVm.uiState.value.productId)
        assertNull(editVm.uiState.value.storeId)
        var saved = false
        editVm.save { saved = true }
        assertFalse(saved)
    }

    @Test
    fun `createProduct creates and selects the product`() = runTest {
        repos()
        val vm = vm()
        vm.load("0oasidu0as9dua0sd", -1)
        val id = vm.createProduct("Zanahoria", UnitOfMeasurement.UNIT, listOf(Price("store-demo", 5.0)))
        assertNotNull(id)
        assertEquals(id, vm.uiState.value.productId)
        assertTrue(vm.uiState.value.products.any { it.productName == "Zanahoria" })
        assertNotNull(productsRepository.getByName("Zanahoria"))
    }

    @Test
    fun `createProduct dedupes case-insensitive and returns null on duplicate`() = runTest {
        repos()
        val vm = vm()
        vm.load("0oasidu0as9dua0sd", -1)
        val dup = vm.createProduct("papa", UnitOfMeasurement.KG, emptyList())
        assertNull(dup)
        assertNull(vm.uiState.value.productId)
    }

    @Test
    fun `updateProduct updates an existing product and keeps selection`() = runTest {
        repos()
        val vm = vm()
        vm.load("0oasidu0as9dua0sd", -1)
        vm.selectProduct("prod-001")
        val original = vm.uiState.value.products.first { it.id == "prod-001" }
        val updated = original.copy(productName = "Papita", prices = listOf(Price("store-test", 3.3)))
        vm.updateProduct(updated)
        assertTrue(vm.uiState.value.products.any { it.id == "prod-001" && it.productName == "Papita" })
        assertEquals("Papita", productsRepository.getByName("Papita")!!.productName)
    }
}
