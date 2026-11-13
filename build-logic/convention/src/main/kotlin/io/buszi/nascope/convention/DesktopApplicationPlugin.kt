package io.buszi.nascope.convention

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.JavaExec
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

        // Full logging for `./gradlew run`; packaged distributions do not go through JavaExec and stay quiet.
        tasks.withType(JavaExec::class.java).configureEach {
            systemProperty("nascope.debug", "true")
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

                        // The user-visible name: the macOS bundle ("Nascope.app"), the Windows Start menu entry,
                        // the Linux package name (lowercased by the deb tooling).
                        packageName = "Nascope"
                        packageVersion = libs.findVersionString("app-version")
                        description = "Live telemetry for your NAS, from a Glances server"
                        vendor = "Nascope"
                        copyright = "Copyright (c) 2026 Nascope"

                        // The packaged runtime is a jlinked JRE with only the modules the build can see. Modules that
                        // dependencies load reflectively are missed and fail at runtime, not at build time, so they
                        // are listed here. The list is the output of `./gradlew :desktopApp:suggestRuntimeModules`:
                        // re-run it after adding or upgrading a dependency and update it.
                        modules("java.instrument", "java.management", "jdk.security.auth", "jdk.unsupported")

                        val icons = project.layout.projectDirectory.dir("icons")

                        macOS {
                            bundleID = "io.buszi.nascope"
                            iconFile.set(icons.file("nascope.icns"))
                        }

                        windows {
                            iconFile.set(icons.file("nascope.ico"))
                            menuGroup = "Nascope"
                            shortcut = true
                            // Per user: no administrator prompt, installs under %LOCALAPPDATA%.
                            perUserInstall = true
                            // Identifies the product across versions: a newer MSI upgrades an older one in place only
                            // while this stays the same. Generated once. NEVER change it after a release.
                            upgradeUuid = "942EBB92-5DC0-443C-87E0-0E71F9B96EFD"
                        }

                        linux {
                            iconFile.set(icons.file("nascope.png"))
                            menuGroup = "Network;Monitor;"
                            appCategory = "net"
                            // The deb Maintainer field needs an e-mail address. Releases get the real one from the
                            // NASCOPE_DEB_MAINTAINER environment variable (a CI secret) or Gradle property, as
                            // "Name <mail>"; local builds use the placeholder.
                            debMaintainer = project.providers.environmentVariable("NASCOPE_DEB_MAINTAINER")
                                .orElse(project.providers.gradleProperty("NASCOPE_DEB_MAINTAINER"))
                                .orElse("Nascope <nascope@example.invalid>")
                                .get()
                        }
                    }
                }
            }
        }
    }
}
