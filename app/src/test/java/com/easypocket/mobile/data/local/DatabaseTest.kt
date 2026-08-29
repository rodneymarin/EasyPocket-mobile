package com.easypocket.mobile.data.local

import android.content.Context
import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class DatabaseTest {
    private lateinit var db: EasyPocketDatabase

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, EasyPocketDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun tearDown() { db.close() }

    @Test
    fun `migration 1 to 2 adds icon with default dollar for existing lists`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val helper = FrameworkSQLiteOpenHelperFactory().create(
            SupportSQLiteOpenHelper.Configuration.builder(context)
                .name(null)
                .callback(object : SupportSQLiteOpenHelper.Callback(1) {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        db.execSQL("CREATE TABLE shopping_lists (id TEXT NOT NULL PRIMARY KEY, title TEXT NOT NULL)")
                        db.execSQL("INSERT INTO shopping_lists (id, title) VALUES ('l1', 'Compras')")
                    }

                    override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {}
                })
                .build()
        )
        val db = helper.writableDatabase

        EasyPocketDatabase.MIGRATION_1_2.migrate(db)

        db.query("SELECT icon FROM shopping_lists WHERE id = 'l1'").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("$", cursor.getString(0))
        }
        helper.close()
    }

    @Test
    fun `delete store cascades prices and nulls item storeId`() = runTest {
        db.storeDao().insert(StoreEntity("s1", "Store 1", 0))
        db.productDao().insert(ProductEntity("p1", "Milk", "lt"))
        db.priceDao().insertAll(listOf(PriceEntity("p1", "s1", 10.0)))
        db.listDao().insert(ShoppingListEntity("l1", "Lista"))
        db.listDao().insertItem(ShoppingListItemEntity(shoppingListId = "l1", productId = "p1", storeId = "s1", quantity = 1.0))

        db.storeDao().deleteByIds(listOf("s1"))

        assertTrue(db.priceDao().getAll().first().isEmpty())
        val item = db.listDao().getById("l1")!!.items.first()
        assertNull(item.storeId)
    }

    @Test
    fun `delete product cascades prices and items`() = runTest {
        db.storeDao().insert(StoreEntity("s1", "Store 1", 0))
        db.productDao().insert(ProductEntity("p1", "Milk", "lt"))
        db.priceDao().insertAll(listOf(PriceEntity("p1", "s1", 10.0)))
        db.listDao().insert(ShoppingListEntity("l1", "Lista"))
        db.listDao().insertItem(ShoppingListItemEntity(shoppingListId = "l1", productId = "p1", storeId = null, quantity = 1.0))

        db.productDao().deleteByIds(listOf("p1"))

        assertTrue(db.priceDao().getAll().first().isEmpty())
        assertTrue(db.listDao().getById("l1")!!.items.isEmpty())
    }

    @Test
    fun `delete list cascades items`() = runTest {
        db.productDao().insert(ProductEntity("p1", "Milk", "lt"))
        db.listDao().insert(ShoppingListEntity("l1", "Lista"))
        db.listDao().insertItem(ShoppingListItemEntity(shoppingListId = "l1", productId = "p1", storeId = null, quantity = 1.0))

        db.listDao().deleteById("l1")

        assertTrue(db.listDao().getAll().first().isEmpty())
    }

    @Test
    fun `getByName is case insensitive`() = runTest {
        db.productDao().insert(ProductEntity("p1", "Leche Entera", "lt"))
        assertEquals("p1", db.productDao().getByName("leche entera")!!.id)
        assertNull(db.productDao().getByName("leche"))
    }

    @Test
    fun `moveItems relocates items to another list`() = runTest {
        db.productDao().insert(ProductEntity("p1", "Milk", "lt"))
        db.listDao().insert(ShoppingListEntity("l1", "A"))
        db.listDao().insert(ShoppingListEntity("l2", "B"))
        val id = db.listDao().insertItem(ShoppingListItemEntity(shoppingListId = "l1", productId = "p1", storeId = null, quantity = 1.0))

        db.listDao().moveItems(listOf(id), "l2")

        assertEquals(0, db.listDao().getById("l1")!!.items.size)
        assertEquals(1, db.listDao().getById("l2")!!.items.size)
    }

    @Test
    fun `removeCompleted deletes only done items`() = runTest {
        db.productDao().insert(ProductEntity("p1", "Milk", "lt"))
        db.listDao().insert(ShoppingListEntity("l1", "A"))
        db.listDao().insertItem(ShoppingListItemEntity(shoppingListId = "l1", productId = "p1", storeId = null, quantity = 1.0, done = true))
        db.listDao().insertItem(ShoppingListItemEntity(shoppingListId = "l1", productId = "p1", storeId = null, quantity = 2.0, done = false))

        db.listDao().removeCompleted("l1")

        assertEquals(1, db.listDao().getById("l1")!!.items.size)
    }

    @Test
    fun `migration 2 to 3 creates history tables`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val helper = FrameworkSQLiteOpenHelperFactory().create(
            SupportSQLiteOpenHelper.Configuration.builder(context)
                .name(null)
                .callback(object : SupportSQLiteOpenHelper.Callback(2) {
                    override fun onCreate(db: SupportSQLiteDatabase) {}
                    override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {}
                })
                .build()
        )
        val db = helper.writableDatabase

        EasyPocketDatabase.MIGRATION_2_3.migrate(db)

        db.execSQL(
            "INSERT INTO purchase_history (id, list_title, list_icon, date, total_amount, item_count) " +
                "VALUES ('h1', 'Lista', '$', 0, 20.0, 2)"
        )
        db.execSQL(
            "INSERT INTO purchase_history_items (history_id, product_name, store_name, quantity, unit_price, total_price) " +
                "VALUES ('h1', 'Milk', 'Store 1', 2.0, 10.0, 20.0)"
        )
        db.query("SELECT list_title FROM purchase_history WHERE id = 'h1'").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("Lista", cursor.getString(0))
        }
        helper.close()
    }

    @Test
    fun `migration 3 to 4 creates categories and adds category columns`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val helper = FrameworkSQLiteOpenHelperFactory().create(
            SupportSQLiteOpenHelper.Configuration.builder(context)
                .name(null)
                .callback(object : SupportSQLiteOpenHelper.Callback(3) {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        db.execSQL("CREATE TABLE products (id TEXT NOT NULL PRIMARY KEY, product_name TEXT NOT NULL, unit_of_measurement TEXT NOT NULL)")
                        db.execSQL(
                            "CREATE TABLE purchase_history_items (" +
                                "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                                "history_id TEXT NOT NULL, product_name TEXT NOT NULL, store_name TEXT, " +
                                "quantity REAL NOT NULL, unit_price REAL NOT NULL, total_price REAL NOT NULL)"
                        )
                        db.execSQL("INSERT INTO products (id, product_name, unit_of_measurement) VALUES ('p1', 'Milk', 'lt')")
                        db.execSQL(
                            "INSERT INTO purchase_history_items (history_id, product_name, store_name, quantity, unit_price, total_price) " +
                                "VALUES ('h1', 'Milk', null, 1.0, 2.0, 2.0)"
                        )
                    }

                    override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {}
                })
                .build()
        )
        val db = helper.writableDatabase

        EasyPocketDatabase.MIGRATION_3_4.migrate(db)

        db.execSQL("INSERT INTO categories (id, name) VALUES ('ABC123', 'Abarrotes')")
        db.query("SELECT product_name, category_id FROM products WHERE id = 'p1'").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("Milk", cursor.getString(0))
            assertNull(cursor.getString(1))
        }
        db.query("SELECT category_code FROM purchase_history_items LIMIT 1").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertNull(cursor.getString(0))
        }
        helper.close()
    }

    @Test
    fun `clearCategory nulls category_id only for matching products`() = runTest {
        db.productDao().insert(ProductEntity("p1", "Milk", "lt", categoryId = "ABC123"))
        db.productDao().insert(ProductEntity("p2", "Bread", "u", categoryId = "XYZ789"))
        db.productDao().insert(ProductEntity("p3", "Rice", "u", categoryId = null))

        db.productDao().clearCategory(listOf("ABC123"))

        val products = db.productDao().getAll().first().associateBy { it.id }
        assertNull(products.getValue("p1").categoryId)
        assertEquals("XYZ789", products.getValue("p2").categoryId)
        assertNull(products.getValue("p3").categoryId)
    }

    @Test
    fun `category dao crud and case insensitive lookup`() = runTest {
        db.categoryDao().insert(CategoryEntity("ABC123", "Abarrotes"))

        assertEquals("ABC123", db.categoryDao().getByName("abarrotes")!!.id)
        assertEquals("ABC123", db.categoryDao().getById("ABC123")!!.id)
        assertNull(db.categoryDao().getByName("No existe"))
        assertNull(db.categoryDao().getById("NOPE99"))

        db.categoryDao().update(CategoryEntity("ABC123", "Despensa"))
        assertEquals("Despensa", db.categoryDao().getById("ABC123")!!.name)

        db.categoryDao().deleteByIds(listOf("ABC123"))
        assertTrue(db.categoryDao().getAll().first().isEmpty())
    }

    @Test
    fun `migration 4 to 5 backfills unique item uids for existing history items`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val helper = FrameworkSQLiteOpenHelperFactory().create(
            SupportSQLiteOpenHelper.Configuration.builder(context)
                .name(null)
                .callback(object : SupportSQLiteOpenHelper.Callback(4) {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        db.execSQL("CREATE TABLE purchase_history (id TEXT NOT NULL PRIMARY KEY)")
                        db.execSQL(
                            "CREATE TABLE purchase_history_items (" +
                                "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                                "history_id TEXT NOT NULL, product_name TEXT NOT NULL, store_name TEXT, " +
                                "quantity REAL NOT NULL, unit_price REAL NOT NULL, total_price REAL NOT NULL, " +
                                "category_code TEXT)"
                        )
                        db.execSQL("INSERT INTO purchase_history (id) VALUES ('h1')")
                        db.execSQL(
                            "INSERT INTO purchase_history_items (history_id, product_name, store_name, quantity, unit_price, total_price, category_code) " +
                                "VALUES ('h1', 'Milk', null, 1.0, 2.0, 2.0, 'ABC123')"
                        )
                        db.execSQL(
                            "INSERT INTO purchase_history_items (history_id, product_name, store_name, quantity, unit_price, total_price, category_code) " +
                                "VALUES ('h1', 'Bread', null, 1.0, 1.0, 1.0, null)"
                        )
                    }

                    override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {}
                })
                .build()
        )
        val db = helper.writableDatabase

        EasyPocketDatabase.MIGRATION_4_5.migrate(db)

        val uids = mutableListOf<String>()
        db.query("SELECT product_name, item_uid, category_code FROM purchase_history_items ORDER BY id").use { cursor ->
            assertTrue(cursor.moveToFirst())
            do {
                assertEquals(32, cursor.getString(1).length)
                uids.add(cursor.getString(1))
            } while (cursor.moveToNext())
        }
        assertEquals(2, uids.size)
        assertEquals(2, uids.toSet().size)
        helper.close()
    }

    @Test
    fun `delete history record cascades items`() = runTest {
        db.purchaseHistoryDao().insertHistory(
            PurchaseHistoryEntity("h1", "Lista", "$", 0L, 20.0, 1)
        )
        db.purchaseHistoryDao().insertItems(
            listOf(
                PurchaseHistoryItemEntity(
                    historyId = "h1", productName = "Milk", storeName = "Store 1",
                    quantity = 2.0, unitPrice = 10.0, totalPrice = 20.0,
                    itemUid = "uid-h1-1",
                )
            )
        )

        db.openHelper.writableDatabase.execSQL("DELETE FROM purchase_history WHERE id = 'h1'")

        db.openHelper.writableDatabase.query("SELECT COUNT(*) FROM purchase_history_items").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals(0, cursor.getInt(0))
        }
        assertTrue(db.purchaseHistoryDao().observeAll().first().isEmpty())
    }
}
