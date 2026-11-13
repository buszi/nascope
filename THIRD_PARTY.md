# Third-party software

Nascope is licensed under the Apache License 2.0 (see `LICENSE`). It is built on the open-source libraries below. All of them are under licenses compatible with Apache-2.0 (Apache-2.0, MIT, BSD, ISC). This list was generated from the resolved runtime dependencies of the desktop app (`desktopApp`, `runtimeClasspath`) and the Android app (`androidApp`, `releaseRuntimeClasspath`) at version 1.0.0, with the license each library declares in its POM. Platform variants of a library (`-desktop`, `-jvm`, `-android`) share the license of the library they belong to. Regenerate it when dependencies change. It is an inventory, not legal advice.

**305 libraries: 294 Apache-2.0, 11 under other licenses (listed first).**

## Bundled with the desktop packages

- **OpenJDK runtime** (Azul Zulu 21, linked into the app by jpackage): GPL-2.0 with the Classpath Exception. The Classpath Exception allows distributing it with applications under any license.
- **Skia / Skiko** (the rendering engine of Compose Multiplatform for desktop): Skia is BSD-3-Clause, Skiko is Apache-2.0.
- Fonts are not bundled: the app uses the platform's sans-serif and monospace fonts.

## Libraries under other licenses

| Library | Version | Used in | License | Note |
|---|---|---|---|---|
| `androidx.datastore:datastore-preferences-external-protobuf` | 1.3.0 | android/desktop | BSD |  |
| `com.github.hypfvieh:dbus-java-core` | 4.2.1 | desktop | MIT |  |
| `com.github.hypfvieh:dbus-java-transport-native-unixsocket` | 4.2.1 | desktop | MIT |  |
| `com.github.javakeyring:java-keyring` | 1.0.4 | desktop | BSD |  |
| `com.michael-bull.kotlin-result:kotlin-result` | 2.3.1 | android/desktop | ISC License |  |
| `com.michael-bull.kotlin-result:kotlin-result-jvm` | 2.3.1 | android/desktop | ISC License |  |
| `de.swiesend:secret-service` | 1.8.1-jdk17 | desktop | MIT |  |
| `net.java.dev.jna:jna` | 5.13.0 | desktop | Apache-2.0, LGPL | dual-licensed Apache-2.0 / LGPL-2.1: used under Apache-2.0 |
| `net.java.dev.jna:jna-platform` | 5.13.0 | desktop | Apache-2.0, LGPL | dual-licensed Apache-2.0 / LGPL-2.1: used under Apache-2.0 |
| `org.slf4j:slf4j-api` | 2.0.19 | android/desktop | MIT |  |
| `pt.davidafsilva.apple:jkeychain` | 1.1.0 | desktop | BSD |  |

## Apache-2.0 libraries

