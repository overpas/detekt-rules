package by.overpas.detekt.architecture

import org.jetbrains.kotlin.psi.KtFile
import java.nio.file.FileSystems
import java.nio.file.PathMatcher
import kotlin.io.path.Path

internal class TestSources(patterns: List<String>) {

    private val matchers: List<PathMatcher> = patterns.map { FileSystems.getDefault().getPathMatcher("glob:$it") }

    operator fun contains(file: KtFile): Boolean {
        val path = Path(file.virtualFilePath)
        return matchers.any { it.matches(path) }
    }
}
