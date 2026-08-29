package com.easypocket.mobile.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "stores")
data class StoreEntity(
    @PrimaryKey val id: String,
    val description: String,
    val color: Int = 0,
)

@Entity(tableName = "products", indices = [Index("product_name")])
data class ProductEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "product_name") val productName: String,
    @ColumnInfo(name = "unit_of_measurement") val unitOfMeasurement: String,
)

@Entity(
    tableName = "product_prices",
    primaryKeys = ["product_id", "store_id"],
    foreignKeys = [
        ForeignKey(entity = ProductEntity::class, parentColumns = ["id"],
            childColumns = ["product_id"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = StoreEntity::class, parentColumns = ["id"],
            childColumns = ["store_id"], onDelete = ForeignKey.CASCADE),
    ],
)
data class PriceEntity(
    @ColumnInfo(name = "product_id") val productId: String,
    @ColumnInfo(name = "store_id") val storeId: String,
    val value: Double,
)

@Entity(tableName = "shopping_lists")
data class ShoppingListEntity(
    @PrimaryKey val id: String,
    val title: String,
    @ColumnInfo(name = "icon", defaultValue = "$") val icon: String = "$",
)

@Entity(
    tableName = "shopping_list_items",
    foreignKeys = [
        ForeignKey(entity = ShoppingListEntity::class, parentColumns = ["id"],
            childColumns = ["shopping_list_id"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = ProductEntity::class, parentColumns = ["id"],
            childColumns = ["product_id"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = StoreEntity::class, parentColumns = ["id"],
            childColumns = ["store_id"], onDelete = ForeignKey.SET_NULL),
    ],
    indices = [Index("shopping_list_id"), Index("product_id"), Index("store_id")],
)
data class ShoppingListItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "shopping_list_id") val shoppingListId: String,
    @ColumnInfo(name = "product_id") val productId: String,
    @ColumnInfo(name = "store_id") val storeId: String?,
    val quantity: Double,
    val done: Boolean = false,
    val pinned: Boolean = false,
)
