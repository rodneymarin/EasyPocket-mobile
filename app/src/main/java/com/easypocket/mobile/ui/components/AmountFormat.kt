package com.easypocket.mobile.ui.components

import com.easypocket.mobile.i18n.Language
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale
import kotlin.math.abs

private val EN_SYMBOLS = DecimalFormatSymbols(Locale.US).apply { currencySymbol = "$" }
private val ES_SYMBOLS = DecimalFormatSymbols().apply {
    currencySymbol = "$"
    decimalSeparator = ','
    groupingSeparator = '.'
}

private val EN_FORMAT = DecimalFormat("$#,##0.00", EN_SYMBOLS)
private val ES_FORMAT = DecimalFormat("$#,##0.00", ES_SYMBOLS)

fun formatAmount(language: Language, value: Double): String = when (language) {
    Language.SPANISH -> ES_FORMAT
    Language.ENGLISH -> EN_FORMAT
}.format(value)

fun formatAmountCompact(language: Language, value: Double): String {
    val sign = if (value < 0) "-" else ""
    val abs = abs(value)
    val body = when {
        abs >= 999_500.0 -> formatCompactUnit(abs / 1_000_000.0, 1) + "M"
        abs >= 10_000.0 -> formatCompactUnit(abs / 1_000.0, 0) + "K"
        abs >= 1_000.0 -> formatCompactUnit(abs / 1_000.0, 1) + "K"
        else -> return formatAmount(language, value)
    }
    return sign + "$" + body
}

private fun formatCompactUnit(scaled: Double, decimals: Int): String {
    val text = if (decimals == 0) {
        String.format(Locale.US, "%.0f", scaled)
    } else {
        String.format(Locale.US, "%.1f", scaled).removeSuffix(".0")
    }
    return text
}
