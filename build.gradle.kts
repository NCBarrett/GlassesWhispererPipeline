plugins {
    alias(libs.plugins.kotlinMultiplatform) apply false
    alias(libs.plugins.composeMultiplatform) apply false
    alias(libs.plugins.composeCompiler) apply false
    id("org.jetbrains.kotlin.plugin.serialization") version "2.0.20" apply false
}
