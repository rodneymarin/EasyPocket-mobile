package com.easypocket.mobile.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.easypocket.mobile.data.local.EasyPocketDatabase
import com.easypocket.mobile.domain.Price
import com.easypocket.mobile.domain.UnitOfMeasurement
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class RepositoriesTest {
    private lateinit var db: EasyPocketDatabase
    private lateinit var stores: StoreRepository
    private lateinit var products: ProductRepository
    private lateinit var lists: ShoppingListRepository

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, EasyPocketDatabase::class.java)
            .allowMainThreadQueries().build()
        stores = StoreRepository(db.storeDao())
        products = ProductRepository(db.productDao(), db.priceDao())
        lists = ShoppingListRepository(db.listDao())
    }

    @After
    fun tearDown() { db.close() }

    @Test
    fun `stores products and lists sort accents the spanish way`() = runTest {
        stores.create("Zapatería")
        stores.create("Ácido")
        stores.create("Ñandú")

        assertEquals(listOf("Ácido", "Ñandú", "Zapatería"), stores.getAll().map { it.description })

        products.create("Zanahoria", UnitOfMeasurement.UNIT)
        products.create("Azúcar", UnitOfMeasurement.UNIT)
        products.create("Ñame", UnitOfMeasurement.UNIT)

        assertEquals(listOf("Azúcar", "Ñame", "Zanahoria"), products.getAll().map { it.productName })

        lists.create("Útiles")
        lists.create("Bodega")
        lists.create("Ítem")

        assertEquals(listOf("Bodega", "Ítem", "Útiles"), lists.getAll().map { it.title })
    }

    @Test
    fun `create store and update`() = runTest {
        val store = stores.create("Demo Store")
        stores.update(store.copy(description = "Renamed", color = 3))
        val all = stores.getAll()
        assertEquals("Renamed", all.first().description)
        assertEquals(3, all.first().color)
    }

    @Test
    fun `product update replaces prices`() = runTest {
        val s1 = stores.create("S1")
        val s2 = stores.create("S2")
        val product = products.create("Milk", UnitOfMeasurement.LT, listOf(Price(s1.id, 10.0)))
        products.update(product.copy(prices = listOf(Price(s2.id, 12.0))))
        val updated = products.getByName("Milk")!!
        assertEquals(1, updated.prices.size)
        assertEquals(s2.id, updated.prices.first().storeId)
    }

    @Test
    fun `findDuplicate ignores self when editing`() = runTest {
        val p = products.create("Milk", UnitOfMeasurement.LT)
        assertNotNull(products.findDuplicate("milk", excludeId = null))
        assertNull(products.findDuplicate("milk", excludeId = p.id))
        assertNotNull(products.findDuplicate("MILK", excludeId = "other"))
    }

    @Test
    fun `list item lifecycle`() = runTest {
        val p = products.create("Milk", UnitOfMeasurement.LT)
        val list = lists.create("Weekly")
        val itemId = lists.addItem(list.id, p.id, null, 2.0)
        lists.toggleItemDone(itemId, true)
        var loaded = lists.getById(list.id)!!
        assertTrue(loaded.items.first().done)
        lists.uncheckAll(list.id)
        loaded = lists.getById(list.id)!!
        assertTrue(!loaded.items.first().done)
        lists.removeCompleted(list.id)
        assertEquals(1, lists.getById(list.id)!!.items.size)
        lists.removeItems(listOf(itemId))
        assertEquals(0, lists.getById(list.id)!!.items.size)
    }
}