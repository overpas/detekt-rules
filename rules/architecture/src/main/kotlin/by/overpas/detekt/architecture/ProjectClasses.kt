package by.overpas.detekt.architecture

import org.jetbrains.kotlin.analysis.api.symbols.KaClassSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaSymbolOrigin
import org.jetbrains.kotlin.psi.KtFile

internal class ProjectClasses(
    file: KtFile,
    depth: Int,
) {

    private val root = file.packageFqName.pathSegments().take(depth)

    operator fun contains(symbol: KaClassSymbol): Boolean {
        val segments = symbol.classId?.run { packageFqName.pathSegments() }.orEmpty()
        return symbol.origin == KaSymbolOrigin.SOURCE || (root.isNotEmpty() && segments.take(root.size) == root)
    }
}
