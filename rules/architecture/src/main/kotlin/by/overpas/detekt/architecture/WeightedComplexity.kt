package by.overpas.detekt.architecture

import dev.detekt.api.Config
import dev.detekt.api.Configuration
import dev.detekt.api.Entity
import dev.detekt.api.Finding
import dev.detekt.api.Rule
import dev.detekt.api.config
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtEnumEntry
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtObjectDeclaration
import java.nio.file.FileSystems
import java.nio.file.PathMatcher
import kotlin.io.path.Path

class WeightedComplexity(config: Config) :
    Rule(
        config,
        "A class or a file must not be too complex. " +
            "The rule adds a weight for each property, function and call, and more for nested and chained calls " +
            "and for many parameters. Split the class or the file into smaller parts.",
    ) {

    @Configuration("maximum weighted complexity of a class")
    private val classLimit: Int by config(100)

    @Configuration("maximum weighted complexity of a file")
    private val fileLimit: Int by config(150)

    @Configuration("maximum weighted complexity of a class in the test sources")
    private val testClassLimit: Int by config(550)

    @Configuration("maximum weighted complexity of a file in the test sources")
    private val testFileLimit: Int by config(600)

    @Configuration("maximum weighted complexity of the composable functions and getters of a class")
    private val composableClassLimit: Int by config(5)

    @Configuration("maximum weighted complexity of the composable functions and getters of a file")
    private val composableFileLimit: Int by config(410)

    @Configuration("weight that a call gets for each level of nesting")
    private val nestWeight: Double by decimalConfig(0.25)

    @Configuration("weight that a call gets for each earlier call in its chain")
    private val chainWeight: Double by decimalConfig(0.1)

    @Configuration("score of a function without calls and with at most one parameter")
    private val minFunctionScore: Double by decimalConfig(1.0)

    @Configuration("count the type arguments and the parts of a function type in a property type, else a type is 1")
    private val arePropertyTypeArgumentsCounted: Boolean by config(true)

    @Configuration("score that a nested class that is not inner adds to its parent class")
    private val nestedClassScore: Double by decimalConfig(1.0)

    @Configuration("path patterns of the test sources, which use the test limits")
    private val testSourcePatterns: List<PathMatcher> by config(
        listOf("**/src/test/**", "**/src/*Test/**"),
    ) { patterns ->
        patterns.map { FileSystems.getDefault().getPathMatcher("glob:$it") }
    }

    @Configuration("annotations that put a function or a property getter in the composable bucket")
    private val composableAnnotations: Set<String> by config(listOf("Composable")) { it.toSet() }

    private val cost by lazy {
        MemberCost(
            composableAnnotations,
            Scoring(
                nestWeight = nestWeight,
                chainWeight = chainWeight,
                minFunctionScore = minFunctionScore,
                arePropertyTypeArgumentsCounted = arePropertyTypeArgumentsCounted,
                nestedClassScore = nestedClassScore,
            ),
        )
    }

    private val classLimits by lazy { Limits(classLimit, testClassLimit, composableClassLimit) }

    private val fileLimits by lazy { Limits(fileLimit, testFileLimit, composableFileLimit) }

    override fun visitKtFile(file: KtFile) {
        super.visitKtFile(file)
        FileParts(file, cost).reportExcess(Entity.atPackageOrFirstDecl(file), fileLimits.buckets(file.isTest()))
    }

    override fun visitClassOrObject(classOrObject: KtClassOrObject) {
        super.visitClassOrObject(classOrObject)
        if (classOrObject is KtEnumEntry || (classOrObject is KtObjectDeclaration && classOrObject.isObjectLiteral())) {
            return
        }
        ClassParts(classOrObject, cost).reportExcess(
            Entity.atName(classOrObject),
            classLimits.buckets(classOrObject.containingKtFile.isTest()),
        )
    }

    private fun ScoredParts.reportExcess(
        entity: Entity,
        buckets: List<Bucket>,
    ) {
        buckets
            .mapNotNull { it.excess(this) }
            .forEach { report(Finding(entity, it)) }
    }

    private fun KtFile.isTest(): Boolean {
        val path = Path(virtualFilePath)
        return testSourcePatterns.any { it.matches(path) }
    }
}
