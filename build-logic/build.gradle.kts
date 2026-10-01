import org.gradle.plugin.use.PluginDependency

plugins {
    `kotlin-dsl`
}

dependencies {
    implementation(files(libs.javaClass.superclass.protectionDomain.codeSource.location))

    implementation(libs.plugins.detekt.toDep())
    implementation(libs.plugins.kotlin.jvm.toDep())
    implementation(libs.plugins.kover.toDep())
    implementation(libs.plugins.shadow.toDep())
}

fun Provider<PluginDependency>.toDep() = map {
    "${it.pluginId}:${it.pluginId}.gradle.plugin:${it.version}"
}
