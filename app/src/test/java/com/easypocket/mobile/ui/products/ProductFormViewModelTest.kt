package com.easypocket.mobile.ui.products

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.easypocket.mobile.data.local.EasyPocketDatabase
import com.easypocket.mobile.data.repository.ProductRepository
import com.easypocket.mobile.data.repository.StoreRepository
import com.easypocket.mobile.data.seed.Seeder
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
class ProductFormViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var db: EasyPocketDatabase
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
        Seeder(db).seedIfEmpty()
    }

    private fun vm() = ProductFormViewModel(productsRepository, storesRepository)

    @Test
    fun `load without id seeds a new form`() = runTest {
        repos()
        val vm = vm()
        vm.load(null)
        val state = vm.uiState.value
        assertEquals(false, state.isEdit)
        assertEquals(null, state.productId)
        assertEquals("", state.name)
        assertEquals(UnitOfMeasurement.UNIT, state.unit)
        assertTrue(state.prices.isEmpty())
        assertEquals(2, state.availableStores.size)
    }

    @Test
    fun `load with id populates existing product and excludes priced stores`() = runTest {
        repos()
        val vm = vm()
        vm.load("prod-001")
        val state = vm.uiState.value
        assertEquals("Papa", state.name)
        assertEquals(UnitOfMeasurement.KG, state.unit)
        assertEquals(true, state.isEdit)
        assertEquals("prod-001", state.productId)
        assertEquals(2, state.prices.size)
        assertTrue(state.availableStores.isEmpty())
    }

    @Test
    fun `setName updates name and clears error`() = runTest {
        repos()
        val vm = vm()
        vm.load(null)
        vm.setName("Naranja")
        assertEquals("Naranja", vm.uiState.value.name)
        assertFalse(vm.uiState.value.nameError)
    }

    @Test
    fun `addPrice and removePrice manage prices and available stores`() = runTest {
        repos()
        val vm = vm()
        vm.load(null)
        assertEquals(2, vm.uiState.value.availableStores.size)
        vm.addPrice("store-demo", "5")
        assertEquals(1, vm.uiState.value.prices.size)
        assertEquals("5", vm.uiState.value.prices.first().value)
        assertEquals("Demo Store", vm.uiState.value.prices.first().storeName)
        assertTrue(vm.uiState.value.availableStores.none { it.id == "store-demo" })
        vm.removePrice("store-demo")
        assertTrue(vm.uiState.value.prices.isEmpty())
        assertEquals(2, vm.uiState.value.availableStores.size)
    }

    @Test
    fun `updatePrice changes an existing price row value`() = runTest {
        repos()
        val vm = vm()
        vm.load(null)
        vm.addPrice("store-demo", "5")
        vm.updatePrice("store-demo", "5.5")
        assertEquals("5.5", vm.uiState.value.prices.first().value)
        assertEquals(1, vm.uiState.value.prices.size)
    }

    @Test
    fun `addPrice rejects invalid value`() = runTest {
        repos()
        val vm = vm()
        vm.load(null)
        vm.addPrice("store-demo", "abc")
        assertTrue(vm.uiState.value.prices.isEmpty())
        vm.addPrice("store-demo", "-3")
        assertTrue(vm.uiState.value.prices.isEmpty())
    }

    @Test
    fun `save creates a new product and calls onSaved`() = runTest {
        repos()
        val vm = vm()
        vm.load(null)
        vm.setName("Zanahoria")
        vm.setUnit(UnitOfMeasurement.KG)
        vm.addPrice("store-demo", "5.5")
        var saved = false
        vm.save { saved = true }
        assertTrue(saved)
        assertFalse(vm.uiState.value.nameError)
        assertNotNull(vm.uiState.value.productId)
        val found = productsRepository.getByName("Zanahoria")
        assertNotNull(found)
        assertEquals(UnitOfMeasurement.KG, found!!.unitOfMeasurement)
        assertEquals(1, found.prices.size)
    }

    @Test
    fun `save rejects duplicate name case-insensitive`() = runTest {
        repos()
        val vm = vm()
        vm.load(null)
        vm.setName("papa")
        vm.setUnit(UnitOfMeasurement.KG)
        var saved = false
        vm.save { saved = true }
        assertFalse(saved)
        assertTrue(vm.uiState.value.nameError)
    }

    @Test
    fun `save does not flag itself as duplicate when editing`() = runTest {
        repos()
        val vm = vm()
        vm.load("prod-001")
        vm.setName("Papa")
        var saved = false
        vm.save { saved = true }
        assertTrue(saved)
        assertFalse(vm.uiState.value.nameError)
    }

    @Test
    fun `save flags a conflicting name when editing`() = runTest {
        repos()
        val vm = vm()
        vm.load("prod-001")
        vm.setName("Cebolla")
        var saved = false
        vm.save { saved = true }
        assertFalse(saved)
        assertTrue(vm.uiState.value.nameError)
    }

    @Test
    fun `save rejects empty name`() = runTest {
        repos()
        val vm = vm()
        vm.load(null)
        vm.setName("   ")
        var saved = false
        vm.save { saved = true }
        assertFalse(saved)
        assertTrue(vm.uiState.value.nameError)
    }

    @Test
    fun `save update persists renamed name unit and prices`() = runTest {
        repos()
        val vm = vm()
        vm.load("prod-001")
        vm.setName("Papa Grande")
        vm.setUnit(UnitOfMeasurement.KG)
        vm.updatePrice("store-demo", "9.9")
        vm.save { }
        val found = productsRepository.getByName("Papa Grande")
        assertNotNull(found)
        assertEquals(UnitOfMeasurement.KG, found!!.unitOfMeasurement)
        assertEquals(2, found.prices.size)
        assertEquals(9.9, found.prices.first { it.storeId == "store-demo" }.value, 0.001)
    }

    @Test
    fun `delete removes the product and calls onDeleted`() = runTest {
        repos()
        val vm = vm()
        vm.load("prod-001")
        var deleted = false
        vm.delete { deleted = true }
        assertTrue(deleted)
        assertNull(productsRepository.getByName("Papa"))
    }
}
