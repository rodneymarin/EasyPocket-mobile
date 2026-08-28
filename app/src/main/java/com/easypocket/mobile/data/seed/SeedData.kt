package com.easypocket.mobile.data.seed

import com.easypocket.mobile.domain.Price
import com.easypocket.mobile.domain.Product
import com.easypocket.mobile.domain.Store
import com.easypocket.mobile.domain.UnitOfMeasurement

object SeedData {

    data class SeedListItem(
        val productId: String,
        val storeId: String?,
        val quantity: Double,
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

    val lists: List<SeedList> = listOf(
        SeedList(
            id = "0oasidu0as9dua0sd",
            title = "Lista Semanal - Verduras",
            items = listOf(
                SeedListItem(productId = "prod-001", storeId = "store-demo", quantity = 2.5),
                SeedListItem(productId = "prod-002", storeId = "store-demo", quantity = 1.5),
                SeedListItem(productId = "prod-003", storeId = "store-test", quantity = 2.0),
            ),
        ),
        SeedList(
            id = "aosidoaisud0a89sud0a9sdui",
            title = "Lácteos y Desayuno",
            items = listOf(
                SeedListItem(productId = "prod-004", storeId = "store-test", quantity = 4.0),
                SeedListItem(productId = "prod-005", storeId = "store-demo", quantity = 6.0),
                SeedListItem(productId = "prod-006", storeId = "store-demo", quantity = 0.8),
                SeedListItem(productId = "prod-007", storeId = "store-test", quantity = 2.0),
            ),
        ),
        SeedList(
            id = "aosdijqw0ewdj0as9dja0sd",
            title = "Carnicería",
            items = listOf(
                SeedListItem(productId = "prod-008", storeId = "store-test", quantity = 1.5),
                SeedListItem(productId = "prod-009", storeId = "store-demo", quantity = 1.2),
                SeedListItem(productId = "prod-010", storeId = "store-demo", quantity = 1.0),
            ),
        ),
    )
}
