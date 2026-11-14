package by.overpas.detekt.architecture

import java.util.Locale

internal interface Measured {
    val name: String

    fun total(score: (Part) -> Double): Double

    fun details(score: (Part) -> Double): String
}

internal fun Double.oneDecimal(): String =
    String.format(Locale.ROOT, "%.1f", this)
