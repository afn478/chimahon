import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi

plugins {
    id("mihon.library")
    kotlin("multiplatform")
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
                api(project.dependencies.platform(kotlinx.coroutines.bom))
                api(kotlinx.coroutines.core)
                api(libs.okio)
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
            }
        }
        val androidMain by getting {
            dependencies {
                implementation(androidx.workmanager)
                implementation(project.dependencies.platform(kotlinx.coroutines.bom))
                implementation(kotlinx.coroutines.core)
                implementation(libs.bundles.js.engine)
            }
        }
        val desktopMain by getting {
            dependencies {
                implementation(libs.rhino)
            }
        }
        val desktopTest by getting {
            dependencies {
                implementation(project.dependencies.platform(kotlinx.coroutines.bom))
                implementation(kotlinx.coroutines.core)
                implementation(kotlinx.coroutines.test)
                implementation(libs.bundles.test)
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
        val linuxTest by getting {
            dependencies {
                implementation(kotlin("test"))
            }
        }
        val windowsTest by getting {
            dependencies {
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
    namespace = "tachiyomi.core.platform"
}
