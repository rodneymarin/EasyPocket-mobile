package com.easypocket.mobile.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        StoreEntity::class, CategoryEntity::class, ProductEntity::class, PriceEntity::class,
        ShoppingListEntity::class, ShoppingListItemEntity::class,
        PurchaseHistoryEntity::class, PurchaseHistoryItemEntity::class,
    ],
    version = 4,
    exportSchema = false,
)
abstract class EasyPocketDatabase : RoomDatabase() {
    abstract fun storeDao(): StoreDao
    abstract fun categoryDao(): CategoryDao
    abstract fun productDao(): ProductDao
    abstract fun priceDao(): PriceDao
    abstract fun listDao(): ShoppingListDao
    abstract fun purchaseHistoryDao(): PurchaseHistoryDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE shopping_lists ADD COLUMN icon TEXT NOT NULL DEFAULT '$'")
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `purchase_history` (" +
                        "`id` TEXT NOT NULL PRIMARY KEY, " +
                        "`list_title` TEXT NOT NULL, " +
                        "`list_icon` TEXT NOT NULL, " +
                        "`date` INTEGER NOT NULL, " +
                        "`total_amount` REAL NOT NULL, " +
                        "`item_count` INTEGER NOT NULL)"
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `purchase_history_items` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`history_id` TEXT NOT NULL, " +
                        "`product_name` TEXT NOT NULL, " +
                        "`store_name` TEXT, " +
                        "`quantity` REAL NOT NULL, " +
                        "`unit_price` REAL NOT NULL, " +
                        "`total_price` REAL NOT NULL, " +
                        "FOREIGN KEY(`history_id`) REFERENCES `purchase_history`(`id`) " +
                        "ON UPDATE NO ACTION ON DELETE CASCADE)"
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_purchase_history_items_history_id` " +
                        "ON `purchase_history_items` (`history_id`)"
                )
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `categories` (" +
                        "`id` TEXT NOT NULL PRIMARY KEY, " +
                        "`name` TEXT NOT NULL)"
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_categories_name` ON `categories` (`name`)")
                db.execSQL("ALTER TABLE `products` ADD COLUMN `category_id` TEXT")
                db.execSQL("ALTER TABLE `purchase_history_items` ADD COLUMN `category_code` TEXT")
            }
        }
    }
}
