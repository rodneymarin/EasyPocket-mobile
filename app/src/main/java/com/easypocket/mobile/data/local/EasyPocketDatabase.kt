package com.easypocket.mobile.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        StoreEntity::class, ProductEntity::class, PriceEntity::class,
        ShoppingListEntity::class, ShoppingListItemEntity::class,
    ],
    version = 2,
    exportSchema = false,
)
abstract class EasyPocketDatabase : RoomDatabase() {
    abstract fun storeDao(): StoreDao
    abstract fun productDao(): ProductDao
    abstract fun priceDao(): PriceDao
    abstract fun listDao(): ShoppingListDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE shopping_lists ADD COLUMN icon TEXT NOT NULL DEFAULT '$'")
            }
        }
    }
}
