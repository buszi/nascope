plugins {
    alias(libs.plugins.convention.kmp.library)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.koin.compiler)
}

kotlin {
    android {
        namespace = "io.buszi.nascope.network"
    }

    sourceSets {
        commonMain.dependencies {
            api(projects.domainModel)

            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.client.logging)
            implementation(libs.ktor.serialization.kotlinx.json)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.napier)

            implementation(libs.koin.core)
            implementation(libs.koin.annotations)
        }

        androidMain.dependencies {
            implementation(libs.ktor.client.okhttp)
        }

        jvmMain.dependencies {
            implementation(libs.ktor.client.okhttp)
        }

        jvmTest.dependencies {
            implementation(libs.ktor.client.mock)
        }
    }
}
