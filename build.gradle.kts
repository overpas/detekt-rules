plugins {
    id("code-coverage")
    id("static-analysis")
    alias(libs.plugins.kotlin.jvm)
}

dependencies {
    compileOnly(libs.detekt.api)

    testImplementation(libs.detekt.test)
    testImplementation(libs.kotlin.test)
}