- **androidx.activity**: activity 1.13.0, activity-compose 1.13.0, activity-ktx 1.13.0
- **androidx.annotation**: annotation-experimental 1.4.1
- **androidx.appcompat**: appcompat 1.7.1, appcompat-resources 1.7.1
- **androidx.arch.core**: core-common 2.2.0, core-runtime 2.2.0
- **androidx.autofill**: autofill 1.0.0
- **androidx.collection**: collection 1.5.0, collection 1.6.0, collection-jvm 1.5.0, collection-jvm 1.6.0, collection-ktx 1.6.0
- **androidx.compose.animation**: animation 1.12.1, animation-android 1.12.1, animation-core 1.12.1, animation-core-android 1.12.1
- **androidx.compose.foundation**: foundation 1.12.1, foundation-android 1.12.1, foundation-layout 1.12.1, foundation-layout-android 1.12.1
- **androidx.compose.material3**: material3 1.4.0, material3-android 1.4.0
- **androidx.compose.runtime**: runtime 1.12.1, runtime-android 1.12.1, runtime-annotation 1.12.1, runtime-annotation-android 1.12.1, runtime-retain 1.12.1, runtime-retain-android 1.12.1, runtime-saveable 1.12.1, runtime-saveable-android 1.12.1
- **androidx.compose.ui**: ui 1.12.1, ui-android 1.12.1, ui-geometry 1.12.1, ui-geometry-android 1.12.1, ui-graphics 1.12.1, ui-graphics-android 1.12.1, ui-text 1.12.1, ui-text-android 1.12.1, ui-tooling-preview 1.12.1, ui-tooling-preview-android 1.12.1, ui-unit 1.12.1, ui-unit-android 1.12.1, ui-util 1.12.1, ui-util-android 1.12.1
- **androidx.concurrent**: concurrent-futures 1.1.0
- **androidx.core**: core 1.18.0, core-ktx 1.18.0, core-splashscreen 1.2.0, core-viewtree 1.0.0
- **androidx.cursoradapter**: cursoradapter 1.0.0
- **androidx.customview**: customview 1.0.0, customview-poolingcontainer 1.0.0
- **androidx.datastore**: datastore-core 1.3.0, datastore-core-android 1.3.0, datastore-core-jvm 1.3.0, datastore-core-okio 1.3.0, datastore-core-okio-jvm 1.3.0, datastore-preferences-core 1.3.0, datastore-preferences-core-android 1.3.0, datastore-preferences-core-jvm 1.3.0, datastore-preferences-proto 1.3.0
- **androidx.drawerlayout**: drawerlayout 1.0.0
- **androidx.emoji2**: emoji2 1.4.0, emoji2-views-helper 1.4.0
- **androidx.fragment**: fragment 1.8.9, fragment-ktx 1.8.9
- **androidx.graphics**: graphics-path 1.0.1
- **androidx.interpolator**: interpolator 1.0.0
- **androidx.lifecycle**: lifecycle-common 2.11.0, lifecycle-common-java8 2.11.0, lifecycle-common-jvm 2.11.0, lifecycle-livedata 2.11.0, lifecycle-livedata-core 2.11.0, lifecycle-livedata-core-ktx 2.11.0, lifecycle-process 2.11.0, lifecycle-runtime 2.11.0, lifecycle-runtime-android 2.11.0, lifecycle-runtime-compose 2.11.0, lifecycle-runtime-compose-android 2.11.0, lifecycle-runtime-ktx 2.11.0, lifecycle-runtime-ktx-android 2.11.0, lifecycle-viewmodel 2.11.0, lifecycle-viewmodel-android 2.11.0, lifecycle-viewmodel-compose 2.11.0, lifecycle-viewmodel-compose-android 2.11.0, lifecycle-viewmodel-ktx 2.11.0, lifecycle-viewmodel-navigation3 2.11.0, lifecycle-viewmodel-navigation3-android 2.11.0, lifecycle-viewmodel-navigation3-desktop 2.11.0, lifecycle-viewmodel-savedstate 2.11.0, lifecycle-viewmodel-savedstate-android 2.11.0
- **androidx.loader**: loader 1.0.0
- **androidx.navigation3**: navigation3-runtime 1.2.0, navigation3-runtime-android 1.2.0, navigation3-runtime-desktop 1.2.0, navigation3-ui 1.2.0, navigation3-ui-android 1.2.0
- **androidx.navigationevent**: navigationevent 1.1.1, navigationevent-android 1.1.1, navigationevent-compose 1.1.1, navigationevent-compose-android 1.1.1
- **androidx.resourceinspection**: resourceinspection-annotation 1.0.1
- **androidx.savedstate**: savedstate 1.4.0, savedstate-android 1.4.0, savedstate-compose 1.4.0, savedstate-compose-android 1.4.0, savedstate-ktx 1.4.0
- **androidx.startup**: startup-runtime 1.2.0
- **androidx.tracing**: tracing 2.0.1, tracing-android 2.0.1
- **androidx.vectordrawable**: vectordrawable 1.1.0, vectordrawable-animated 1.1.0
- **androidx.versionedparcelable**: versionedparcelable 1.1.1
- **androidx.viewpager**: viewpager 1.0.0
- **androidx.window**: window 1.5.0, window-core 1.5.0, window-core-android 1.5.0
- **at.favre.lib**: hkdf 1.1.0
- **co.touchlab**: stately-concurrency 2.1.0, stately-concurrency-jvm 2.1.0, stately-concurrent-collections 2.1.0, stately-concurrent-collections-jvm 2.1.0, stately-strict 2.1.0, stately-strict-jvm 2.1.0
- **com.charleskorn.kaml**: kaml 0.79.0, kaml-jvm 0.79.0
- **com.squareup.okhttp3**: okhttp 5.5.1, okhttp-android 5.5.1, okhttp-jvm 5.5.1
- **com.squareup.okio**: okio 3.18.2, okio-jvm 3.18.2
- **io.github.aakira**: napier 2.7.1, napier-android 2.7.1
- **io.insert-koin**: koin-android 4.2.2, koin-annotations 4.2.2, koin-annotations-jvm 4.2.2, koin-compose 4.2.2, koin-compose-android 4.2.2, koin-compose-jvm 4.2.2, koin-compose-viewmodel 4.2.2, koin-compose-viewmodel-android 4.2.2, koin-compose-viewmodel-jvm 4.2.2, koin-core 4.2.2, koin-core-annotations 4.2.2, koin-core-annotations-jvm 4.2.2, koin-core-jvm 4.2.2, koin-core-viewmodel 4.2.2, koin-core-viewmodel-android 4.2.2, koin-core-viewmodel-jvm 4.2.2
- **io.ktor**: ktor-client-content-negotiation 3.6.1, ktor-client-content-negotiation-jvm 3.6.1, ktor-client-core 3.6.1, ktor-client-core-jvm 3.6.1, ktor-client-logging 3.6.1, ktor-client-logging-jvm 3.6.1, ktor-client-okhttp 3.6.1, ktor-client-okhttp-jvm 3.6.1, ktor-events 3.6.1, ktor-events-jvm 3.6.1, ktor-http 3.6.1, ktor-http-cio 3.6.1, ktor-http-cio-jvm 3.6.1, ktor-http-jvm 3.6.1, ktor-io 3.6.1, ktor-io-jvm 3.6.1, ktor-network 3.6.1, ktor-network-jvm 3.6.1, ktor-openapi-schema 3.6.1, ktor-openapi-schema-jvm 3.6.1, ktor-serialization 3.6.1, ktor-serialization-jvm 3.6.1, ktor-serialization-kotlinx 3.6.1, ktor-serialization-kotlinx-json 3.6.1, ktor-serialization-kotlinx-json-jvm 3.6.1, ktor-serialization-kotlinx-jvm 3.6.1, ktor-sse 3.6.1, ktor-sse-jvm 3.6.1, ktor-utils 3.6.1, ktor-utils-jvm 3.6.1, ktor-websocket-serialization 3.6.1, ktor-websocket-serialization-jvm 3.6.1, ktor-websockets 3.6.1, ktor-websockets-jvm 3.6.1
- **it.krzeminski**: snakeyaml-engine-kmp 3.1.1, snakeyaml-engine-kmp-jvm 3.1.1
- **net.thauvin.erik.urlencoder**: urlencoder-lib 1.6.0, urlencoder-lib-jvm 1.6.0
- **org.jetbrains.androidx.lifecycle**: lifecycle-common 2.11.0, lifecycle-common-jvm 2.11.0, lifecycle-runtime 2.11.0, lifecycle-runtime-android 2.11.0, lifecycle-runtime-compose 2.11.0, lifecycle-runtime-compose-android 2.11.0, lifecycle-runtime-compose-desktop 2.11.0, lifecycle-runtime-desktop 2.11.0, lifecycle-viewmodel 2.11.0, lifecycle-viewmodel-android 2.11.0, lifecycle-viewmodel-compose 2.11.0, lifecycle-viewmodel-compose-android 2.11.0, lifecycle-viewmodel-compose-desktop 2.11.0, lifecycle-viewmodel-desktop 2.11.0, lifecycle-viewmodel-navigation3 2.11.0, lifecycle-viewmodel-navigation3-android 2.11.0, lifecycle-viewmodel-navigation3-desktop 2.11.0, lifecycle-viewmodel-savedstate 2.11.0, lifecycle-viewmodel-savedstate-android 2.11.0, lifecycle-viewmodel-savedstate-desktop 2.11.0
- **org.jetbrains.androidx.navigation3**: navigation3-ui 1.2.0, navigation3-ui-android 1.2.0, navigation3-ui-desktop 1.2.0
- **org.jetbrains.androidx.navigationevent**: navigationevent-compose 1.1.0
- **org.jetbrains.androidx.savedstate**: savedstate-compose 1.4.0, savedstate-compose-desktop 1.4.0
- **org.jetbrains.compose.animation**: animation 1.12.2, animation-android 1.12.2, animation-core 1.12.2, animation-core-android 1.12.2, animation-core-desktop 1.12.2, animation-desktop 1.12.2
- **org.jetbrains.compose.annotation-internal**: annotation 1.9.1
- **org.jetbrains.compose.collection-internal**: collection 1.9.1
- **org.jetbrains.compose.components**: components-resources 1.12.2, components-resources-android 1.12.2, components-resources-desktop 1.12.2
- **org.jetbrains.compose.desktop**: desktop 1.12.2, desktop-jvm 1.12.2, desktop-jvm-macos-arm64 1.12.2
- **org.jetbrains.compose.foundation**: foundation 1.12.2, foundation-android 1.12.2, foundation-desktop 1.12.2, foundation-layout 1.12.2, foundation-layout-android 1.12.2, foundation-layout-desktop 1.12.2
- **org.jetbrains.compose.material**: material 1.12.2, material-desktop 1.12.2, material-ripple 1.12.2, material-ripple-desktop 1.12.2
- **org.jetbrains.compose.runtime**: runtime 1.12.2, runtime-android 1.12.2, runtime-desktop 1.12.2, runtime-saveable 1.12.2, runtime-saveable-android 1.12.2, runtime-saveable-desktop 1.12.2
- **org.jetbrains.compose.ui**: ui 1.12.2, ui-android 1.12.2, ui-backhandler 1.12.2, ui-backhandler-android 1.12.2, ui-backhandler-desktop 1.12.2, ui-desktop 1.12.2, ui-geometry 1.12.2, ui-geometry-android 1.12.2, ui-geometry-desktop 1.12.2, ui-graphics 1.12.2, ui-graphics-android 1.12.2, ui-graphics-desktop 1.12.2, ui-text 1.12.2, ui-text-android 1.12.2, ui-text-desktop 1.12.2, ui-tooling-preview 1.12.2, ui-tooling-preview-android 1.12.2, ui-tooling-preview-desktop 1.12.2, ui-unit 1.12.2, ui-unit-android 1.12.2, ui-unit-desktop 1.12.2, ui-util 1.12.2, ui-util-android 1.12.2, ui-util-desktop 1.12.2
- **org.jetbrains.kotlin**: kotlin-stdlib 2.4.21, kotlin-stdlib-common 2.4.21
- **org.jetbrains.kotlinx**: atomicfu 0.28.0, kotlinx-coroutines-android 1.11.0, kotlinx-coroutines-bom 1.11.0, kotlinx-coroutines-core 1.11.0, kotlinx-coroutines-core-jvm 1.11.0, kotlinx-coroutines-slf4j 1.11.0, kotlinx-datetime 0.8.0-0.6.x-compat, kotlinx-datetime-jvm 0.8.0-0.6.x-compat, kotlinx-io-bytestring 0.9.1, kotlinx-io-bytestring-jvm 0.9.1, kotlinx-io-core 0.9.1, kotlinx-io-core-jvm 0.9.1, kotlinx-serialization-bom 1.12.0, kotlinx-serialization-core 1.12.0, kotlinx-serialization-core-jvm 1.12.0, kotlinx-serialization-json 1.12.0, kotlinx-serialization-json-io 1.12.0, kotlinx-serialization-json-io-jvm 1.12.0, kotlinx-serialization-json-jvm 1.12.0
- **org.jetbrains.skiko**: skiko 0.150.2, skiko-awt 0.150.2, skiko-awt-runtime-macos-arm64 0.150.2
- **org.jspecify**: jspecify 1.0.0
