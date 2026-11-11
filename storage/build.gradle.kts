plugins {
    alias(libs.plugins.convention.kmp.library)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.koin.compiler)
}

kotlin {
    android {
        namespace = "io.buszi.nascope.storage"
    }

    sourceSets {
        commonMain.dependencies {
            api(projects.domainModel)
            api(libs.kotlinx.coroutines.core)

            implementation(libs.androidx.datastore.preferences.core)
            implementation(libs.kotlinx.serialization.json)

            implementation(libs.koin.core)
            implementation(libs.koin.annotations)
        }

        jvmMain.dependencies {
            implementation(libs.java.keyring)
        }
    }
}
