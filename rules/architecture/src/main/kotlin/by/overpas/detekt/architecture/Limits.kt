package by.overpas.detekt.architecture

import org.jetbrains.kotlin.psi.KtFile

internal class Limits(
    private val normal: Int,
    private val test: Int,
    private val composable: Int,
    private val testSources: TestSources,
) {

    fun buckets(file: KtFile): List<Bucket> =
        listOf(
            if (file in testSources) Bucket("test", test, Part::normal) else Bucket("normal", normal, Part::normal),
            Bucket("composable", composable, Part::composable),
        )
}
