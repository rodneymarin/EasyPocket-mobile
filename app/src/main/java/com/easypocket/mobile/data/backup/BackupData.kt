package com.easypocket.mobile.data.backup

import kotlinx.serialization.Serializable

@Serializable
data class BackupData(
    val version: Int,
    val exportedAt: String,
    val stores: List<BackupStore>,
    val categories: List<BackupCategory> = emptyList(),
    val products: List<BackupProduct>,
    val prices: List<BackupPrice>,
    val shoppingLists: List<BackupList>,
    val listItems: List<BackupListItem>,
    val purchaseHistory: List<BackupPurchaseHistory> = emptyList(),
    val purchaseHistoryItems: List<BackupPurchaseHistoryItem> = emptyList(),
    val purchaseHistoryCategoryTotals: List<BackupPurchaseHistoryCategoryTotal> = emptyList(),
    val lastCategories: List<BackupProductLastCategory> = emptyList(),
)

@Serializable
data class BackupStore(
    val id: String,
    val description: String,
    val color: Int,
)

@Serializable
data class BackupCategory(
    val id: String,
    val name: String,
    val icon: String = "$",
)

@Serializable
data class BackupProduct(
    val id: String,
    val productName: String,
    val unitOfMeasurement: String,
    // Kept for backward compatibility with version 1 backups. In version 2
    // products no longer carry a category; it is exported to lastCategories.
    val categoryId: String? = null,
)

@Serializable
data class BackupProductLastCategory(
    val productId: String,
    val categoryId: String,
)

@Serializable
data class BackupPrice(
    val productId: String,
    val storeId: String,
    val value: Double,
)

@Serializable
data class BackupList(
    val id: String,
    val title: String,
    val icon: String = "$",
    val categoryId: String? = null,
)

@Serializable
data class BackupListItem(
    val id: Long,
    val shoppingListId: String,
    val productId: String,
    val storeId: String? = null,
    val categoryId: String? = null,
    val quantity: Double,
    val done: Boolean = false,
    val pinned: Boolean = false,
)

@Serializable
data class BackupPurchaseHistory(
    val id: String,
    val listTitle: String,
    val listIcon: String = "$",
    val date: Long,
    val totalAmount: Double,
    val itemCount: Int,
)

@Serializable
data class BackupPurchaseHistoryItem(
    val historyId: String,
    val productName: String,
    val storeName: String? = null,
    val quantity: Double,
    val unitPrice: Double,
    val totalPrice: Double,
    val categoryCode: String? = null,
    val itemUid: String? = null,
)

@Serializable
data class BackupPurchaseHistoryCategoryTotal(
    val historyId: String,
    val categoryCode: String? = null,
    val total: Double,
)
