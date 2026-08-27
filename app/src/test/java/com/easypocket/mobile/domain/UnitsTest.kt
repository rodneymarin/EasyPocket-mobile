package com.easypocket.mobile.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class UnitsTest {
    @Test
    fun `fromRaw maps known values`() {
        assertEquals(UnitOfMeasurement.KG, UnitOfMeasurement.fromRaw("kg"))
        assertEquals(UnitOfMeasurement.LATA, UnitOfMeasurement.fromRaw("lata"))
        assertEquals(UnitOfMeasurement.UNIT, UnitOfMeasurement.fromRaw("unit"))
    }

    @Test
    fun `fromRaw returns null for unknown`() {
        assertNull(UnitOfMeasurement.fromRaw("gallon"))
        assertNull(UnitOfMeasurement.fromRaw(""))
    }

    @Test
    fun `raw roundtrip`() {
        UnitOfMeasurement.entries.forEach { assertEquals(it, UnitOfMeasurement.fromRaw(it.raw)) }
    }
}
