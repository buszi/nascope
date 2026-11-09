plugins {
    alias(libs.plugins.convention.kmp.library)
    alias(libs.plugins.convention.kmp.compose)
}

kotlin {
    android {
        namespace = "io.buszi.nascope.resources"

        androidResources.enable = true
    }

    sourceSets {
        commonMain.dependencies {
            api(libs.compose.components.resources)
        }
    }
}

compose.resources {
    publicResClass = true
    packageOfResClass = "io.buszi.nascope.resources"
    generateResClass = always
}
