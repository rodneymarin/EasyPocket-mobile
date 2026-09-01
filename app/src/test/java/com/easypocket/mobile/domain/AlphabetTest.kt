package com.easypocket.mobile.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class AlphabetTest {

    private fun assertOrder(expected: List<String>) {
        val sorted = expected.shuffled().sortedWith(Alphabet.comparator { it })
        assertEquals(expected, sorted)
    }

    @Test
    fun `accented vowels sort next to their base letter`() {
        assertOrder(
            listOf("Árbol", "Bebida", "Ébano", "Fabricar", "Íntimo", "Lápiz", "Mesa", "Óptimo", "Pan", "Útil"),
        )
    }

    @Test
    fun `ene sorts as its own letter between n and o`() {
        assertOrder(listOf("Anillo", "Año", "Apio"))
    }

    @Test
    fun `case does not affect order`() {
        assertOrder(listOf("arándano", "Banana", "CEREZA"))
    }

    @Test
    fun `identical primary strings get a deterministic tiebreak`() {
        val sorted = listOf("mamá", "MAMA", "Mamá").sortedWith(Alphabet.comparator { it })
        assertEquals(listOf("MAMA", "Mamá", "mamá"), sorted)
    }
    @Test
    fun `comparator works on a selected property`() {
        data class Item(val name: String)
        val items = listOf(Item("Zorro"), Item("Árbol"))
        val sorted = items.sortedWith(Alphabet.comparator { it.name })
        assertEquals("Árbol", sorted.first().name)
    }
}
