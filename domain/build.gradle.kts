plugins {
    alias(libs.plugins.convention.kmp.library)
    alias(libs.plugins.koin.compiler)
}

kotlin {
    android {
        namespace = "io.buszi.nascope.domain"
    }

    sourceSets {
        commonMain.dependencies {
            api(projects.domainModel)
            api(libs.kotlinx.coroutines.core)

            implementation(projects.network)
            implementation(projects.storage)

            implementation(libs.kotlin.result)
            implementation(libs.koin.core)
            implementation(libs.koin.annotations)
        }

        jvmTest.dependencies {
            implementation(projects.testUtils)
        }
    }
}
