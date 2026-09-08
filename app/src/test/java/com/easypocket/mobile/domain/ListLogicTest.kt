package com.easypocket.mobile.domain

import com.easypocket.mobile.i18n.Language
import org.junit.Assert.assertEquals
import org.junit.Test

class ListLogicTest {
    private val milk = Product("p1", "Milk", UnitOfMeasurement.LT, listOf(Price("s1", 10.0), Price("s2", 8.5)))
    private val bread = Product("p2", "Bread", UnitOfMeasurement.UNIT, listOf(Price("s1", 2.0)))
    private val products = mapOf("p1" to milk, "p2" to bread)

    @Test
    fun `itemTotal uses price at item store`() {
        val item = ShoppingListItem(1, "p1", 2.0, "s1", done = false, pinned = false)
        assertEquals(20.0, ListLogic.itemTotal(item, milk), 0.001)
    }

    @Test
    fun `itemTotal without store is zero`() {
        val item = ShoppingListItem(1, "p1", 2.0, null, done = false, pinned = false)
        assertEquals(0.0, ListLogic.itemTotal(item, milk), 0.001)
    }

    @Test
    fun `itemTotal without price at store is zero`() {
        val item = ShoppingListItem(1, "p2", 2.0, "s2", done = false, pinned = false)
        assertEquals(0.0, ListLogic.itemTotal(item, bread), 0.001)
    }

    @Test
    fun `totalAmount counts all items (including done) and respects store filter`() {
        val items = listOf(
            ShoppingListItem(1, "p1", 1.0, "s1", done = false, pinned = false), // 10
            ShoppingListItem(2, "p1", 1.0, "s2", done = true, pinned = false),  // 8.5 done
            ShoppingListItem(3, "p2", 3.0, "s1", done = false, pinned = false), // 6
        )
        val list = ShoppingList("l1", "Lista", items = items)
        assertEquals(24.5, ListLogic.totalAmount(list, products, null), 0.001) // 10 + 8.5 + 6
        assertEquals(16.0, ListLogic.totalAmount(list, products, "s1"), 0.001) // ambos pendientes en s1: 10 + 6
        assertEquals(8.5, ListLogic.cartAmount(list, products, null), 0.001)
        assertEquals(0.0, ListLogic.cartAmount(list, products, "s1"), 0.001)    // único done está en s2
    }

    @Test
    fun `orderedItems pending first pinned top then alphabetical`() {
        val items = listOf(
            ShoppingListItem(1, "p1", 1.0, null, done = true, pinned = false),   // Milk done
            ShoppingListItem(2, "p2", 1.0, null, done = false, pinned = true),   // Bread pinned
            ShoppingListItem(3, "p1", 1.0, null, done = false, pinned = false),  // Milk
        )
        val ordered = ListLogic.orderedItems(items, products)
        assertEquals(listOf(2L, 3L, 1L), ordered.map { it.id })
    }

    @Test
    fun `clipboardText formats name quantity unit`() {
        val items = listOf(
            ShoppingListItem(1, "p1", 2.0, null),
            ShoppingListItem(2, "p2", 1.0, null),
        )
        val text = ListLogic.clipboardText(ShoppingList("l1", "Lista", items = items), products, Language.ENGLISH)
        val lines = text.lines()
        assertEquals("Milk ... 2 Liters", lines[0])
        assertEquals("Bread ... 1 Unit", lines[1])
    }

    @Test
    fun `normalize strips accents and lowercases`() {
        assertEquals("papa", ListLogic.normalize("Papá"))
        assertEquals("nino", ListLogic.normalize("NIÑO"))
    }

    @Test
    fun `findBestProductMatch returns null for blank or unmatched queries`() {
        assertEquals(null, ListLogic.findBestProductMatch("", products.values.toList()))
        assertEquals(null, ListLogic.findBestProductMatch("   ", products.values.toList()))
        assertEquals(null, ListLogic.findBestProductMatch("chocolate", products.values.toList()))
    }

    @Test
    fun `findBestProductMatch matches single word ignoring case and accents`() {
        assertEquals(milk, ListLogic.findBestProductMatch("milk", products.values.toList()))
        assertEquals(milk, ListLogic.findBestProductMatch("MILK", products.values.toList()))
    }

    @Test
    fun `findBestProductMatch uses all words in the line`() {
        val wholeMilk = Product("p3", "Leche entera", UnitOfMeasurement.UNIT)
        val lactoseFreeMilk = Product("p4", "Leche deslactosada", UnitOfMeasurement.UNIT)
        val list = listOf(milk, bread, wholeMilk, lactoseFreeMilk)
        assertEquals(wholeMilk, ListLogic.findBestProductMatch("leche entera", list))
        assertEquals(null, ListLogic.findBestProductMatch("leche chocolate", list))
    }

    @Test
    fun `findBestProductMatch picks best score and prefers exact match`() {
        val milkPowder = Product("p3", "Milk powder", UnitOfMeasurement.UNIT)
        val list = listOf(milk, bread, milkPowder)
        assertEquals(milk, ListLogic.findBestProductMatch("milk", list))
        assertEquals(milkPowder, ListLogic.findBestProductMatch("powder milk", list))
    }

    @Test
    fun `groupedSections sorts categories alphabetically, items alphabetically and uncategorized last`() {
        val fruits = Category("c1", "Frutas")
        val veggies = Category("c2", "Verduras")
        val apple = Product("p1", "Apple", UnitOfMeasurement.UNIT)
        val banana = Product("p2", "Banana", UnitOfMeasurement.UNIT)
        val carrot = Product("p3", "Carrot", UnitOfMeasurement.UNIT)
        val water = Product("p4", "Water", UnitOfMeasurement.LT)
        val products = mapOf("p1" to apple, "p2" to banana, "p3" to carrot, "p4" to water)
        val categories = mapOf("c1" to fruits, "c2" to veggies)
        val items = listOf(
            ShoppingListItem(1, "p4", 1.0, null),
            ShoppingListItem(2, "p3", 1.0, null, categoryId = "c2"),
            ShoppingListItem(3, "p2", 1.0, null, categoryId = "c1"),
            ShoppingListItem(4, "p1", 1.0, null, categoryId = "c1", pinned = true),
        )
        val sections = ListLogic.groupedSections(items, products, categories)
        assertEquals(listOf("c1", "c2", null), sections.map { it.category?.id })
        assertEquals(listOf(4L, 3L), sections[0].items.map { it.id }) // pinned primero, luego alfabético
        assertEquals(listOf(2L), sections[1].items.map { it.id })
        assertEquals(listOf(1L), sections[2].items.map { it.id })
    }

    @Test
    fun `matchesFilter uses the item category, not the product's`() {
        // Dos ítems del mismo producto: uno categorizado y otro sin categoría.
        val list = ShoppingList(
            "l1",
            "Lista",
            items = listOf(
                ShoppingListItem(1, "p1", 1.0, "s1", categoryId = "c1"),
                ShoppingListItem(2, "p1", 1.0, "s2"),
            ),
        )
        assertEquals(10.0, ListLogic.totalAmount(list, products, null, "c1"), 0.001)
        assertEquals(18.5, ListLogic.totalAmount(list, products, null), 0.001)
    }
}