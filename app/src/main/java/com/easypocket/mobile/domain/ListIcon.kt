package com.easypocket.mobile.domain

import java.text.BreakIterator

object ListIcon {
    const val DEFAULT = "$"

    fun sanitize(raw: String): String {
        val text = raw.trim()
        if (text.isEmpty()) return ""
        val iterator = BreakIterator.getCharacterInstance()
        iterator.setText(text)
        val first = text.substring(0, iterator.next())
        val c = first.first()
        return if (c.isLetterOrDigit() || c.code > 127) first else DEFAULT
    }
}
