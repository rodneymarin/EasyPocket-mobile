package com.easypocket.mobile.ui.components

import com.easypocket.mobile.i18n.Language
import org.junit.Assert.assertEquals
import org.junit.Test

class AmountFormatTest {

    @Test
    fun `formatAmount adds thousands separator in english`() {
        assertEquals("$1,234.56", formatAmount(Language.ENGLISH, 1234.56))
        assertEquals("$12,345.00", formatAmount(Language.ENGLISH, 12345.0))
        assertEquals("$1,234,567.89", formatAmount(Language.ENGLISH, 1234567.89))
    }

    @Test
    fun `formatAmount adds thousands separator in spanish`() {
        assertEquals("$1.234,56", formatAmount(Language.SPANISH, 1234.56))
        assertEquals("$12.345,00", formatAmount(Language.SPANISH, 12345.0))
        assertEquals("$1.234.567,89", formatAmount(Language.SPANISH, 1234567.89))
    }

    @Test
    fun `formatAmount keeps small amounts with two decimals`() {
        assertEquals("$0.00", formatAmount(Language.ENGLISH, 0.0))
        assertEquals("$999.99", formatAmount(Language.ENGLISH, 999.99))
        assertEquals("$0,00", formatAmount(Language.SPANISH, 0.0))
    }

    @Test
    fun `formatAmount handles negatives`() {
        assertEquals("-$1,234.56", formatAmount(Language.ENGLISH, -1234.56))
        assertEquals("-$1.234,56", formatAmount(Language.SPANISH, -1234.56))
    }

    @Test
    fun `formatAmountCompact uses short labels`() {
        assertEquals("$1K", formatAmountCompact(Language.ENGLISH, 1000.0))
        assertEquals("$1.2K", formatAmountCompact(Language.ENGLISH, 1200.0))
        assertEquals("$12K", formatAmountCompact(Language.ENGLISH, 12345.0))
        assertEquals("$123K", formatAmountCompact(Language.ENGLISH, 123456.0))
        assertEquals("$1.2M", formatAmountCompact(Language.ENGLISH, 1234567.0))
        assertEquals("$999.99", formatAmountCompact(Language.ENGLISH, 999.99))
    }

    @Test
    fun `formatAmountCompact rounds up to next tier at the boundary`() {
        assertEquals("$1M", formatAmountCompact(Language.ENGLISH, 999999.0))
        assertEquals("$1M", formatAmountCompact(Language.ENGLISH, 999950.0))
    }

    @Test
    fun `formatAmountCompact handles negatives`() {
        assertEquals("-$1.2K", formatAmountCompact(Language.ENGLISH, -1234.0))
    }
}
