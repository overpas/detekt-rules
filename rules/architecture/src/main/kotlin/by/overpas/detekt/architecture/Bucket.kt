package by.overpas.detekt.architecture

import java.util.Locale

internal class Bucket(
    private val label: String,
    private val limit: Int,
    private val score: (Part) -> Double,
) {

    fun excess(scored: ScoredParts): String? {
        val total = scored.parts.sumOf(score)
        if (total - limit <= TOLERANCE) return null
        val largest = scored.parts
            .asSequence()
            .filter { score(it) > 0 }
            .sortedByDescending(score)
            .take(LISTED_PARTS)
            .joinToString { "`${it.name}` ${format(score(it))}" }
        return "`${scored.name}` has a weighted complexity of ${format(total)} " +
            "(limit ${format(limit.toDouble())}, $label bucket). Largest parts: $largest."
    }

    private fun format(value: Double): String =
        String.format(Locale.ROOT, "%.1f", value)

    private companion object {

        const val TOLERANCE = 1e-9
        const val LISTED_PARTS = 3
    }
}
