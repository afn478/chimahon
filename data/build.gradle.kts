import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi

plugins {
    id("mihon.library")
    kotlin("multiplatform")
    kotlin("plugin.serialization")
    alias(libs.plugins.sqldelight)
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
                api(projects.core.database)
                api(projects.domain)
                api(project.dependencies.platform(kotlinx.coroutines.bom))
                api(kotlinx.coroutines.core)
                api(libs.sqldelight.runtime)
                implementation(kotlinx.serialization.json)
            }
        }
        val androidMain by getting {
            kotlin.srcDir("src/main/java")
            dependencies {
                implementation(projects.sourceApi)
                implementation(projects.core.common)

                api(libs.bundles.sqldelight)
            }
        }
        val desktopMain by getting {
            dependencies {
                implementation(libs.sqldelight.coroutines)
            }
        }
        val desktopTest by getting {
            dependencies {
                implementation(libs.bundles.test)
                runtimeOnly(libs.junit.platform.launcher)
            }
        }
        val nativeTest by getting {
            dependencies {
                implementation(kotlin("test"))
            }
        }
    }

    @OptIn(ExperimentalKotlinGradlePluginApi::class)
    compilerOptions {
        freeCompilerArgs.add(
            "-opt-in=kotlinx.serialization.ExperimentalSerializationApi",
        )
    }
}

android {
    namespace = "tachiyomi.data"

    defaultConfig {
        consumerProguardFiles("consumer-rules.pro")
    }
}

sqldelight {
    databases {
        create("Database") {
            packageName.set("tachiyomi.data")
            dialect(libs.sqldelight.dialects.sql)
            schemaOutputDirectory.set(project.file("./src/commonMain/sqldelight"))
        }
    }
}
