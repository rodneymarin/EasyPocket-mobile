package com.easypocket.mobile.ui.lists

import android.content.ClipboardManager
import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.easypocket.mobile.data.local.EasyPocketDatabase
import com.easypocket.mobile.data.repository.ProductRepository
import com.easypocket.mobile.data.repository.PurchaseHistoryRepository
import com.easypocket.mobile.data.repository.ShoppingListRepository
import com.easypocket.mobile.data.repository.StoreRepository
import com.easypocket.mobile.data.seed.Seeder
import com.easypocket.mobile.i18n.Language
import com.easypocket.mobile.util.MainDispatcherRule
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ListDetailViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var db: EasyPocketDatabase
    private lateinit var listsRepository: ShoppingListRepository
    private lateinit var storesRepository: StoreRepository
    private lateinit var productsRepository: ProductRepository

    @After
    fun tearDown() {
        db.close()
    }

    private suspend fun createVm(): ListDetailViewModel {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, EasyPocketDatabase::class.java)
            .allowMainThreadQueries().build()
        storesRepository = StoreRepository(db.storeDao())
        productsRepository = ProductRepository(db.productDao(), db.priceDao())
        listsRepository = ShoppingListRepository(db.listDao())
        Seeder(db).seedIfEmpty()
        return ListDetailViewModel(
            listsRepository,
            storesRepository,
            productsRepository,
            PurchaseHistoryRepository(
                db = db,
                historyDao = db.purchaseHistoryDao(),
                listDao = db.listDao(),
                productDao = db.productDao(),
                priceDao = db.priceDao(),
                storeDao = db.storeDao(),
            ),
        )
    }

    @Test
    fun `load populates list store filter and selection defaults`() = runTest {
        val vm = createVm()
        vm.load("0oasidu0as9dua0sd")
        val state = vm.uiState.value
        assertEquals("Lista Semanal - Verduras", state.list?.title)
        assertEquals(3, state.list?.items?.size)
        assertEquals(2, state.stores.size)
        assertEquals(null, state.storeFilter)
        assertTrue(state.selection.isEmpty())
        assertFalse(state.isLoading)
    }

    @Test
    fun `toggleDone updates optimistically and persists`() = runTest {
        val vm = createVm()
        vm.load("0oasidu0as9dua0sd")
        val item = vm.uiState.value.list!!.items.first { it.productId == "prod-001" }
        assertFalse(item.done)
        vm.toggleDone(item)
        assertTrue(vm.uiState.value.list!!.items.first { it.productId == "prod-001" }.done)
        vm.load("0oasidu0as9dua0sd")
        assertTrue(vm.uiState.value.list!!.items.first { it.productId == "prod-001" }.done)
    }

    @Test
    fun `setStoreFilter changes totals`() = runTest {
        val vm = createVm()
        vm.load("0oasidu0as9dua0sd")
        assertEquals(13.2, vm.uiState.value.visibleTotal, 0.001)
        assertEquals(0.0, vm.uiState.value.cartTotal, 0.001)
        vm.setStoreFilter("store-demo")
        assertEquals(8.2, vm.uiState.value.visibleTotal, 0.001)
        assertEquals(0.0, vm.uiState.value.cartTotal, 0.001)
    }

    @Test
    fun `setStoreFilter filters the displayed item list`() = runTest {
        val vm = createVm()
        vm.load("0oasidu0as9dua0sd")
        assertEquals(3, vm.uiState.value.pendingItems.size)
        assertTrue(vm.uiState.value.doneItems.isEmpty())

        vm.setStoreFilter("store-demo")
        assertEquals(setOf("prod-001", "prod-002"), vm.uiState.value.pendingItems.map { it.productId }.toSet())
        assertTrue(vm.uiState.value.doneItems.isEmpty())

        vm.setStoreFilter("store-test")
        assertEquals(setOf("prod-003"), vm.uiState.value.pendingItems.map { it.productId }.toSet())
        assertTrue(vm.uiState.value.doneItems.isEmpty())

        vm.setStoreFilter(null)
        assertEquals(3, vm.uiState.value.pendingItems.size)
        assertTrue(vm.uiState.value.doneItems.isEmpty())
    }

    @Test
    fun `setStoreFilter to a store with no items leaves the filtered list empty`() = runTest {
        val vm = createVm()
        vm.load("0oasidu0as9dua0sd")
        assertTrue(vm.uiState.value.hasItems)
        vm.setStoreFilter("store-nope")
        assertTrue(vm.uiState.value.pendingItems.isEmpty())
        assertTrue(vm.uiState.value.doneItems.isEmpty())
    }

    @Test
    fun `uncheckAll clears done flags and keeps pending items`() = runTest {
        val vm = createVm()
        val listId = "aosidoaisud0a89sud0a9sdui"
        vm.load(listId)
        val item = vm.uiState.value.list!!.items.first { it.productId == "prod-004" }
        vm.toggleDone(item)
        assertTrue(vm.uiState.value.list!!.items.first { it.productId == "prod-004" }.done)
        vm.uncheckAll()
        assertTrue(vm.uiState.value.list!!.items.all { !it.done })
        assertEquals(4, vm.uiState.value.list!!.items.size)
        vm.load(listId)
        assertTrue(vm.uiState.value.list!!.items.all { !it.done })
        assertEquals(4, vm.uiState.value.list!!.items.size)
    }

    @Test
    fun `removeCompleted deletes only done items`() = runTest {
        val vm = createVm()
        val listId = "aosidoaisud0a89sud0a9sdui"
        vm.load(listId)
        val item1 = vm.uiState.value.list!!.items.first { it.productId == "prod-004" }
        val item2 = vm.uiState.value.list!!.items.first { it.productId == "prod-005" }
        vm.toggleDone(item1)
        vm.toggleDone(item2)
        vm.removeCompleted()
        assertEquals(2, vm.uiState.value.list!!.items.size)
        assertTrue(vm.uiState.value.list!!.items.none { it.productId == "prod-004" || it.productId == "prod-005" })
        vm.load(listId)
        assertEquals(2, vm.uiState.value.list!!.items.size)
    }

    @Test
    fun `pinSelected marks pinned and reorders pinned first`() = runTest {
        val vm = createVm()
        val listId = "aosidoaisud0a89sud0a9sdui"
        vm.load(listId)
        val item = vm.uiState.value.list!!.items.first { it.productId == "prod-007" }
        vm.setSelection(setOf(item.id))
        vm.pinSelected(true)
        assertTrue(vm.uiState.value.selection.isEmpty())
        val ordered = vm.uiState.value.orderedItems
        assertEquals("Mantequilla", vm.uiState.value.productsById[ordered.first().productId]?.productName)
        vm.load(listId)
        assertTrue(vm.uiState.value.list!!.items.first { it.id == item.id }.pinned)
    }

    @Test
    fun `moveSelected relocates items to target list`() = runTest {
        val vm = createVm()
        val sourceId = "aosidoaisud0a89sud0a9sdui"
        val targetId = "0oasidu0as9dua0sd"
        vm.load(sourceId)
        val item = vm.uiState.value.list!!.items.first { it.productId == "prod-006" }
        val sourceCount = vm.uiState.value.list!!.items.size
        vm.setSelection(setOf(item.id))
        vm.moveSelected(targetId)
        assertEquals(sourceCount - 1, vm.uiState.value.list!!.items.size)
        assertTrue(vm.uiState.value.list!!.items.none { it.id == item.id })
        vm.load(targetId)
        assertTrue(vm.uiState.value.list!!.items.any { it.id == item.id })
        vm.load(sourceId)
        assertTrue(vm.uiState.value.list!!.items.none { it.id == item.id })
    }

    @Test
    fun `copyToClipboard produces expected text`() = runTest {
        val vm = createVm()
        val list = listsRepository.create("Mi lista")
        listsRepository.addItem(list.id, "prod-001", "store-demo", 2.5)
        listsRepository.addItem(list.id, "prod-004", "store-demo", 1.5)
        vm.load(list.id)
        val context = ApplicationProvider.getApplicationContext<Context>()
        vm.copyToClipboard(context, Language.SPANISH)
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val text = clipboard.primaryClip?.getItemAt(0)?.coerceToText(context).toString()
        assertEquals("Papa ... 2.5 Kg\nLeche líquida ... 1.5 Litros", text)
    }

    @Test
    fun `copyToClipboard respects the active store filter`() = runTest {
        val vm = createVm()
        val list = listsRepository.create("Mi lista")
        listsRepository.addItem(list.id, "prod-001", "store-demo", 2.5)
        listsRepository.addItem(list.id, "prod-004", "store-test", 1.5)
        vm.load(list.id)
        vm.setStoreFilter("store-demo")
        val context = ApplicationProvider.getApplicationContext<Context>()
        vm.copyToClipboard(context, Language.SPANISH)
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val text = clipboard.primaryClip?.getItemAt(0)?.coerceToText(context).toString()
        assertEquals("Papa ... 2.5 Kg", text)
    }

    @Test
    fun `deleteSelected removes exactly selected items`() = runTest {
        val vm = createVm()
        val listId = "aosidoaisud0a89sud0a9sdui"
        vm.load(listId)
        val item1 = vm.uiState.value.list!!.items.first { it.productId == "prod-004" }
        val item2 = vm.uiState.value.list!!.items.first { it.productId == "prod-005" }
        vm.setSelection(setOf(item1.id, item2.id))
        vm.deleteSelected()
        assertEquals(2, vm.uiState.value.list!!.items.size)
        assertTrue(vm.uiState.value.selection.isEmpty())
        assertTrue(vm.uiState.value.list!!.items.none { it.productId == "prod-004" || it.productId == "prod-005" })
        vm.load(listId)
        assertEquals(2, vm.uiState.value.list!!.items.size)
    }
}
