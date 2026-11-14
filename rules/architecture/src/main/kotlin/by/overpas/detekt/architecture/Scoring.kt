package by.overpas.detekt.architecture

internal data class Scoring(
    val nestWeight: Double,
    val chainWeight: Double,
    val minFunctionScore: Double,
    val arePropertyTypeArgumentsCounted: Boolean,
    val nestedClassScore: Double,
)
