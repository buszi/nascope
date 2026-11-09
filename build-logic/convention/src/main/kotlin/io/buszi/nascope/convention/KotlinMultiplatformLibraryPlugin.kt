package io.buszi.nascope.convention

import com.android.build.api.dsl.KotlinMultiplatformAndroidLibraryTarget
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.invoke
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

class KotlinMultiplatformLibraryPlugin : Plugin<Project> {

    override fun apply(target: Project) = with(target) {
        with(pluginManager) {
            apply(libs.findPlugin("kotlinMultiplatform"))
            apply(libs.findPlugin("androidMultiplatformLibrary"))
        }

        extensions.configure<KotlinMultiplatformExtension> {
            compilerOptions {
                freeCompilerArgs.addAll("-Xexpect-actual-classes")
            }

            extensions.configure<KotlinMultiplatformAndroidLibraryTarget>("android") {
                compileSdk = libs.findVersionInt("android-compileSdk")
                minSdk = libs.findVersionInt("android-minSdk")

                compilerOptions {
                    jvmTarget.set(JVM_TARGET)
                }
            }

            jvm()

            sourceSets {
                named("jvmTest") {
                    dependencies {
                        implementation(libs.findLibrary("junit").get())
                        implementation(libs.findLibrary("kotlin-test").get())
                        implementation(libs.findLibrary("kotlinx-coroutines-test").get())
                        implementation(libs.findLibrary("kotlinx-coroutinesSwing").get())
                    }
                }
            }
        }
    }
}
