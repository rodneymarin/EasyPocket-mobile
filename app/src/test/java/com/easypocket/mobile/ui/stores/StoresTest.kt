package com.easypocket.mobile.ui.stores

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.easypocket.mobile.data.local.EasyPocketDatabase
import com.easypocket.mobile.data.repository.ProductRepository
import com.easypocket.mobile.data.repository.ShoppingListRepository
import com.easypocket.mobile.data.repository.StoreRepository
import com.easypocket.mobile.data.seed.Seeder
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
class StoresTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var db: EasyPocketDatabase
    private lateinit var storesRepository: StoreRepository
    private lateinit var productsRepository: ProductRepository
    private lateinit var listsRepository: ShoppingListRepository

    @After
    fun tearDown() {
        if (::db.isInitialized) db.close()
    }

    private suspend fun repos() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, EasyPocketDatabase::class.java)
            .allowMainThreadQueries().build()
        storesRepository = StoreRepository(db, db.storeDao(), db.priceDao())
        productsRepository = ProductRepository(db, db.productDao(), db.priceDao())
        listsRepository = ShoppingListRepository(db, db.listDao())
        Seeder(db).seedIfEmpty()
    }

    // ---- StoreListViewModel ----

    private suspend fun listVm() = StoreListViewModel(storesRepository)

    @Test
    fun `search filters by normalized store description`() = runTest {
        repos()
        val vm = listVm()
        vm.refresh()
        assertEquals(2, vm.uiState.value.stores.size)
        assertEquals(2, vm.uiState.value.filtered.size)

        vm.setSearch("demo")
        assertEquals(1, vm.uiState.value.filtered.size)
        assertEquals("Demo Store", vm.uiState.value.filtered.first().description)

        vm.setSearch("zzzz")
        assertEquals(0, vm.uiState.value.filtered.size)

        vm.setSearch("")
        assertEquals(2, vm.uiState.value.filtered.size)
    }

    @Test
    fun `selection can be set cleared and toggled`() = runTest {
        repos()
        val vm = listVm()
        vm.setSelection(setOf("store-demo"))
        assertEquals(setOf("store-demo"), vm.uiState.value.selection)
        vm.toggleSelection("store-test")
        assertEquals(setOf("store-demo", "store-test"), vm.uiState.value.selection)
        vm.toggleSelection("store-demo")
        assertEquals(setOf("store-test"), vm.uiState.value.selection)
        vm.clearSelection()
        assertTrue(vm.uiState.value.selection.isEmpty())
    }

    @Test
    fun `deleteSelected removes stores their prices and nulls list items store id`() = runTest {
        repos()
        val vm = listVm()
        vm.refresh()
        vm.setSelection(setOf("store-demo"))
        vm.deleteSelected()

        assertTrue(vm.uiState.value.selection.isEmpty())
        assertEquals(1, vm.uiState.value.stores.size)
        assertTrue(vm.uiState.value.stores.none { it.id == "store-demo" })

        val papa = productsRepository.getByName("Papa")!!
        assertFalse(papa.prices.any { it.storeId == "store-demo" })
        assertTrue(papa.prices.any { it.storeId == "store-test" })

        val list = listsRepository.getById("0oasidu0as9dua0sd")!!
        val item = list.items.first { it.productId == "prod-001" }
        assertNull(item.storeId)
    }

    @Test
    fun `deleteSelected noop when selection empty`() = runTest {
        repos()
        val vm = listVm()
        vm.refresh()
        vm.clearSelection()
        vm.deleteSelected()
        assertEquals(2, vm.uiState.value.stores.size)
    }

    // ---- StoreFormViewModel ----

    private suspend fun formVm() = StoreFormViewModel(storesRepository)

    @Test
    fun `load without id seeds a new form`() = runTest {
        repos()
        val vm = formVm()
        vm.load(null)
        val state = vm.uiState.value
        assertNull(state.store)
        assertEquals("", state.name)
        assertEquals(0, state.color)
        assertFalse(state.isEdit)
    }

    @Test
    fun `load with id populates existing store`() = runTest {
        repos()
        val vm = formVm()
        vm.load("store-demo")
        val state = vm.uiState.value
        assertEquals("Demo Store", state.name)
        assertEquals(0, state.color)
        assertTrue(state.isEdit)
        assertEquals("store-demo", state.store?.id)
    }

    @Test
    fun `setName updates name and clears error`() = runTest {
        repos()
        val vm = formVm()
        vm.load("store-demo")
        vm.setName("Mercado Central")
        assertEquals("Mercado Central", vm.uiState.value.name)
        assertFalse(vm.uiState.value.nameError)
    }

    @Test
    fun `save rejects empty name`() = runTest {
        repos()
        val vm = formVm()
        vm.load(null)
        vm.setName("   ")
        var saved = false
        vm.save { saved = true }
        assertFalse(saved)
        assertTrue(vm.uiState.value.nameError)
        assertEquals(2, storesRepository.getAll().size)
    }

    @Test
    fun `save creates a store with the selected color`() = runTest {
        repos()
        val vm = formVm()
        vm.load(null)
        vm.setName("Farmacia")
        vm.setColor(5)
        var savedId: String? = null
        vm.save { savedId = it }
        assertNotNull(savedId)
        assertFalse(vm.uiState.value.nameError)
        val created = storesRepository.getAll().first { it.description == "Farmacia" }
        assertEquals(5, created.color)
        assertEquals(created.id, savedId)
    }

    @Test
    fun `save update keeps the same id`() = runTest {
        repos()
        val vm = formVm()
        vm.load("store-demo")
        vm.setName("Mercado Central")
        vm.setColor(2)
        var updatedId: String? = null
        vm.save { updatedId = it }
        assertNull(updatedId)
        assertFalse(vm.uiState.value.nameError)
        val updated = storesRepository.getAll().first { it.id == "store-demo" }
        assertEquals("Mercado Central", updated.description)
        assertEquals(2, updated.color)
    }

    @Test
    fun `delete removes the store and returns its snapshot`() = runTest {
        repos()
        val vm = formVm()
        vm.load("store-demo")
        val deleted = vm.delete()
        assertTrue(deleted != null)
        assertTrue(storesRepository.getAll().none { it.id == "store-demo" })
    }
}
