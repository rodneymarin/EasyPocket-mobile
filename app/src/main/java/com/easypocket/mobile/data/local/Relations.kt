package com.easypocket.mobile.data.local

import androidx.room.Embedded
import androidx.room.Relation

data class ShoppingListWithItems(
    @Embedded val list: ShoppingListEntity,
    @Relation(parentColumn = "id", entityColumn = "shopping_list_id")
    val items: List<ShoppingListItemEntity>,
)

data class PurchaseHistoryWithItems(
    @Embedded val record: PurchaseHistoryEntity,
    @Relation(parentColumn = "id", entityColumn = "history_id")
    val items: List<PurchaseHistoryItemEntity>,
    @Relation(parentColumn = "id", entityColumn = "history_id")
    val categoryTotals: List<PurchaseHistoryCategoryTotalEntity> = emptyList(),
)
