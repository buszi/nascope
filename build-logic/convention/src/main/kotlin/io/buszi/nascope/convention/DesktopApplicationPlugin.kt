package io.buszi.nascope.convention

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.jetbrains.compose.ComposeExtension
import org.jetbrains.compose.desktop.DesktopExtension
import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension

class DesktopApplicationPlugin : Plugin<Project> {

    override fun apply(target: Project) = with(target) {
        with(pluginManager) {
            apply(libs.findPlugin("kotlinJvm"))
        }

        extensions.configure<KotlinJvmProjectExtension> {
            compilerOptions {
                jvmTarget.set(JVM_TARGET)
            }
        }

        extensions.configure<ComposeExtension> {
            extensions.configure<DesktopExtension>("desktop") {
                application {
                    mainClass = "io.buszi.nascope.MainKt"

                    nativeDistributions {
                        targetFormats(
                            TargetFormat.Dmg,
                            TargetFormat.Msi,
                            TargetFormat.Deb,
                        )

                        packageName = "io.buszi.nascope"
                        packageVersion = libs.findVersionString("app-version")

                        val icons = project.layout.projectDirectory.dir("icons")
                        macOS { iconFile.set(icons.file("nascope.icns")) }
                        windows { iconFile.set(icons.file("nascope.ico")) }
                        linux { iconFile.set(icons.file("nascope.png")) }
                    }
                }
            }
        }
    }
}
