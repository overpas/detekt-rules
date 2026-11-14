package by.overpas.detekt.architecture

internal data class Part(
    val name: String,
    val normal: Double,
    val composable: Double,
) {

    constructor(
        name: String,
        score: Double,
        isComposable: Boolean,
    ) : this(name, if (isComposable) 0.0 else score, if (isComposable) score else 0.0)

    constructor(
        name: String,
        parts: List<Part>,
    ) : this(name, parts.sumOf { it.normal }, parts.sumOf { it.composable })
}
