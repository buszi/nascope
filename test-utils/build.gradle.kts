plugins {
    alias(libs.plugins.convention.kmp.library)
}

kotlin {
    android {
        namespace = "io.buszi.nascope.test"
    }

    sourceSets {
        jvmMain.dependencies {
            api(libs.junit)
            api(libs.kotlinx.coroutines.test)
            api(libs.turbine)
        }
    }
}
