package io.buszi.nascope.convention

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.findByType
import org.jetbrains.compose.ComposeBuildConfig.composeMaterial3Version
import org.jetbrains.compose.ComposeBuildConfig.composeVersion
import org.jetbrains.kotlin.compose.compiler.gradle.ComposeCompilerGradlePluginExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

class ComposeMultiplatformPlugin : Plugin<Project> {

    override fun apply(target: Project) = with(target) {
        with(pluginManager) {
            apply(libs.findPlugin("composeMultiplatform"))
            apply(libs.findPlugin("composeCompiler"))
        }

        extensions.configure<ComposeCompilerGradlePluginExtension> {
            includeSourceInformation.set(true)
        }

        if (extensions.findByType(KotlinMultiplatformExtension::class) != null) {
            // KMP module, apply dependencies
            extensions.configure<KotlinMultiplatformExtension> {
                sourceSets.named("commonMain").dependencies {
                    implementation("org.jetbrains.compose.runtime:runtime:$composeVersion")
                    implementation("org.jetbrains.compose.foundation:foundation:$composeVersion")
                    implementation("org.jetbrains.compose.material3:material3") {
                        version { strictly(composeMaterial3Version) }
                    }
                    implementation("org.jetbrains.compose.ui:ui:$composeVersion")
                    implementation("org.jetbrains.compose.ui:ui-tooling-preview:$composeVersion")
                }
            }

            dependencies {
                add("androidRuntimeClasspath", "org.jetbrains.compose.ui:ui-tooling:$composeVersion")
            }
        }
    }
}
