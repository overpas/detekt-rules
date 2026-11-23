package by.overpas.detekt.architecture

import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtElement

internal data class StoreReference(
    val element: KtElement,
    val store: FqName,
    val type: String,
)
