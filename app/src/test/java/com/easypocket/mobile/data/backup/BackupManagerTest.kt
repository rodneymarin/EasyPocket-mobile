package com.easypocket.mobile.data.backup

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.easypocket.mobile.data.local.CategoryEntity
import com.easypocket.mobile.data.local.EasyPocketDatabase
import com.easypocket.mobile.data.local.ProductEntity
import com.easypocket.mobile.data.local.PurchaseHistoryEntity
import com.easypocket.mobile.data.local.PurchaseHistoryItemEntity
import com.easypocket.mobile.data.local.ShoppingListEntity
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

    private suspend fun insertHistory(id: String, title: String = "Compras") {
        db.purchaseHistoryDao().insertHistory(
            PurchaseHistoryEntity(id, title, "🛒", 1_756_400_000_000, 20.0, 2)
        )
        db.purchaseHistoryDao().insertItems(
            listOf(
                PurchaseHistoryItemEntity(
                    historyId = id, productName = "Milk", storeName = "Store 1",
                    quantity = 2.0, unitPrice = 10.0, totalPrice = 20.0,
                )
            )
        )
    }

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

    @Test
    fun `export and import preserve history`() = runTest {
        insertHistory("h1", "Compras semanales")

        val json = manager.exportToString()
        db.clearAllTables()
        manager.importData(manager.parse(json).getOrThrow())

        val records = db.purchaseHistoryDao().observeAll().first()
        assertEquals(1, records.size)
        val record = records.first()
        assertEquals("h1", record.record.id)
        assertEquals("Compras semanales", record.record.listTitle)
        assertEquals("🛒", record.record.listIcon)
        assertEquals(1_756_400_000_000, record.record.date)
        assertEquals(20.0, record.record.totalAmount, 0.001)
        assertEquals(2, record.record.itemCount)
        assertEquals(1, record.items.size)
        assertEquals("Milk", record.items.first().productName)
        assertEquals("Store 1", record.items.first().storeName)
    }

    @Test
    fun `import old backup without history replaces it with empty`() = runTest {
        insertHistory("h1")
        val oldBackupJson = """
            {"version":1,"exportedAt":"2026-01-01T00:00:00Z","stores":[],"products":[],"prices":[],
             "shoppingLists":[],"listItems":[]}
        """.trimIndent()

        manager.importData(manager.parse(oldBackupJson).getOrThrow())

        assertTrue(db.purchaseHistoryDao().observeAll().first().isEmpty())
    }

    @Test
    fun `parse rejects history item referencing unknown history`() {
        val badBackupJson = """
            {"version":1,"exportedAt":"2026-01-01T00:00:00Z","stores":[],"products":[],"prices":[],
             "shoppingLists":[],"listItems":[],
             "purchaseHistory":[],
             "purchaseHistoryItems":[{"historyId":"nope","productName":"Milk","quantity":1.0,"unitPrice":1.0,"totalPrice":1.0}]}
        """.trimIndent()

        assertTrue(manager.parse(badBackupJson).isFailure)
    }

    @Test
    fun `export and import preserve categories and category codes`() = runTest {
        db.categoryDao().insert(CategoryEntity("ABC123", "Abarrotes"))
        db.productDao().insert(ProductEntity("p1", "Milk", "lt", categoryId = "ABC123"))
        db.purchaseHistoryDao().insertHistory(PurchaseHistoryEntity("h1", "Compras", "🛒", 1L, 10.0, 1))
        db.purchaseHistoryDao().insertItems(
            listOf(
                PurchaseHistoryItemEntity(
                    historyId = "h1", productName = "Milk", storeName = null,
                    quantity = 1.0, unitPrice = 10.0, totalPrice = 10.0, categoryCode = "ABC123",
                )
            )
        )

        val json = manager.exportToString()
        db.clearAllTables()
        manager.importData(manager.parse(json).getOrThrow())

        assertEquals("Abarrotes", db.categoryDao().getById("ABC123")!!.name)
        assertEquals("ABC123", db.productDao().getAll().first().first { it.id == "p1" }.categoryId)
        val items = db.purchaseHistoryDao().observeAll().first().first().items
        assertEquals("ABC123", items.first().categoryCode)
    }

    @Test
    fun `import keeps history category code that does not exist in the master`() = runTest {
        val backupJson = """
            {"version":1,"exportedAt":"2026-01-01T00:00:00Z","stores":[],"products":[],"prices":[],
             "shoppingLists":[],"listItems":[],
             "purchaseHistory":[{"id":"h1","listTitle":"Compras","listIcon":"$","date":1,"totalAmount":10.0,"itemCount":1}],
             "purchaseHistoryItems":[{"historyId":"h1","productName":"Milk","quantity":1.0,"unitPrice":10.0,"totalPrice":10.0,"categoryCode":"ZZZ999"}]}
        """.trimIndent()

        manager.importData(manager.parse(backupJson).getOrThrow())

        val items = db.purchaseHistoryDao().observeAll().first().first().items
        assertEquals("ZZZ999", items.first().categoryCode)
        assertTrue(db.categoryDao().getAll().first().isEmpty())
    }
}
