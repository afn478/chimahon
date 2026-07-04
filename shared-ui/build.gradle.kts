import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget

plugins {
    id("mihon.library")
    kotlin("multiplatform")
    id("org.jetbrains.kotlin.plugin.compose")
}

val composeMultiplatformVersion = compose.versions.multiplatform.get()

kotlin {
    androidTarget()
    jvm("desktop") {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }
    iosArm64()
    iosSimulatorArm64()

    targets.withType<KotlinNativeTarget>().configureEach {
        binaries.framework {
            baseName = "ChimahonShared"
            isStatic = true
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(projects.core.database)
            implementation(projects.core.extensions)
            implementation(projects.core.platform)
            implementation(projects.data)
            implementation(projects.sourceApi)
            implementation(project.dependencies.platform(kotlinx.coroutines.bom))
            implementation(kotlinx.coroutines.core)
            implementation("io.ktor:ktor-client-core:3.5.0")
            implementation("io.ktor:ktor-client-cio:3.5.0")
            implementation("org.jetbrains.compose.runtime:runtime:$composeMultiplatformVersion")
            implementation("org.jetbrains.compose.foundation:foundation:$composeMultiplatformVersion")
            // JetBrains last published the multiplatform extended icon pack at 1.7.3.
            implementation("org.jetbrains.compose.material:material-icons-extended:1.7.3")
            implementation("org.jetbrains.compose.material:material:$composeMultiplatformVersion")
            implementation("org.jetbrains.compose.ui:ui:$composeMultiplatformVersion")
            implementation("org.jetbrains.compose.components:components-resources:$composeMultiplatformVersion")
        }
        val androidMain by getting {
            dependencies {
                implementation(androidx.workmanager)
                implementation(libs.sqldelight.android.driver)
                implementation(libs.sqlite.framework)
            }
        }
        val desktopMain by getting {
            dependencies {
                implementation("de.femtopedia.dex2jar:dex-translator:2.4.36")
                implementation("de.femtopedia.dex2jar:dex-tools:2.4.36")
                implementation("net.dongliu:apk-parser:2.6.10")
                implementation("org.ow2.asm:asm:9.9.1")
                implementation("org.ow2.asm:asm-commons:9.9.1")
            }
        }
        val desktopTest by getting {
            dependencies {
                implementation(kotlin("test"))
                implementation(project.dependencies.platform(kotlinx.coroutines.bom))
                implementation(kotlinx.coroutines.core)
            }
        }
    }

    @OptIn(ExperimentalKotlinGradlePluginApi::class)
    compilerOptions {
        freeCompilerArgs.add("-Xexpect-actual-classes")
    }
}

android {
    namespace = "app.chimahon.shared"
}
