package com.easypocket.mobile.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.easypocket.mobile.data.local.EasyPocketDatabase
import com.easypocket.mobile.data.local.ProductEntity
import com.easypocket.mobile.data.local.PurchaseHistoryEntity
import com.easypocket.mobile.data.local.PurchaseHistoryItemEntity
import kotlinx.coroutines.flow.first
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
class CategoryRepositoryTest {
    private lateinit var db: EasyPocketDatabase
    private lateinit var repo: CategoryRepository

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, EasyPocketDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repo = CategoryRepository(db, db.categoryDao(), db.productDao())
    }

    @After
    fun tearDown() { db.close() }

    @Test
    fun `create generates a 6 char alphanumeric unique code`() = runTest {
        val created = repo.create("Abarrotes")

        assertEquals(6, created.id.length)
        assertTrue(created.id.all { it in 'A'..'Z' || it in '0'..'9' })
        assertEquals("Abarrotes", repo.getAll().single().name)
    }

    @Test
    fun `create generates unique ids across many categories`() = runTest {
        repeat(30) { index -> repo.create("Cat $index") }

        val ids = repo.getAll().map { it.id }
        assertEquals(30, ids.size)
        assertEquals(30, ids.toSet().size)
    }

    @Test
    fun `create stores the provided icon`() = runTest {
        repo.create("Abarrotes", "🛒")

        assertEquals("🛒", repo.getAll().single().icon)
    }

    @Test
    fun `create defaults to dollar icon when none provided`() = runTest {
        repo.create("Abarrotes")
        repo.create("Bebidas", "")

        assertEquals("$", repo.getAll().first { it.name == "Abarrotes" }.icon)
        assertEquals("$", repo.getAll().first { it.name == "Bebidas" }.icon)
    }

    @Test
    fun `update persists icon changes`() = runTest {
        val created = repo.create("Abarrotes", "$")

        repo.update(created.copy(icon = "🛒"))

        assertEquals("🛒", repo.getAll().single().icon)
    }

    @Test
    fun `getAll returns categories ordered alphabetically for spanish speakers`() = runTest {
        repo.create("Zapallos")
        repo.create("Árbol")
        repo.create("Año")
        repo.create("Anillo")

        assertEquals(listOf("Anillo", "Año", "Árbol", "Zapallos"), repo.getAll().map { it.name })
    }

    @Test
    fun `findDuplicate is case insensitive and excludes self`() = runTest {
        val created = repo.create("Abarrotes")

        assertNotNull(repo.findDuplicate("abarrotes", null))
        assertNull(repo.findDuplicate("Abarrotes", created.id))
        assertNull(repo.findDuplicate("Bebidas", created.id))
    }

    @Test
    fun `update renames the category keeping the same id`() = runTest {
        val created = repo.create("Abarrotes")

        repo.update(created.copy(name = "Despensa"))

        val stored = repo.getAll()
        assertEquals(1, stored.size)
        assertEquals(created.id, stored.first().id)
        assertEquals("Despensa", stored.first().name)
    }

    @Test
    fun `deleteAll clears categoryId from products and deletes categories`() = runTest {
        val cat = repo.create("Abarrotes")
        db.productDao().insert(ProductEntity("p1", "Arroz", "u", categoryId = cat.id))
        db.productDao().insert(ProductEntity("p2", "Leche", "lt", categoryId = null))

        repo.deleteAll(listOf(cat.id))

        assertTrue(repo.getAll().isEmpty())
        assertEquals(null, db.productDao().getAll().first().first { it.id == "p1" }.categoryId)
        assertEquals(null, db.productDao().getAll().first().first { it.id == "p2" }.categoryId)
    }

    @Test
    fun `deleteAll keeps history category codes intact`() = runTest {
        val cat = repo.create("Abarrotes")
        db.productDao().insert(ProductEntity("p1", "Arroz", "u", categoryId = cat.id))
        db.purchaseHistoryDao().insertHistory(PurchaseHistoryEntity("h1", "Lista", "$", 0L, 10.0, 1))
        db.purchaseHistoryDao().insertItems(
            listOf(
                PurchaseHistoryItemEntity(
                    historyId = "h1", productName = "Arroz", storeName = null,
                    quantity = 1.0, unitPrice = 10.0, totalPrice = 10.0, categoryCode = cat.id,
                    itemUid = "uid-1",
                )
            )
        )

        repo.deleteAll(listOf(cat.id))

        val items = db.purchaseHistoryDao().observeAll().first().first().items
        assertEquals(cat.id, items.first().categoryCode)
        assertNull(repo.findDuplicate("Abarrotes", null))
    }
}
