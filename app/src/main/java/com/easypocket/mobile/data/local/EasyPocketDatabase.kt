package com.easypocket.mobile.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        StoreEntity::class, ProductEntity::class, PriceEntity::class,
        ShoppingListEntity::class, ShoppingListItemEntity::class,
    ],
    version = 1,
    exportSchema = false,
)
abstract class EasyPocketDatabase : RoomDatabase() {
    abstract fun storeDao(): StoreDao
    abstract fun productDao(): ProductDao
    abstract fun priceDao(): PriceDao
    abstract fun listDao(): ShoppingListDao
}
