import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi

plugins {
    id("mihon.library")
    kotlin("multiplatform")
    kotlin("plugin.serialization")
}

kotlin {
    androidTarget()
    jvm("desktop")
    mingwX64("windows")
    linuxX64("linux")
    iosX64()
    iosArm64()
    iosSimulatorArm64()

    applyDefaultHierarchyTemplate()

    sourceSets {
        val commonMain by getting {
            dependencies {
                api(projects.core.platform)
                api(kotlinx.serialization.json)
            }
        }
        val desktopTest by getting {
            dependencies {
                implementation(libs.bundles.test)
                implementation(project.dependencies.platform(kotlinx.coroutines.bom))
                implementation(kotlinx.coroutines.core)
                runtimeOnly(libs.junit.platform.launcher)
            }
        }
        val iosTest by getting {
            dependencies {
                implementation(project.dependencies.platform(kotlinx.coroutines.bom))
                implementation(kotlinx.coroutines.core)
                implementation(kotlin("test"))
            }
        }
    }

    @OptIn(ExperimentalKotlinGradlePluginApi::class)
    compilerOptions {
        freeCompilerArgs.add("-Xexpect-actual-classes")
    }
}

android {
    namespace = "tachiyomi.core.extensions"
}
