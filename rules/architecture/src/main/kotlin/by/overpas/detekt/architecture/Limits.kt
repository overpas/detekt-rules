package by.overpas.detekt.architecture

internal class Limits(
    private val normal: Int,
    private val test: Int,
    private val composable: Int,
) {

    fun buckets(isTest: Boolean): List<Bucket> =
        listOf(
            if (isTest) Bucket("test", test, Part::normal) else Bucket("normal", normal, Part::normal),
            Bucket("composable", composable, Part::composable),
        )
}
