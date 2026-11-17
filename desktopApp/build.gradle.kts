plugins {
    alias(libs.plugins.convention.kmp.compose)
    alias(libs.plugins.convention.desktop.application)
}

dependencies {
    implementation(projects.shared)

    implementation(compose.desktop.currentOs)
    implementation(libs.kotlinx.coroutinesSwing)
    implementation(libs.jna)
    implementation(libs.jna.platform)
    implementation(libs.napier)
}

// The offscreen snapshot harness (docs/desktop-snapshots.md): renders the real app headless into PNGs, for checking
// the desktop UI without a window or any OS permission. A source set of its own, so it never ships in a package.
val snapshot: SourceSet = sourceSets.create("snapshot") {
    compileClasspath += sourceSets.main.get().output
    runtimeClasspath += sourceSets.main.get().output
}
configurations.named(snapshot.implementationConfigurationName) {
    extendsFrom(configurations.implementation.get())
}
configurations.named(snapshot.runtimeOnlyConfigurationName) {
    extendsFrom(configurations.runtimeOnly.get())
}
// Compiled with the app, so an API change that breaks the harness shows up in `./gradlew assemble`.
tasks.named("assemble") {
    dependsOn(snapshot.classesTaskName)
}

dependencies {
    "snapshotImplementation"(projects.domain)
    // Only for SecretStore: the harness swaps the OS keychain for an in-memory store.
    "snapshotImplementation"(projects.storage)
    "snapshotImplementation"(libs.compose.ui.test)
    "snapshotImplementation"(libs.androidx.lifecycle.runtimeCompose)
    "snapshotImplementation"(libs.androidx.lifecycle.viewmodelCompose)
    "snapshotImplementation"(libs.koin.core)
}

/**
 * `./gradlew :desktopApp:snapshot -Psnapshot.script=<file> [-Psnapshot.out=<dir>] [-Psnapshot.home=<dir>] [-Psnapshot.density=<n>]`
 *
 * Runs a snapshot script (see `SnapshotScript`) against a scratch home directory, never the user's real settings.
 */
tasks.register<JavaExec>("snapshot") {
    group = "verification"
    description = "Renders the desktop app offscreen from a snapshot script into PNGs."
    classpath = snapshot.runtimeClasspath
    mainClass.set("io.buszi.nascope.snapshot.SnapshotMainKt")
    // Headless: no window, no dock icon, nothing on the user's screen.
    systemProperty("java.awt.headless", "true")

    // Relative paths are taken from the root project directory.
    val rootDir = rootProject.layout.projectDirectory
    val script = providers.gradleProperty("snapshot.script")
    val out = providers.gradleProperty("snapshot.out").orElse(layout.buildDirectory.dir("snapshots").map { it.asFile.path })
    val home = providers.gradleProperty("snapshot.home")
    val density = providers.gradleProperty("snapshot.density").orElse("1")
    // Snapshots depend on the script and the time, never up to date.
    outputs.upToDateWhen { false }
    argumentProviders.add(
        CommandLineArgumentProvider {
            val scriptPath = script.orNull ?: throw GradleException("Pass the script: -Psnapshot.script=<file>")
            fun path(value: String) = rootDir.file(value).asFile.absolutePath
            listOf("--script", path(scriptPath), "--out", path(out.get()), "--density", density.get()) +
                (home.orNull?.let { listOf("--home", path(it)) } ?: emptyList())
        },
    )
}
