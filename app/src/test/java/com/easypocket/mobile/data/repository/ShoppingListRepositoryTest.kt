package com.easypocket.mobile.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.easypocket.mobile.data.local.EasyPocketDatabase
import com.easypocket.mobile.data.local.ProductLastCategoryEntity
import com.easypocket.mobile.data.local.ShoppingListItemEntity
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ShoppingListRepositoryTest {
    private lateinit var db: EasyPocketDatabase
    private lateinit var repo: ShoppingListRepository

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, EasyPocketDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repo = ShoppingListRepository(db, db.listDao())
    }

    @After
    fun tearDown() { db.close() }

    @Test
    fun `create keeps provided icon`() = runTest {
        val list = repo.create("Compras", "🛒")
        assertEquals("🛒", repo.getById(list.id)!!.icon)
    }

    @Test
    fun `create falls back to dollar when icon is blank`() = runTest {
        val list = repo.create("Compras", "")
        assertEquals("$", repo.getById(list.id)!!.icon)
    }

    @Test
    fun `create without icon defaults to dollar`() = runTest {
        val list = repo.create("Compras")
        assertEquals("$", repo.getById(list.id)!!.icon)
    }

    @Test
    fun `rename updates title and icon`() = runTest {
        val list = repo.create("Compras", "🛒")
        repo.rename(list.id, "Nuevo", "A")
        val updated = repo.getById(list.id)!!
        assertEquals("Nuevo", updated.title)
        assertEquals("A", updated.icon)
    }

    private suspend fun seedProducts(vararg ids: String) {
        ids.forEach { db.productDao().insert(com.easypocket.mobile.data.local.ProductEntity(it, "Producto $it", "u")) }
    }

    private suspend fun addItem(listId: String, productId: String, categoryId: String?) {
        db.listDao().insertItem(
            ShoppingListItemEntity(
                shoppingListId = listId,
                productId = productId,
                storeId = null,
                categoryId = categoryId,
                quantity = 1.0,
            )
        )
    }

    @Test
    fun `setCategory re-categorizes every item of the list`() = runTest {
        db.categoryDao().insert(com.easypocket.mobile.data.local.CategoryEntity("cat-a", "A"))
        db.categoryDao().insert(com.easypocket.mobile.data.local.CategoryEntity("cat-b", "B"))
        seedProducts("p1", "p2", "p3")
        val list = repo.create("Compras")
        addItem(list.id, "p1", "cat-a")
        addItem(list.id, "p2", null)
        addItem(list.id, "p3", "cat-b")

        repo.setCategory(list.id, "cat-b")

        val updated = repo.getById(list.id)!!
        assertEquals("cat-b", updated.categoryId)
        updated.items.forEach { assertEquals("cat-b", it.categoryId) }
    }

    @Test
    fun `setCategory with null clears list and items categories`() = runTest {
        db.categoryDao().insert(com.easypocket.mobile.data.local.CategoryEntity("cat-a", "A"))
        seedProducts("p1")
        val list = repo.create("Compras", categoryId = "cat-a")
        addItem(list.id, "p1", "cat-a")

        repo.setCategory(list.id, null)

        val updated = repo.getById(list.id)!!
        assertNull(updated.categoryId)
        updated.items.forEach { assertNull(it.categoryId) }
    }

    @Test
    fun `setCategory never touches product last-category memory`() = runTest {
        db.categoryDao().insert(com.easypocket.mobile.data.local.CategoryEntity("cat-a", "A"))
        db.categoryDao().insert(com.easypocket.mobile.data.local.CategoryEntity("cat-b", "B"))
        seedProducts("p1")
        db.productLastCategoryDao().upsert(ProductLastCategoryEntity("p1", "cat-a"))
        val list = repo.create("Compras")
        addItem(list.id, "p1", null)

        repo.setCategory(list.id, "cat-b")

        assertEquals("cat-a", db.productLastCategoryDao().getByProductId("p1")!!.categoryId)
    }
}
