import org.gradle.kotlin.dsl.`java-gradle-plugin`
import org.gradle.kotlin.dsl.`kotlin-dsl`

plugins {
    `java-gradle-plugin`
    `kotlin-dsl`
}

gradlePlugin {
    plugins {
        register("AndroidApplicationPlugin") {
            id = "plugins.android.application"
            version = libs.versions.plugins.get()
            implementationClass = "io.buszi.nascope.convention.AndroidApplicationPlugin"
        }
        register("DesktopApplicationPlugin") {
            id = "plugins.desktop.application"
            version = libs.versions.plugins.get()
            implementationClass = "io.buszi.nascope.convention.DesktopApplicationPlugin"
        }
        register("KotlinMultiplatformLibraryPlugin") {
            id = "plugins.kmp.library"
            version = libs.versions.plugins.get()
            implementationClass = "io.buszi.nascope.convention.KotlinMultiplatformLibraryPlugin"
        }
        register("ComposeMultiplatformPlugin") {
            id = "plugins.kmp.compose"
            version = libs.versions.plugins.get()
            implementationClass = "io.buszi.nascope.convention.ComposeMultiplatformPlugin"
        }
    }
}

dependencies {
    implementation(gradleApi())
    implementation(libs.plgn.kotlin)
    implementation(libs.plgn.kotlin.compose.compiler)
    implementation(libs.plgn.android.application)
    implementation(libs.plgn.android.kmp.library)
    implementation(libs.plgn.compose.multiplatform)
}
