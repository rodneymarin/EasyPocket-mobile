package com.easypocket.mobile.data.backup

import kotlinx.serialization.Serializable

@Serializable
data class BackupData(
    val version: Int,
    val exportedAt: String,
    val stores: List<BackupStore>,
    val products: List<BackupProduct>,
    val prices: List<BackupPrice>,
    val shoppingLists: List<BackupList>,
    val listItems: List<BackupListItem>,
)

@Serializable
data class BackupStore(
    val id: String,
    val description: String,
    val color: Int,
)

@Serializable
data class BackupProduct(
    val id: String,
    val productName: String,
    val unitOfMeasurement: String,
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
)

@Serializable
data class BackupListItem(
    val id: Long,
    val shoppingListId: String,
    val productId: String,
    val storeId: String? = null,
    val quantity: Double,
    val done: Boolean = false,
    val pinned: Boolean = false,
)
