package by.overpas.detekt.architecture

internal data class FunctionScore(
    val part: Part,
    val parameters: Int,
    val calls: Int,
) : Measured {

    override val name: String = part.name

    override fun total(score: (Part) -> Double): Double =
        score(part)

    override fun details(score: (Part) -> Double): String =
        "Parameters: $parameters, calls: $calls."
}
