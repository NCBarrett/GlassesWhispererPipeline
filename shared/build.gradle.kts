plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    id("org.jetbrains.kotlin.plugin.serialization")
}

kotlin {
    jvm("desktop") {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
        }
    }

    sourceSets {
        val desktopMain by getting {
            dependencies {
                implementation(compose.desktop.currentOs)

                // Clean modern multiplatform dependencies properties
                implementation(compose.runtime)
                implementation(compose.foundation)
                implementation(compose.material)

                // 🎨 Explicitly add the Material Design extended icon set artifact
                implementation(compose.materialIconsExtended)

                // 🗄️ Native SQLite Core Driver Configuration
                implementation("org.xerial:sqlite-jdbc:3.46.1.3")

                // 📡 Hybrid Network Pipeline Extensions
                implementation("io.ktor:ktor-client-core:2.3.12")
                implementation("io.ktor:ktor-client-cio:2.3.12")
                implementation("io.ktor:ktor-client-content-negotiation:2.3.12")
                implementation("io.ktor:ktor-serialization-kotlinx-json:2.3.12")
            }
        }
    }
}
