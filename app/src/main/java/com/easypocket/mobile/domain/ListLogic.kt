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

    private fun matchesFilter(
        item: ShoppingListItem,
        productsById: Map<String, Product>,
        storeFilter: String?,
        categoryFilter: String?,
    ): Boolean =
        (storeFilter == null || item.storeId == storeFilter) &&
            (categoryFilter == null || item.categoryId == categoryFilter)

    fun totalAmount(
        list: ShoppingList,
        productsById: Map<String, Product>,
        storeFilter: String?,
        categoryFilter: String? = null,
    ): Double =
        list.items.filter { matchesFilter(it, productsById, storeFilter, categoryFilter) }
            .sumOf { itemTotal(it, productsById[it.productId]) }

    fun cartAmount(
        list: ShoppingList,
        productsById: Map<String, Product>,
        storeFilter: String?,
        categoryFilter: String? = null,
    ): Double =
        list.items.filter { it.done && matchesFilter(it, productsById, storeFilter, categoryFilter) }
            .sumOf { itemTotal(it, productsById[it.productId]) }

    fun orderedItems(items: List<ShoppingListItem>, productsById: Map<String, Product>): List<ShoppingListItem> {
        fun nameOf(i: ShoppingListItem) = productsById[i.productId]?.productName ?: ""
        val pending = items.filter { !it.done }.sortedWith(
            compareByDescending<ShoppingListItem> { it.pinned }.thenBy { normalize(nameOf(it)) }
        )
        val done = items.filter { it.done }.sortedBy { normalize(nameOf(it)) }
        return pending + done
    }

    fun groupedSections(
        items: List<ShoppingListItem>,
        productsById: Map<String, Product>,
        categoriesById: Map<String, Category>,
    ): List<ItemSection> {
        fun sortGroup(group: List<ShoppingListItem>): List<ShoppingListItem> = group.sortedWith(
            compareByDescending<ShoppingListItem> { it.pinned }
                .thenBy { normalize(productsById[it.productId]?.productName ?: "") },
        )
        val groups = items.groupBy { it.categoryId }
        val sections = groups.filterKeys { it != null }.map { (categoryId, group) ->
            ItemSection(categoriesById[categoryId], sortGroup(group))
        }.sortedWith(Alphabet.comparator { it.category?.name ?: "" })
        val withoutCategory = groups[null]?.let { listOf(ItemSection(null, sortGroup(it))) } ?: emptyList()
        return sections + withoutCategory
    }

    fun unitLabelKey(unit: UnitOfMeasurement, quantity: Double): String =
        if (quantity > 1.0) "unit.${unit.raw}.plural" else "unit.${unit.raw}"

    fun clipboardText(
        list: ShoppingList,
        productsById: Map<String, Product>,
        language: Language,
        storeFilter: String? = null,
        categoryFilter: String? = null,
    ): String =
        list.items.filter { matchesFilter(it, productsById, storeFilter, categoryFilter) }.joinToString("\n") { item ->
            val product = productsById[item.productId]
            val unitLabel = product?.let { unitLabel(it.unitOfMeasurement, item.quantity, language) } ?: ""
            "${product?.productName ?: ""} ... ${trimQuantity(item.quantity)} $unitLabel"
        }

    fun findBestProductMatch(query: String, products: List<Product>): Product? {
        val normalizedQuery = normalize(query).trim()
        if (normalizedQuery.isEmpty()) return null
        val queryWords = normalizedQuery.split(Regex("\\s+")).filter { it.isNotEmpty() }
        var best: Product? = null
        var bestScore = -1
        for (product in products) {
            val name = normalize(product.productName)
            if (name == normalizedQuery) return product
            val nameWords = name.split(Regex("\\s+"))
            if (queryWords.any { !name.contains(it) }) continue
            val score = queryWords.sumOf { word -> if (nameWords.any { it == word }) 2 else 1 }
            if (score > bestScore || (score == bestScore && best != null && name.length < normalize(best.productName).length)) {
                bestScore = score
                best = product
            }
        }
        return best
    }

    fun trimQuantity(q: Double): String =
        if (q == q.toLong().toDouble()) q.toLong().toString() else q.toString()

    private fun unitLabel(unit: UnitOfMeasurement, quantity: Double, language: Language): String {
        val key = unitLabelKey(unit, quantity)
        val label = t(key, language)
        return if (label != key) label else unit.raw
    }
}