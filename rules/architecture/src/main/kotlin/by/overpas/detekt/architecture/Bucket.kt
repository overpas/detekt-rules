package by.overpas.detekt.architecture

internal class Bucket(
    private val label: String,
    private val limit: Int,
    private val score: (Part) -> Double,
) {

    fun excess(measured: Measured): String? {
        val total = measured.total(score)
        if (total - limit <= TOLERANCE) return null
        return "`${measured.name}` has a weighted complexity of ${total.oneDecimal()} " +
            "(limit ${limit.toDouble().oneDecimal()}, $label bucket). ${measured.details(score)}"
    }

    private companion object {

        const val TOLERANCE = 1e-9
    }
}
