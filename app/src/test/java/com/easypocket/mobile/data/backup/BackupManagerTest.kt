package com.easypocket.mobile.data.backup

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.easypocket.mobile.data.local.EasyPocketDatabase
import com.easypocket.mobile.data.seed.Seeder
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class BackupManagerTest {
    private lateinit var db: EasyPocketDatabase
    private lateinit var seeder: Seeder
    private lateinit var manager: BackupManager

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, EasyPocketDatabase::class.java)
            .allowMainThreadQueries().build()
        seeder = Seeder(db)
        manager = BackupManager(db)
    }

    @After
    fun tearDown() { db.close() }

    private val validJson = """
        {"version":1,"exportedAt":"2026-08-27T00:00:00Z",
         "stores":[{"id":"store-demo","description":"Demo","color":0}],
         "products":[{"id":"prod-001","productName":"Milk","unitOfMeasurement":"kg"}],
         "prices":[{"productId":"prod-001","storeId":"store-demo","value":10.5}],
         "shoppingLists":[{"id":"list-1","title":"Weekly"}],
         "listItems":[{"id":7,"shoppingListId":"list-1","productId":"prod-001","storeId":"store-demo","quantity":2.0,"done":false,"pinned":false}]}
    """.trimIndent()

    @Test
    fun `parse accepts valid backup`() {
        assertTrue(manager.parse(validJson).isSuccess)
    }

    @Test
    fun `parse rejects malformed json`() {
        assertTrue(manager.parse("{not json").isFailure)
    }

    @Test
    fun `parse rejects unsupported version`() {
        val json = validJson.replaceFirst("\"version\":1", "\"version\":2")
        assertTrue(manager.parse(json).isFailure)
    }

    @Test
    fun `parse rejects item with unknown product`() {
        val json = validJson.replaceFirst("\"productId\":\"prod-001\",\"storeId\":\"store-demo\",\"value\":10.5", "\"productId\":\"prod-001\",\"storeId\":\"store-demo\",\"value\":10.5")
            .replaceFirst("\"shoppingListId\":\"list-1\",\"productId\":\"prod-001\",\"storeId\":\"store-demo\",\"quantity\":2.0", "\"shoppingListId\":\"list-1\",\"productId\":\"missing-prod\",\"storeId\":\"store-demo\",\"quantity\":2.0")
        assertTrue(manager.parse(json).isFailure)
    }

    @Test
    fun `parse rejects price with unknown store`() {
        val json = validJson.replaceFirst("\"storeId\":\"store-demo\",\"value\":10.5", "\"storeId\":\"missing-store\",\"value\":10.5")
        assertTrue(manager.parse(json).isFailure)
    }

    @Test
    fun `parse rejects unknown unit of measurement`() {
        val json = validJson.replaceFirst("\"unitOfMeasurement\":\"kg\"", "\"unitOfMeasurement\":\"gallon\"")
        assertTrue(manager.parse(json).isFailure)
    }

    @Test
    fun `parse rejects duplicate list item ids`() {
        val json = validJson.replace(
            "\"listItems\":[{\"id\":7,\"shoppingListId\":\"list-1\",\"productId\":\"prod-001\",\"storeId\":\"store-demo\",\"quantity\":2.0,\"done\":false,\"pinned\":false}]",
            "\"listItems\":[{\"id\":7,\"shoppingListId\":\"list-1\",\"productId\":\"prod-001\",\"storeId\":\"store-demo\",\"quantity\":2.0,\"done\":false,\"pinned\":false},{\"id\":7,\"shoppingListId\":\"list-1\",\"productId\":\"prod-001\",\"storeId\":\"store-demo\",\"quantity\":3.0,\"done\":false,\"pinned\":false}]"
        )
        assertTrue(manager.parse(json).isFailure)
    }

    @Test
    fun `import replaces all data`() = runTest {
        seeder.seedIfEmpty()
        assertEquals(2, db.storeDao().getAll().first().size)
        assertEquals(10, db.productDao().getAll().first().size)

        val backup = manager.parse(validJson).getOrThrow()
        manager.importData(backup)

        assertEquals(1, db.storeDao().getAll().first().size)
        assertEquals(1, db.productDao().getAll().first().size)
        assertEquals(1, db.priceDao().getAll().first().size)
        assertEquals(1, db.listDao().getAll().first().size)
        val list = db.listDao().getById("list-1")!!
        assertEquals(1, list.items.size)
        assertEquals(7L, list.items.first().id)
    }

    @Test
    fun `export produces json containing seeded data`() = runTest {
        seeder.seedIfEmpty()
        val json = manager.exportToString()
        val parsed = manager.parse(json).getOrThrow()
        assertEquals(2, parsed.stores.size)
        assertEquals(10, parsed.products.size)
        assertEquals(3, parsed.shoppingLists.size)
    }
}
