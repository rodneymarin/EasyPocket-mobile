package com.easypocket.mobile.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class ListIconTest {

    @Test
    fun `keeps a single emoji`() {
        assertEquals("🛒", ListIcon.sanitize("🛒"))
    }

    @Test
    fun `keeps a single alphanumeric character`() {
        assertEquals("A", ListIcon.sanitize("A"))
        assertEquals("5", ListIcon.sanitize("5"))
    }

    @Test
    fun `keeps only the first grapheme when several are typed`() {
        assertEquals("A", ListIcon.sanitize("AB"))
        assertEquals("🛒", ListIcon.sanitize("🛒🎉"))
    }

    @Test
    fun `keeps a ZWJ emoji as one grapheme`() {
        assertEquals("👨‍👩‍👧", ListIcon.sanitize("👨‍👩‍👧"))
    }

    @Test
    fun `falls back to dollar for non alphanumeric ascii symbols`() {
        assertEquals("$", ListIcon.sanitize("!"))
        assertEquals("$", ListIcon.sanitize("#"))
    }

    @Test
    fun `returns empty for empty input`() {
        assertEquals("", ListIcon.sanitize(""))
        assertEquals("", ListIcon.sanitize("   "))
    }

    @Test
    fun `trims surrounding whitespace before taking the grapheme`() {
        assertEquals("🛒", ListIcon.sanitize("  🛒  "))
    }
}
