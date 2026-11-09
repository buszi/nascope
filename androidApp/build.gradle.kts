plugins {
    alias(libs.plugins.convention.android.application)
    alias(libs.plugins.convention.kmp.compose)
}

dependencies {
    implementation(projects.shared)

    implementation(libs.androidx.activity.compose)
}
