package com.easypocket.mobile.domain

enum class UnitOfMeasurement(val raw: String) {
    BAG("bag"), BOTTLE("bottle"), BOX("box"), CONTAINER("container"),
    KG("kg"), LATA("lata"), LT("lt"), PACK("pack"), UNIT("unit");

    companion object {
        fun fromRaw(raw: String): UnitOfMeasurement? =
            entries.firstOrNull { it.raw == raw }
    }
}
