package com.easypocket.mobile.ui.products

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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ProductListViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var db: EasyPocketDatabase
    private lateinit var productsRepository: ProductRepository
    private lateinit var listsRepository: ShoppingListRepository
    private lateinit var vm: ProductListViewModel

    @After
    fun tearDown() {
        if (::db.isInitialized) db.close()
    }

    private suspend fun createVm(): ProductListViewModel {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, EasyPocketDatabase::class.java)
            .allowMainThreadQueries().build()
        productsRepository = ProductRepository(db, db.productDao(), db.priceDao())
        listsRepository = ShoppingListRepository(db, db.listDao())
        Seeder(db).seedIfEmpty()
        vm = ProductListViewModel(
            productsRepository,
            StoreRepository(db.storeDao()),
        )
        return vm
    }

    @Test
    fun `search filters by normalized product name`() = runTest {
        val vm = createVm()
        vm.refresh()
        assertEquals(10, vm.uiState.value.products.size)
        assertEquals(10, vm.uiState.value.filtered.size)

        vm.setSearch("leche")
        assertEquals(1, vm.uiState.value.filtered.size)
        assertEquals("Leche líquida", vm.uiState.value.filtered.first().productName)

        vm.setSearch("papa")
        assertEquals(1, vm.uiState.value.filtered.size)
        assertEquals("Papa", vm.uiState.value.filtered.first().productName)

        vm.setSearch("")
        assertEquals(10, vm.uiState.value.filtered.size)
    }

    @Test
    fun `selection can be set and cleared`() = runTest {
        val vm = createVm()
        vm.setSelection(setOf("prod-001", "prod-002"))
        assertEquals(setOf("prod-001", "prod-002"), vm.uiState.value.selection)
        vm.clearSelection()
        assertTrue(vm.uiState.value.selection.isEmpty())
    }

    @Test
    fun `deleteSelected removes products their prices and referencing list items`() = runTest {
        val vm = createVm()
        vm.refresh()
        vm.setSelection(setOf("prod-001"))
        vm.deleteSelected()

        assertTrue(vm.uiState.value.selection.isEmpty())
        assertNull(productsRepository.getByName("Papa"))
        assertEquals(9, vm.uiState.value.products.size)

        val list = listsRepository.getById("0oasidu0as9dua0sd")!!
        assertFalse(list.items.any { it.productId == "prod-001" })
        assertTrue(list.items.any { it.productId == "prod-002" })
    }

    @Test
    fun `deleteSelected noop when selection empty`() = runTest {
        val vm = createVm()
        vm.refresh()
        vm.clearSelection()
        vm.deleteSelected()
        assertEquals(10, vm.uiState.value.products.size)
    }
}
