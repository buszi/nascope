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

            val releaseSigning = releaseSigningConfig(this@with)

            buildTypes {
                getByName("release") {
                    signingConfig = releaseSigning
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

        warnIfReleaseIsUnsigned()

        extensions.configure<KotlinAndroidProjectExtension> {
            compilerOptions {
                jvmTarget.set(JVM_TARGET)
            }
        }
    }
}

private const val KEYSTORE_FILE = "NASCOPE_KEYSTORE_FILE"
private const val KEYSTORE_PASSWORD = "NASCOPE_KEYSTORE_PASSWORD"
private const val KEY_ALIAS = "NASCOPE_KEY_ALIAS"
private const val KEY_PASSWORD = "NASCOPE_KEY_PASSWORD"

private val SIGNING_VARIABLES = listOf(KEYSTORE_FILE, KEYSTORE_PASSWORD, KEY_ALIAS, KEY_PASSWORD)

/** Environment first (CI), then Gradle properties (`~/.gradle/gradle.properties`); blank counts as unset. */
private fun Project.signingValue(name: String): String? =
    providers.environmentVariable(name).orElse(providers.gradleProperty(name)).orNull?.takeIf { it.isNotBlank() }

/** Why the release signing config cannot be built, or `null` when it can. */
private fun Project.releaseSigningProblem(): String? {
    val missing = SIGNING_VARIABLES.filter { signingValue(it) == null }
    if (missing.isNotEmpty()) return "missing ${missing.joinToString()}"

    val keystore = rootProject.file(signingValue(KEYSTORE_FILE)!!)
    if (!keystore.isFile) return "$KEYSTORE_FILE points to a missing file: $keystore"
    return null
}

/** The release signing config, or `null` when the secrets are not (fully) available. */
private fun ApplicationExtension.releaseSigningConfig(project: Project) = with(project) {
    if (releaseSigningProblem() != null) return@with null

    signingConfigs.create("release") {
        storeFile = rootProject.file(signingValue(KEYSTORE_FILE)!!)
        storePassword = signingValue(KEYSTORE_PASSWORD)
        keyAlias = signingValue(KEY_ALIAS)
        keyPassword = signingValue(KEY_PASSWORD)
    }
}

/** Warns when (and only when) a release APK is actually packaged without a release key. */
private fun Project.warnIfReleaseIsUnsigned() {
    val problem = releaseSigningProblem() ?: return
    val message = "Release APK is NOT signed ($problem). See docs/release.md."

    tasks.matching { it.name == "packageRelease" }.configureEach {
        doFirst { logger.warn("WARNING: $message") }
    }
}
