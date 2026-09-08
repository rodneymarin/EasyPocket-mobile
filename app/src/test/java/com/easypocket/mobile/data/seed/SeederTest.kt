package com.easypocket.mobile.data.seed

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.easypocket.mobile.data.local.EasyPocketDatabase
import com.easypocket.mobile.data.local.ProductEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SeederTest {
    private lateinit var db: EasyPocketDatabase
    private lateinit var seeder: Seeder

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, EasyPocketDatabase::class.java)
            .allowMainThreadQueries().build()
        seeder = Seeder(db)
    }

    @After
    fun tearDown() { db.close() }

    @Test
    fun `seedIfEmpty inserts demo data once`() = runTest {
        seeder.seedIfEmpty()
        assertEquals(2, db.storeDao().getAll().first().size)
        seeder.seedIfEmpty()
        assertEquals(2, db.storeDao().getAll().first().size)
        assertEquals(10, db.productDao().getAll().first().size)
        assertEquals(4, db.categoryDao().getAll().first().size)
        assertEquals(10, db.productLastCategoryDao().getAll().first().size)
        assertEquals(3, db.listDao().getAll().first().size)
    }

    @Test
    fun `seed items carry their category and products remember last category`() = runTest {
        seeder.seedIfEmpty()
        val items = db.listDao().getAll().first().flatMap { it.items }
        assertEquals(10, items.size)
        assertEquals(10, items.count { it.categoryId != null })
        val lastCategories = db.productLastCategoryDao().getAll().first().associate { it.productId to it.categoryId }
        items.forEach { item ->
            assertEquals(lastCategories[item.productId], item.categoryId)
        }
    }

    @Test
    fun `resetToSeed clears and reseeds`() = runTest {
        seeder.seedIfEmpty()
        db.productDao().insert(ProductEntity("extra", "Extra", "kg"))
        seeder.resetToSeed()
        val products = db.productDao().getAll().first()
        assertEquals(10, products.size)
        assertEquals(0, products.count { it.id == "extra" })
    }
}
