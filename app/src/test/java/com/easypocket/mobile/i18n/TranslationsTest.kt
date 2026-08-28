package com.easypocket.mobile.i18n

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class TranslationsTest {
    @Test
    fun `missing key falls back to key`() {
        assertEquals("nope.missing", t("nope.missing", Language.ENGLISH))
        assertEquals("nope.missing", t("nope.missing", Language.SPANISH))
    }

    @Test
    fun `interpolates params`() {
        val en = t("lists.showingCount", Language.ENGLISH, mapOf("count" to "3"))
        val es = t("lists.showingCount", Language.SPANISH, mapOf("count" to "3"))
        assertEquals("Showing 3 lists", en)
        assertEquals("Mostrando 3 listas", es)
    }

    @Test
    fun `interpolates multiple params`() {
        val result = t("list.items", Language.ENGLISH, mapOf("completed" to "2", "count" to "5"))
        assertEquals("2/5 items", result)
    }

    @Test
    fun `english and spanish share the same key set`() {
        assertEquals(englishStrings.keys, spanishStrings.keys)
    }

    @Test
    fun `cloud and auth keys were removed`() {
        listOf(
            "menu.dataSource",
            "menu.cloud",
            "menu.local",
            "menu.signOut",
            "auth.signIn",
        ).forEach { key ->
            assertEquals(key, t(key, Language.ENGLISH))
        }
    }

    @Test
    fun `export and import keys are kept`() {
        assertEquals("Export data", t("menu.exportData", Language.ENGLISH))
        assertEquals("Import data", t("menu.importData", Language.ENGLISH))
    }

    @Test
    fun `backup keys are translated in both languages`() {
        listOf(
            "backup.exportSuccess",
            "backup.exportError",
            "backup.importSuccess",
            "backup.importError",
            "backup.importWarning.title",
            "backup.importWarning.message",
        ).forEach { key ->
            assertNotEquals(key, t(key, Language.ENGLISH))
            assertNotEquals(key, t(key, Language.SPANISH))
        }
    }

    @Test
    fun `default language is english for unknown code`() {
        assertEquals(Language.ENGLISH, Language.fromCode(null))
        assertEquals(Language.ENGLISH, Language.fromCode("fr"))
        assertEquals(Language.SPANISH, Language.fromCode("es"))
    }
}
