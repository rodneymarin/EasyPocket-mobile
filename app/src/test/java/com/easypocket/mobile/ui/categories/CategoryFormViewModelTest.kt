package com.easypocket.mobile.ui.categories

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.easypocket.mobile.data.local.EasyPocketDatabase
import com.easypocket.mobile.data.local.ProductEntity
import com.easypocket.mobile.data.local.ProductLastCategoryEntity
import com.easypocket.mobile.data.local.ShoppingListEntity
import com.easypocket.mobile.data.local.ShoppingListItemEntity
import com.easypocket.mobile.data.repository.CategoryRepository
import com.easypocket.mobile.util.MainDispatcherRule
import kotlinx.coroutines.flow.first
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
class CategoryFormViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var db: EasyPocketDatabase
    private lateinit var categoriesRepository: CategoryRepository

    @After
    fun tearDown() {
        if (::db.isInitialized) db.close()
    }

    private fun repos() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, EasyPocketDatabase::class.java)
            .allowMainThreadQueries().build()
        categoriesRepository = CategoryRepository(db, db.categoryDao(), db.listDao(), db.productLastCategoryDao())
    }

    private fun vm() = CategoryFormViewModel(categoriesRepository)

    @Test
    fun `load without id prepares a new form`() = runTest {
        repos()
        val v = vm()
        v.load(null)
        val state = v.uiState.value
        assertFalse(state.isEdit)
        assertNull(state.category)
        assertEquals("", state.name)
    }

    @Test
    fun `load with id populates the form for editing`() = runTest {
        repos()
        val created = categoriesRepository.create("Abarrotes")
        val v = vm()
        v.load(created.id)
        val state = v.uiState.value
        assertTrue(state.isEdit)
        assertEquals("Abarrotes", state.name)
        assertEquals(created.id, state.category?.id)
    }

    @Test
    fun `save creates a category with generated id and calls onSaved`() = runTest {
        repos()
        val v = vm()
        v.load(null)
        v.setName("Abarrotes")
        var savedId: String? = null
        v.save { savedId = it }
        assertFalse(v.uiState.value.nameError)
        val stored = categoriesRepository.getAll()
        assertEquals(1, stored.size)
        assertEquals("Abarrotes", stored.first().name)
        assertEquals(6, stored.first().id.length)
        assertEquals(stored.first().id, savedId)
    }

    @Test
    fun `save rejects empty and duplicate names`() = runTest {
        repos()
        categoriesRepository.create("Abarrotes")

        val v = vm()
        v.load(null)
        v.setName("   ")
        var saved = false
        v.save { saved = true }
        assertFalse(saved)
        assertTrue(v.uiState.value.nameError)

        v.setName("abarrotes")
        v.save { saved = true }
        assertFalse(saved)
        assertTrue(v.uiState.value.nameError)
    }

    @Test
    fun `save update renames without duplicating itself`() = runTest {
        repos()
        val created = categoriesRepository.create("Abarrotes")
        val v = vm()
        v.load(created.id)
        v.setName("Despensa")
        var saved = false
        v.save { saved = true }
        assertTrue(saved)
        assertEquals("Despensa", categoriesRepository.getAll().single().name)
    }

    @Test
    fun `load with id populates the icon`() = runTest {
        repos()
        val created = categoriesRepository.create("Abarrotes", "🛒")
        val v = vm()
        v.load(created.id)
        assertEquals("🛒", v.uiState.value.icon)
    }

    @Test
    fun `setIcon updates the form state`() = runTest {
        repos()
        val v = vm()
        v.load(null)
        v.setIcon("🛒")
        assertEquals("🛒", v.uiState.value.icon)
    }

    @Test
    fun `save creates a category with the selected icon`() = runTest {
        repos()
        val v = vm()
        v.load(null)
        v.setName("Abarrotes")
        v.setIcon("🛒")
        v.save { }
        assertEquals("🛒", categoriesRepository.getAll().single().icon)
    }

    @Test
    fun `save update changes the icon keeping the same id`() = runTest {
        repos()
        val created = categoriesRepository.create("Abarrotes", "$")
        val v = vm()
        v.load(created.id)
        v.setName("Abarrotes")
        v.setIcon("B")
        var saved = false
        v.save { saved = true }
        assertTrue(saved)
        val stored = categoriesRepository.getAll().single()
        assertEquals(created.id, stored.id)
        assertEquals("B", stored.icon)
    }

    @Test
    fun `delete removes category, clears item categories and last-category memory`() = runTest {
        repos()
        val created = categoriesRepository.create("Abarrotes")
        db.productDao().insert(ProductEntity("p1", "Arroz", "u"))
        db.listDao().insert(ShoppingListEntity("l1", "Lista"))
        val itemId = db.listDao().insertItem(
            ShoppingListItemEntity(shoppingListId = "l1", productId = "p1", storeId = null, categoryId = created.id, quantity = 1.0)
        )
        db.productLastCategoryDao().upsert(ProductLastCategoryEntity("p1", created.id))
        val v = vm()
        v.load(created.id)
        val deleted = v.delete()
        assertTrue(deleted != null)
        assertTrue(categoriesRepository.getAll().isEmpty())
        val items = db.listDao().getById("l1")!!.items
        assertEquals(1, items.size)
        assertNull(items.first { it.id == itemId }.categoryId)
        assertNull(db.productLastCategoryDao().getByProductId("p1"))
    }
}
