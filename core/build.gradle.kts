plugins {
    id("code-coverage")
    id("jvm-lib")
    id("static-analysis")
}

dependencies {
    compileOnly(libs.detekt.api)
}
