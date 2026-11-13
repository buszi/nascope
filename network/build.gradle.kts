import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType

plugins {
    alias(libs.plugins.convention.kmp.library)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.koin.compiler)
}

kotlin {
    android {
        namespace = "io.buszi.nascope.network"
    }

    // Android and desktop both run OkHttp on the JVM's TLS stack: their HTTP client, TLS trust and exception
    // mapping live once in `jvmSharedMain`.
    @OptIn(ExperimentalKotlinGradlePluginApi::class)
    applyDefaultHierarchyTemplate {
        common {
            group("jvmShared") {
                withJvm()
                withCompilations { it.platformType == KotlinPlatformType.androidJvm }
            }
        }
    }

    sourceSets {
        commonMain.dependencies {
            api(projects.domainModel)
            api(libs.kotlin.result)

            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.client.logging)
            implementation(libs.ktor.serialization.kotlinx.json)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.napier)

            implementation(libs.koin.core)
            implementation(libs.koin.annotations)
        }

        named("jvmSharedMain").dependencies {
            implementation(libs.ktor.client.okhttp)
        }

        jvmTest.dependencies {
            implementation(libs.ktor.client.mock)
            implementation(libs.okhttp.tls)
            implementation(libs.okhttp.mockwebserver3)
        }
    }
}
