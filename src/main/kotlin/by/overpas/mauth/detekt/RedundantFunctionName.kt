package by.overpas.mauth.detekt

import dev.detekt.api.Config
import dev.detekt.api.Configuration
import dev.detekt.api.Entity
import dev.detekt.api.Finding
import dev.detekt.api.Rule
import dev.detekt.api.config
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtObjectDeclaration
import org.jetbrains.kotlin.psi.psiUtil.containingClassOrObject

class RedundantFunctionName(config: Config) :
    Rule(
        config,
        "A function name must not repeat the subject that the name of its class already gives. " +
            "In `UserRepository`, `get` is enough and `getUser` is redundant.",
    ) {

    @Configuration("words that a function name can repeat from the class name")
    private val ignoredWords: Set<String> by config(
        listOf("And", "At", "By", "For", "From", "In", "Of", "On", "Or", "To", "With"),
    ) { it.toSet() }

    override fun visitNamedFunction(function: KtNamedFunction) {
        super.visitNamedFunction(function)
        val className = function.checkedClassName()
        val functionName = function.name
        if (className == null || functionName == null) return
        val phrase = className.subjectPhrases().firstOrNull { functionName.words().containsPhrase(it) } ?: return
        val subject = phrase.joinToString("")
        report(
            Finding(
                Entity.atName(function),
                "`$functionName` repeats `$subject` from `$className`. Remove `$subject` from the name.",
            ),
        )
    }

    private fun KtNamedFunction.checkedClassName(): String? =
        containingClassOrObject
            ?.takeUnless { it is KtObjectDeclaration && it.isCompanion() }
            ?.takeUnless { hasModifier(KtTokens.OVERRIDE_KEYWORD) || hasModifier(KtTokens.PRIVATE_KEYWORD) }
            ?.name

    private fun String.subjectPhrases(): List<List<String>> {
        val words = words()
        val subject = if (words.size > 1) words.dropLast(1) else words
        return subject.indices
            .map { subject.drop(it) }
            .filterNot { phrase -> phrase.all { it in ignoredWords } }
    }

    private fun List<String>.containsPhrase(phrase: List<String>): Boolean =
        windowed(phrase.size).any { window ->
            window.dropLast(1).zip(phrase.dropLast(1)).all { (word, subjectWord) ->
                word.equals(subjectWord, ignoreCase = true)
            } && window.last().isFormOf(phrase.last())
        }

    private fun String.isFormOf(word: String): Boolean =
        listOf(word, "${word}s", "${word}es").any { equals(it, ignoreCase = true) }

    private fun String.words(): List<String> =
        WORD_PATTERN.findAll(this).map { it.value }.toList()

    private companion object {
        val WORD_PATTERN = Regex("[A-Z]+(?![a-z])|[A-Z]?[a-z0-9]+")
    }
}
