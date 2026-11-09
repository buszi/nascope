package io.buszi.nascope.convention

import com.android.build.api.dsl.ApplicationExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.jetbrains.kotlin.gradle.dsl.KotlinAndroidProjectExtension

class AndroidApplicationPlugin : Plugin<Project> {

    override fun apply(target: Project) = with(target) {
        with(pluginManager) {
            apply(libs.findPlugin("androidApplication"))
        }

        extensions.configure<ApplicationExtension> {
            namespace = "io.buszi.nascope"
            compileSdk = libs.findVersionInt("android-compileSdk")

            defaultConfig {
                applicationId = "io.buszi.nascope"
                minSdk = libs.findVersionInt("android-minSdk")
                targetSdk = libs.findVersionInt("android-targetSdk")
                versionCode = System.getenv("VERSION_CODE")?.toIntOrNull() ?: 1
                versionName = libs.findVersionString("app-version")
            }

            packaging {
                resources {
                    excludes += "/META-INF/{AL2.0,LGPL2.1}"
                }
            }

            buildFeatures {
                compose = true
                buildConfig = true
                resValues = true
            }

            compileOptions {
                sourceCompatibility = JDK_VERSION
                targetCompatibility = JDK_VERSION
            }

            buildTypes {
                getByName("release") {
                    // TODO setup signing
                    signingConfig = signingConfigs.getByName("debug")
                    isMinifyEnabled = true
                    isShrinkResources = true
                    isDebuggable = false

                    proguardFiles(
                        getDefaultProguardFile("proguard-android-optimize.txt"),
                        "proguard-rules.pro"
                    )
                }

                getByName("debug") {
                    isDefault = true
                    versionNameSuffix = "-debug"
                    signingConfig = signingConfigs.getByName("debug")
                    isMinifyEnabled = false
                    isShrinkResources = false
                    isDebuggable = true
                }
            }
        }

        extensions.configure<KotlinAndroidProjectExtension> {
            compilerOptions {
                jvmTarget.set(JVM_TARGET)
            }
        }
    }
}
