package com.easypocket.mobile.data.seed

import com.easypocket.mobile.domain.Category
import com.easypocket.mobile.domain.Price
import com.easypocket.mobile.domain.Product
import com.easypocket.mobile.domain.Store
import com.easypocket.mobile.domain.UnitOfMeasurement

object SeedData {

    data class SeedListItem(
        val productId: String,
        val storeId: String?,
        val quantity: Double,
        val categoryId: String? = null,
        val done: Boolean = false,
        val pinned: Boolean = false,
    )

    data class SeedList(
        val id: String,
        val title: String,
        val items: List<SeedListItem>,
    )

    val stores: List<Store> = listOf(
        Store(id = "store-demo", description = "Demo Store", color = 0),
        Store(id = "store-test", description = "Test Store", color = 1),
    )

    val products: List<Product> = listOf(
        Product("prod-001", "Papa", UnitOfMeasurement.KG, listOf(Price("store-demo", 2.2), Price("store-test", 3.1))),
        Product("prod-002", "Cebolla", UnitOfMeasurement.KG, listOf(Price("store-demo", 1.8), Price("store-test", 1.95))),
        Product("prod-003", "Tomate", UnitOfMeasurement.KG, listOf(Price("store-test", 2.5))),
        Product("prod-004", "Leche líquida", UnitOfMeasurement.LT, listOf(Price("store-demo", 1.5), Price("store-test", 1.6))),
        Product("prod-005", "Yogurt Firme", UnitOfMeasurement.UNIT, listOf(Price("store-demo", 0.9))),
        Product("prod-006", "Queso Blanco", UnitOfMeasurement.KG, listOf(Price("store-demo", 5.8), Price("store-test", 6.2))),
        Product("prod-007", "Mantequilla", UnitOfMeasurement.UNIT, listOf(Price("store-test", 2.1))),
        Product("prod-008", "Pechuga de Pollo", UnitOfMeasurement.KG, listOf(Price("store-demo", 4.5), Price("store-test", 4.2))),
        Product("prod-009", "Carne Molida", UnitOfMeasurement.KG, listOf(Price("store-demo", 6.9), Price("store-test", 7.1))),
        Product("prod-010", "Filet de Merluza", UnitOfMeasurement.KG, emptyList()),
    )

    val categories: List<Category> = listOf(
        Category(id = "cat-verduras", name = "Verduras", icon = "🥬"),
        Category(id = "cat-lacteos", name = "Lácteos", icon = "🥛"),
        Category(id = "cat-carnes", name = "Carnes", icon = "🍖"),
        Category(id = "cat-pescados", name = "Pescados", icon = "🐟"),
    )

    // Remembers the last category used per product so the item form
    // pre-selects it when adding the product to a list.
    val lastCategories: List<Pair<String, String>> = listOf(
        "prod-001" to "cat-verduras",
        "prod-002" to "cat-verduras",
        "prod-003" to "cat-verduras",
        "prod-004" to "cat-lacteos",
        "prod-005" to "cat-lacteos",
        "prod-006" to "cat-lacteos",
        "prod-007" to "cat-lacteos",
        "prod-008" to "cat-carnes",
        "prod-009" to "cat-carnes",
        "prod-010" to "cat-pescados",
    )

    val lists: List<SeedList> = listOf(
        SeedList(
            id = "0oasidu0as9dua0sd",
            title = "Lista Semanal - Verduras",
            items = listOf(
                SeedListItem(productId = "prod-001", storeId = "store-demo", quantity = 2.5, categoryId = "cat-verduras"),
                SeedListItem(productId = "prod-002", storeId = "store-demo", quantity = 1.5, categoryId = "cat-verduras"),
                SeedListItem(productId = "prod-003", storeId = "store-test", quantity = 2.0, categoryId = "cat-verduras"),
            ),
        ),
        SeedList(
            id = "aosidoaisud0a89sud0a9sdui",
            title = "Lácteos y Desayuno",
            items = listOf(
                SeedListItem(productId = "prod-004", storeId = "store-test", quantity = 4.0, categoryId = "cat-lacteos"),
                SeedListItem(productId = "prod-005", storeId = "store-demo", quantity = 6.0, categoryId = "cat-lacteos"),
                SeedListItem(productId = "prod-006", storeId = "store-demo", quantity = 0.8, categoryId = "cat-lacteos"),
                SeedListItem(productId = "prod-007", storeId = "store-test", quantity = 2.0, categoryId = "cat-lacteos"),
            ),
        ),
        SeedList(
            id = "aosdijqw0ewdj0as9dja0sd",
            title = "Carnicería",
            items = listOf(
                SeedListItem(productId = "prod-008", storeId = "store-test", quantity = 1.5, categoryId = "cat-carnes"),
                SeedListItem(productId = "prod-009", storeId = "store-demo", quantity = 1.2, categoryId = "cat-carnes"),
                SeedListItem(productId = "prod-010", storeId = "store-demo", quantity = 1.0, categoryId = "cat-pescados"),
            ),
        ),
    )
}
