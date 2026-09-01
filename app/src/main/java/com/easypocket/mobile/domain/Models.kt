package com.easypocket.mobile.domain

data class Store(val id: String, val description: String, val color: Int)
data class Category(val id: String, val name: String, val icon: String = "$")
data class Price(val storeId: String, val value: Double)
data class Product(
    val id: String,
    val productName: String,
    val unitOfMeasurement: UnitOfMeasurement,
    val prices: List<Price> = emptyList(),
    val categoryId: String? = null,
)
data class ShoppingListItem(
    val id: Long,
    val productId: String,
    val quantity: Double,
    val storeId: String?,
    val done: Boolean = false,
    val pinned: Boolean = false,
)
data class ShoppingList(
    val id: String,
    val title: String,
    val icon: String = "$",
    val items: List<ShoppingListItem> = emptyList(),
)
