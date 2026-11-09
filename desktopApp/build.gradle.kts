plugins {
    alias(libs.plugins.convention.kmp.compose)
    alias(libs.plugins.convention.desktop.application)
}

dependencies {
    implementation(projects.shared)

    implementation(compose.desktop.currentOs)
    implementation(libs.kotlinx.coroutinesSwing)
}
