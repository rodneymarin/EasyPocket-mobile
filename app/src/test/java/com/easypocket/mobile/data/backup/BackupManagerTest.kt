package com.easypocket.mobile.data.backup

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.easypocket.mobile.data.local.EasyPocketDatabase
import com.easypocket.mobile.data.local.ShoppingListEntity
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class BackupManagerTest {
    private lateinit var db: EasyPocketDatabase
    private lateinit var manager: BackupManager

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, EasyPocketDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        manager = BackupManager(db)
    }

    @After
    fun tearDown() { db.close() }

    @Test
    fun `export and import preserve list icon`() = runTest {
        db.listDao().insert(ShoppingListEntity("l1", "Compras", "🛒"))

        val json = manager.exportToString()
        db.clearAllTables()
        manager.importData(manager.parse(json).getOrThrow())

        assertEquals("🛒", db.listDao().getById("l1")!!.list.icon)
    }

    @Test
    fun `import backup without icon defaults to dollar`() = runTest {
        val oldBackupJson = """
            {"version":1,"exportedAt":"2026-01-01T00:00:00Z","stores":[],"products":[],"prices":[],
             "shoppingLists":[{"id":"l1","title":"Compras"}],"listItems":[]}
        """.trimIndent()

        manager.importData(manager.parse(oldBackupJson).getOrThrow())

        assertEquals("$", db.listDao().getById("l1")!!.list.icon)
    }
}
