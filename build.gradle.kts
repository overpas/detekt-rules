plugins {
    alias(libs.plugins.kotlin.jvm)
    id("code-coverage")
    id("static-analysis")
}

dependencies {
    compileOnly(libs.detekt.api)

    testImplementation(libs.detekt.test)
    testImplementation(libs.kotlin.test)
}
