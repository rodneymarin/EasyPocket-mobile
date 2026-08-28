package com.easypocket.mobile.ui.lists

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
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ListListViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var db: EasyPocketDatabase

    @After
    fun tearDown() {
        db.close()
    }

    private suspend fun createVm(): ListListViewModel {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, EasyPocketDatabase::class.java)
            .allowMainThreadQueries().build()
        val stores = StoreRepository(db.storeDao())
        val products = ProductRepository(db.productDao(), db.priceDao())
        val lists = ShoppingListRepository(db.listDao())
        Seeder(db).seedIfEmpty()
        return ListListViewModel(lists, products)
    }

    @Test
    fun `search filters by normalized title`() = runTest {
        val vm = createVm()
        vm.refresh()
        assertEquals(3, vm.uiState.value.lists.size)
        vm.setSearch("lacteos")
        assertEquals(1, vm.uiState.value.filtered.size)
        vm.setSearch("")
        assertEquals(3, vm.uiState.value.filtered.size)
    }

    @Test
    fun `card data computes counts and total`() = runTest {
        val vm = createVm()
        vm.refresh()
        val first = vm.uiState.value.lists.first { it.list.title == "Lista Semanal - Verduras" }
        assertEquals(3, first.itemCount)
        assertEquals(0, first.doneCount)
        assertEquals(13.2, first.total, 0.001)
    }

    @Test
    fun `createList returns new id and refreshes`() = runTest {
        val vm = createVm()
        vm.refresh()
        val id = vm.createList("Nueva")
        assertNotNull(id)
        assertTrue(vm.uiState.value.lists.any { it.list.id == id })
    }

    @Test
    fun `deleteList removes it`() = runTest {
        val vm = createVm()
        vm.refresh()
        val id = vm.createList("Nueva")
        assertNotNull(id)
        assertTrue(vm.uiState.value.lists.any { it.list.id == id })
        vm.deleteList(id!!)
        assertFalse(vm.uiState.value.lists.any { it.list.id == id })
    }
}
