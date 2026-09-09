package com.easypocket.mobile.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        StoreEntity::class, CategoryEntity::class, ProductEntity::class, ProductLastCategoryEntity::class,
        PriceEntity::class, ShoppingListEntity::class, ShoppingListItemEntity::class,
        PurchaseHistoryEntity::class, PurchaseHistoryItemEntity::class,
    ],
    version = 8,
    exportSchema = false,
)
abstract class EasyPocketDatabase : RoomDatabase() {
    abstract fun storeDao(): StoreDao
    abstract fun categoryDao(): CategoryDao
    abstract fun productDao(): ProductDao
    abstract fun productLastCategoryDao(): ProductLastCategoryDao
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

    val MIGRATION_5_6 = object : Migration(5, 6) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE categories ADD COLUMN icon TEXT NOT NULL DEFAULT '$'")
        }
    }

    val MIGRATION_4_5 = object : Migration(4, 5) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS `purchase_history_items_new` (" +
                    "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                    "`history_id` TEXT NOT NULL, " +
                    "`product_name` TEXT NOT NULL, " +
                    "`store_name` TEXT, " +
                    "`quantity` REAL NOT NULL, " +
                    "`unit_price` REAL NOT NULL, " +
                    "`total_price` REAL NOT NULL, " +
                    "`category_code` TEXT, " +
                    "`item_uid` TEXT NOT NULL, " +
                    "FOREIGN KEY(`history_id`) REFERENCES `purchase_history`(`id`) " +
                    "ON UPDATE NO ACTION ON DELETE CASCADE)"
            )
            db.execSQL(
                "INSERT INTO `purchase_history_items_new` " +
                    "(`id`, `history_id`, `product_name`, `store_name`, `quantity`, `unit_price`, `total_price`, `category_code`, `item_uid`) " +
                    "SELECT `id`, `history_id`, `product_name`, `store_name`, `quantity`, `unit_price`, `total_price`, `category_code`, " +
                    "lower(hex(randomblob(16))) FROM `purchase_history_items`"
            )
            db.execSQL("DROP TABLE `purchase_history_items`")
            db.execSQL("ALTER TABLE `purchase_history_items_new` RENAME TO `purchase_history_items`")
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_purchase_history_items_history_id` " +
                    "ON `purchase_history_items` (`history_id`)"
            )
            db.execSQL(
                "CREATE UNIQUE INDEX IF NOT EXISTS `index_purchase_history_items_item_uid` " +
                    "ON `purchase_history_items` (`item_uid`)"
            )
        }
    }

    // - Creates `product_last_categories` and seeds it from the existing
    //   `products.category_id` values, so every product keeps its last used
    //   category as pre-selection for future list items.
    // - Removes `products.category_id` (categories now live on list items),
    //   which requires recreating `products` and, because of enforced foreign
    //   keys, its child tables `product_prices` and `shopping_list_items`.
    //   Data is staged in temporary tables WITHOUT foreign keys, `products` is
    //   dropped and rebuilt, and then the final child tables (with foreign
    //   keys) are rebuilt from the staged data.
    // - Adds `shopping_list_items.category_id` (nullable, null for existing rows).
    val MIGRATION_6_7 = object : Migration(6, 7) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS `product_last_categories` (" +
                    "`product_id` TEXT NOT NULL, " +
                    "`category_id` TEXT NOT NULL, " +
                    "PRIMARY KEY(`product_id`))"
            )
            db.execSQL(
                "INSERT INTO `product_last_categories` (`product_id`, `category_id`) " +
                    "SELECT `id`, `category_id` FROM `products` WHERE `category_id` IS NOT NULL"
            )

            db.execSQL(
                "CREATE TABLE IF NOT EXISTS `products_tmp` (" +
                    "`id` TEXT NOT NULL, " +
                    "`product_name` TEXT NOT NULL, " +
                    "`unit_of_measurement` TEXT NOT NULL, " +
                    "PRIMARY KEY(`id`))"
            )
            db.execSQL(
                "INSERT INTO `products_tmp` (`id`, `product_name`, `unit_of_measurement`) " +
                    "SELECT `id`, `product_name`, `unit_of_measurement` FROM `products`"
            )
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS `product_prices_tmp` (" +
                    "`product_id` TEXT NOT NULL, " +
                    "`store_id` TEXT NOT NULL, " +
                    "`value` REAL NOT NULL, " +
                    "PRIMARY KEY(`product_id`, `store_id`))"
            )
            db.execSQL(
                "INSERT INTO `product_prices_tmp` (`product_id`, `store_id`, `value`) " +
                    "SELECT `product_id`, `store_id`, `value` FROM `product_prices`"
            )
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS `shopping_list_items_tmp` (" +
                    "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                    "`shopping_list_id` TEXT NOT NULL, " +
                    "`product_id` TEXT NOT NULL, " +
                    "`store_id` TEXT, " +
                    "`category_id` TEXT, " +
                    "`quantity` REAL NOT NULL, " +
                    "`done` INTEGER NOT NULL, " +
                    "`pinned` INTEGER NOT NULL)"
            )
            db.execSQL(
                "INSERT INTO `shopping_list_items_tmp` " +
                    "(`id`, `shopping_list_id`, `product_id`, `store_id`, `category_id`, `quantity`, `done`, `pinned`) " +
                    "SELECT `id`, `shopping_list_id`, `product_id`, `store_id`, NULL, `quantity`, `done`, `pinned` FROM `shopping_list_items`"
            )

            // Safe now: nothing left referencing `products` has foreign keys.
            db.execSQL("DROP TABLE `shopping_list_items`")
            db.execSQL("DROP TABLE `product_prices`")
            db.execSQL("DROP TABLE `products`")

            db.execSQL("ALTER TABLE `products_tmp` RENAME TO `products`")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_products_product_name` ON `products` (`product_name`)")

            db.execSQL(
                "CREATE TABLE IF NOT EXISTS `product_prices` (" +
                    "`product_id` TEXT NOT NULL, " +
                    "`store_id` TEXT NOT NULL, " +
                    "`value` REAL NOT NULL, " +
                    "PRIMARY KEY(`product_id`, `store_id`), " +
                    "FOREIGN KEY(`product_id`) REFERENCES `products`(`id`) " +
                    "ON UPDATE NO ACTION ON DELETE CASCADE, " +
                    "FOREIGN KEY(`store_id`) REFERENCES `stores`(`id`) " +
                    "ON UPDATE NO ACTION ON DELETE CASCADE)"
            )
            db.execSQL(
                "INSERT INTO `product_prices` (`product_id`, `store_id`, `value`) " +
                    "SELECT `product_id`, `store_id`, `value` FROM `product_prices_tmp`"
            )
            db.execSQL("DROP TABLE `product_prices_tmp`")

            db.execSQL(
                "CREATE TABLE IF NOT EXISTS `shopping_list_items` (" +
                    "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                    "`shopping_list_id` TEXT NOT NULL, " +
                    "`product_id` TEXT NOT NULL, " +
                    "`store_id` TEXT, " +
                    "`category_id` TEXT, " +
                    "`quantity` REAL NOT NULL, " +
                    "`done` INTEGER NOT NULL, " +
                    "`pinned` INTEGER NOT NULL, " +
                    "FOREIGN KEY(`shopping_list_id`) REFERENCES `shopping_lists`(`id`) " +
                    "ON UPDATE NO ACTION ON DELETE CASCADE, " +
                    "FOREIGN KEY(`product_id`) REFERENCES `products`(`id`) " +
                    "ON UPDATE NO ACTION ON DELETE CASCADE, " +
                    "FOREIGN KEY(`store_id`) REFERENCES `stores`(`id`) " +
                    "ON UPDATE NO ACTION ON DELETE SET NULL)"
            )
            db.execSQL(
                "INSERT INTO `shopping_list_items` " +
                    "(`id`, `shopping_list_id`, `product_id`, `store_id`, `category_id`, `quantity`, `done`, `pinned`) " +
                    "SELECT `id`, `shopping_list_id`, `product_id`, `store_id`, `category_id`, `quantity`, `done`, `pinned` FROM `shopping_list_items_tmp`"
            )
            db.execSQL("DROP TABLE `shopping_list_items_tmp`")
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_shopping_list_items_shopping_list_id` " +
                    "ON `shopping_list_items` (`shopping_list_id`)"
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_shopping_list_items_product_id` " +
                    "ON `shopping_list_items` (`product_id`)"
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_shopping_list_items_store_id` " +
                    "ON `shopping_list_items` (`store_id`)"
            )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_shopping_list_items_category_id` " +
                        "ON `shopping_list_items` (`category_id`)"
                )
            }
        }

    // - Adds `shopping_lists.category_id` (nullable) so a list can force a
    //   category on every item added to it. Null for existing lists.
    val MIGRATION_7_8 = object : Migration(7, 8) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE shopping_lists ADD COLUMN category_id TEXT")
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_shopping_lists_category_id` " +
                    "ON `shopping_lists` (`category_id`)"
            )
        }
    }
}
}
