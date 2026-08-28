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
    fun `totalAmount counts only pending and respects store filter`() {
        val items = listOf(
            ShoppingListItem(1, "p1", 1.0, "s1", done = false, pinned = false), // 10
            ShoppingListItem(2, "p1", 1.0, "s2", done = true, pinned = false),  // 8.5 done
            ShoppingListItem(3, "p2", 3.0, "s1", done = false, pinned = false), // 6
        )
        val list = ShoppingList("l1", "Lista", items)
        assertEquals(16.0, ListLogic.totalAmount(list, products, null), 0.001)
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
        val text = ListLogic.clipboardText(ShoppingList("l1", "Lista", items), products, Language.ENGLISH)
        val lines = text.lines()
        assertEquals("Milk ... 2 Liters", lines[0])
        assertEquals("Bread ... 1 Unit", lines[1])
    }

    @Test
    fun `normalize strips accents and lowercases`() {
        assertEquals("papa", ListLogic.normalize("Papá"))
        assertEquals("nino", ListLogic.normalize("NIÑO"))
    }
}