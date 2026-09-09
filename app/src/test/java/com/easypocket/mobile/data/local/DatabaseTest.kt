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
import org.junit.Assert.assertFalse
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
    fun `clearCategory nulls category_id only for matching list items`() = runTest {
        db.productDao().insert(ProductEntity("p1", "Milk", "lt"))
        db.listDao().insert(ShoppingListEntity("l1", "A"))
        val id1 = db.listDao().insertItem(
            ShoppingListItemEntity(shoppingListId = "l1", productId = "p1", storeId = null, categoryId = "ABC123", quantity = 1.0)
        )
        val id2 = db.listDao().insertItem(
            ShoppingListItemEntity(shoppingListId = "l1", productId = "p1", storeId = null, categoryId = "XYZ789", quantity = 1.0)
        )

        db.listDao().clearCategory(listOf("ABC123"))

        val items = db.listDao().getById("l1")!!.items.associateBy { it.id }
        assertNull(items.getValue(id1).categoryId)
        assertEquals("XYZ789", items.getValue(id2).categoryId)
    }

    @Test
    fun `last category dao upserts replaces and deletes`() = runTest {
        db.productLastCategoryDao().upsert(ProductLastCategoryEntity("p1", "ABC123"))
        db.productLastCategoryDao().upsert(ProductLastCategoryEntity("p1", "XYZ789"))
        db.productLastCategoryDao().upsert(ProductLastCategoryEntity("p2", "ABC123"))

        assertEquals("XYZ789", db.productLastCategoryDao().getByProductId("p1")!!.categoryId)
        assertEquals("ABC123", db.productLastCategoryDao().getByProductId("p2")!!.categoryId)
        assertNull(db.productLastCategoryDao().getByProductId("p3"))

        db.productLastCategoryDao().deleteForCategories(listOf("ABC123"))
        assertNull(db.productLastCategoryDao().getByProductId("p2"))
        assertEquals("XYZ789", db.productLastCategoryDao().getByProductId("p1")!!.categoryId)

        db.productLastCategoryDao().deleteForProduct("p1")
        assertNull(db.productLastCategoryDao().getByProductId("p1"))
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
    fun `migration 5 to 6 adds icon with default dollar for existing categories`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val helper = FrameworkSQLiteOpenHelperFactory().create(
            SupportSQLiteOpenHelper.Configuration.builder(context)
                .name(null)
                .callback(object : SupportSQLiteOpenHelper.Callback(5) {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        db.execSQL("CREATE TABLE categories (id TEXT NOT NULL PRIMARY KEY, name TEXT NOT NULL)")
                        db.execSQL("CREATE INDEX index_categories_name ON categories (name)")
                        db.execSQL("INSERT INTO categories (id, name) VALUES ('ABC123', 'Abarrotes')")
                    }

                    override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {}
                })
                .build()
        )
        val db = helper.writableDatabase

        EasyPocketDatabase.MIGRATION_5_6.migrate(db)

        db.query("SELECT icon FROM categories WHERE id = 'ABC123'").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("$", cursor.getString(0))
        }
        helper.close()
    }

    @Test
    fun `migration 6 to 7 moves product categories to last categories and adds item category`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val helper = FrameworkSQLiteOpenHelperFactory().create(
            SupportSQLiteOpenHelper.Configuration.builder(context)
                .name(null)
                .callback(object : SupportSQLiteOpenHelper.Callback(6) {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        db.execSQL("CREATE TABLE stores (id TEXT NOT NULL PRIMARY KEY, description TEXT NOT NULL, color INTEGER NOT NULL)")
                        db.execSQL("CREATE TABLE categories (id TEXT NOT NULL PRIMARY KEY, name TEXT NOT NULL, icon TEXT NOT NULL)")
                        db.execSQL("CREATE INDEX index_categories_name ON categories (name)")
                        db.execSQL(
                            "CREATE TABLE products (" +
                                "id TEXT NOT NULL PRIMARY KEY, " +
                                "product_name TEXT NOT NULL, " +
                                "unit_of_measurement TEXT NOT NULL, " +
                                "category_id TEXT)"
                        )
                        db.execSQL("CREATE INDEX index_products_product_name ON products (product_name)")
                        db.execSQL(
                            "CREATE TABLE product_prices (" +
                                "product_id TEXT NOT NULL, store_id TEXT NOT NULL, value REAL NOT NULL, " +
                                "PRIMARY KEY(product_id, store_id), " +
                                "FOREIGN KEY(product_id) REFERENCES products(id) ON UPDATE NO ACTION ON DELETE CASCADE, " +
                                "FOREIGN KEY(store_id) REFERENCES stores(id) ON UPDATE NO ACTION ON DELETE CASCADE)"
                        )
                        db.execSQL("CREATE TABLE shopping_lists (id TEXT NOT NULL PRIMARY KEY, title TEXT NOT NULL, icon TEXT NOT NULL)")
                        db.execSQL(
                            "CREATE TABLE shopping_list_items (" +
                                "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                                "shopping_list_id TEXT NOT NULL, product_id TEXT NOT NULL, store_id TEXT, " +
                                "quantity REAL NOT NULL, done INTEGER NOT NULL, pinned INTEGER NOT NULL, " +
                                "FOREIGN KEY(shopping_list_id) REFERENCES shopping_lists(id) ON UPDATE NO ACTION ON DELETE CASCADE, " +
                                "FOREIGN KEY(product_id) REFERENCES products(id) ON UPDATE NO ACTION ON DELETE CASCADE, " +
                                "FOREIGN KEY(store_id) REFERENCES stores(id) ON UPDATE NO ACTION ON DELETE SET NULL)"
                        )
                        db.execSQL("CREATE INDEX index_shopping_list_items_shopping_list_id ON shopping_list_items (shopping_list_id)")
                        db.execSQL("CREATE INDEX index_shopping_list_items_product_id ON shopping_list_items (product_id)")
                        db.execSQL("CREATE INDEX index_shopping_list_items_store_id ON shopping_list_items (store_id)")

                        db.execSQL("INSERT INTO stores (id, description, color) VALUES ('s1', 'Store 1', 0)")
                        db.execSQL("INSERT INTO categories (id, name, icon) VALUES ('ABC123', 'Abarrotes', 'X')")
                        db.execSQL("INSERT INTO products (id, product_name, unit_of_measurement, category_id) VALUES ('p1', 'Milk', 'lt', 'ABC123')")
                        db.execSQL("INSERT INTO products (id, product_name, unit_of_measurement, category_id) VALUES ('p2', 'Bread', 'u', 'XYZ789')")
                        db.execSQL("INSERT INTO products (id, product_name, unit_of_measurement, category_id) VALUES ('p3', 'Rice', 'u', NULL)")
                        db.execSQL("INSERT INTO product_prices (product_id, store_id, value) VALUES ('p1', 's1', 10.0)")
                        db.execSQL("INSERT INTO shopping_lists (id, title, icon) VALUES ('l1', 'Lista', '$')")
                        db.execSQL(
                            "INSERT INTO shopping_list_items (id, shopping_list_id, product_id, store_id, quantity, done, pinned) " +
                                "VALUES (1, 'l1', 'p1', 's1', 2.0, 0, 0)"
                        )
                    }

                    override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {}
                })
                .build()
        )
        val db = helper.writableDatabase

        EasyPocketDatabase.MIGRATION_6_7.migrate(db)

        // products loses the category column but keeps its rows
        db.query("SELECT product_name, unit_of_measurement FROM products WHERE id = 'p1'").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("Milk", cursor.getString(0))
        }
        db.query("SELECT COUNT(*) FROM products").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals(3, cursor.getInt(0))
        }

        // existing product categories become the remembered last category
        db.query("SELECT product_id, category_id FROM product_last_categories ORDER BY product_id").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("p1", cursor.getString(0))
            assertEquals("ABC123", cursor.getString(1))
            assertTrue(cursor.moveToNext())
            assertEquals("p2", cursor.getString(0))
            assertEquals("XYZ789", cursor.getString(1))
            assertFalse(cursor.moveToNext())
        }

        // prices are preserved
        db.query("SELECT value FROM product_prices WHERE product_id = 'p1'").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals(10.0, cursor.getDouble(0), 0.001)
        }

        // list items keep their data and start without a category
        db.query("SELECT product_id, store_id, category_id, quantity, done, pinned FROM shopping_list_items WHERE id = 1").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("p1", cursor.getString(0))
            assertEquals("s1", cursor.getString(1))
            assertNull(cursor.getString(2))
            assertEquals(2.0, cursor.getDouble(3), 0.001)
            assertEquals(0, cursor.getInt(4))
            assertEquals(0, cursor.getInt(5))
        }
        helper.close()
    }

    @Test
    fun `migration 7 to 8 adds nullable category to shopping lists`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val helper = FrameworkSQLiteOpenHelperFactory().create(
            SupportSQLiteOpenHelper.Configuration.builder(context)
                .name(null)
                .callback(object : SupportSQLiteOpenHelper.Callback(7) {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        db.execSQL("CREATE TABLE shopping_lists (id TEXT NOT NULL PRIMARY KEY, title TEXT NOT NULL, icon TEXT NOT NULL)")
                        db.execSQL("INSERT INTO shopping_lists (id, title, icon) VALUES ('l1', 'Lista', '$')")
                        db.execSQL("INSERT INTO shopping_lists (id, title, icon) VALUES ('l2', 'Otra', '$')")
                    }

                    override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {}
                })
                .build()
        )
        val db = helper.writableDatabase

        EasyPocketDatabase.MIGRATION_7_8.migrate(db)

        // existing lists keep their data and start without a category
        db.query("SELECT id, title, category_id FROM shopping_lists ORDER BY id").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("l1", cursor.getString(0))
            assertEquals("Lista", cursor.getString(1))
            assertNull(cursor.getString(2))
            assertTrue(cursor.moveToNext())
            assertEquals("l2", cursor.getString(0))
            assertNull(cursor.getString(2))
            assertFalse(cursor.moveToNext())
        }

        // the new column is usable
        db.execSQL("UPDATE shopping_lists SET category_id = 'ABC123' WHERE id = 'l2'")
        db.query("SELECT category_id FROM shopping_lists WHERE id = 'l2'").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("ABC123", cursor.getString(0))
        }

        db.query(
            "SELECT name FROM sqlite_master WHERE type = 'index' AND name = 'index_shopping_lists_category_id'"
        ).use { cursor ->
            assertTrue(cursor.moveToFirst())
        }
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
