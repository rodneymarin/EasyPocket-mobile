package com.easypocket.mobile.domain

import java.text.Collator
import java.util.Locale

object Alphabet {

    private val collator: Collator = Collator.getInstance(Locale("es")).apply {
        strength = Collator.PRIMARY
    }

    fun compare(a: String, b: String): Int {
        val primary = collator.compare(a, b)
        if (primary != 0) return primary
        val normalized = ListLogic.normalize(a).compareTo(ListLogic.normalize(b))
        if (normalized != 0) return normalized
        return a.compareTo(b)
    }

    fun <T> comparator(selector: (T) -> String): Comparator<T> =
        Comparator { x, y -> compare(selector(x), selector(y)) }
}
