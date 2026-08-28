package com.easypocket.mobile.domain

import com.easypocket.mobile.i18n.Language
import com.easypocket.mobile.i18n.t
import java.text.Normalizer

object ListLogic {

    fun normalize(s: String): String =
        Normalizer.normalize(s, Normalizer.Form.NFD)
            .replace(Regex("\\p{M}+"), "")
            .lowercase()

    fun itemTotal(item: ShoppingListItem, product: Product?): Double {
        val storeId = item.storeId ?: return 0.0
        val price = product?.prices?.firstOrNull { it.storeId == storeId }?.value ?: return 0.0
        return price * item.quantity
    }

    private fun matchesFilter(item: ShoppingListItem, storeFilter: String?): Boolean =
        storeFilter == null || item.storeId == storeFilter

    fun totalAmount(list: ShoppingList, productsById: Map<String, Product>, storeFilter: String?): Double =
        list.items.filter { matchesFilter(it, storeFilter) }
            .sumOf { itemTotal(it, productsById[it.productId]) }

    fun cartAmount(list: ShoppingList, productsById: Map<String, Product>, storeFilter: String?): Double =
        list.items.filter { it.done && matchesFilter(it, storeFilter) }
            .sumOf { itemTotal(it, productsById[it.productId]) }

    fun orderedItems(items: List<ShoppingListItem>, productsById: Map<String, Product>): List<ShoppingListItem> {
        fun nameOf(i: ShoppingListItem) = productsById[i.productId]?.productName ?: ""
        val pending = items.filter { !it.done }.sortedWith(
            compareByDescending<ShoppingListItem> { it.pinned }.thenBy { normalize(nameOf(it)) }
        )
        val done = items.filter { it.done }.sortedBy { normalize(nameOf(it)) }
        return pending + done
    }

    fun unitLabelKey(unit: UnitOfMeasurement, quantity: Double): String =
        if (quantity > 1.0) "unit.${unit.raw}.plural" else "unit.${unit.raw}"

    fun clipboardText(list: ShoppingList, productsById: Map<String, Product>, language: Language): String =
        list.items.joinToString("\n") { item ->
            val product = productsById[item.productId]
            val unitLabel = product?.let { unitLabel(it.unitOfMeasurement, item.quantity, language) } ?: ""
            "${product?.productName ?: ""} ... ${trimQuantity(item.quantity)} $unitLabel"
        }

    fun trimQuantity(q: Double): String =
        if (q == q.toLong().toDouble()) q.toLong().toString() else q.toString()

    private fun unitLabel(unit: UnitOfMeasurement, quantity: Double, language: Language): String {
        val key = unitLabelKey(unit, quantity)
        val label = t(key, language)
        return if (label != key) label else unit.raw
    }
}