package com.easypocket.mobile.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.easypocket.mobile.data.local.EasyPocketDatabase
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
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
        repo = ShoppingListRepository(db.listDao())
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
}
