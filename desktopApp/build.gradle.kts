import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    // 1. Keep the multiplatform plugin definitions that match your master settings classpath
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

kotlin {
    jvm("desktop") {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
        }
    }

    // 2. 🎯 THE SECRET WEAPON: Explicitly point the compiler to your exact src/main/kotlin folder path
    sourceSets {
        val desktopMain by getting {
            kotlin.srcDirs("src/main/kotlin")
            dependencies {
                implementation(project(":shared")) // Relational link to your database and layouts
                implementation(compose.desktop.currentOs)
                implementation(libs.kotlinx.coroutinesSwing)
                implementation(libs.compose.uiToolingPreview)
            }
        }
    }
}

compose.desktop {
    application {
        mainClass = "glasseswhisperer.pipeline.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Msi, TargetFormat.Deb)
            packageName = "GlassesWhispererPipeline"
            packageVersion = "1.0.0"
        }
    }
}
