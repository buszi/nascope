# Release shrinking of the desktop app (the *Release* package tasks); -Pnascope.desktop.shrink=false turns it off.
# Compose Desktop's ProGuard: no obfuscation, no optimization. See docs/release.md for how it was verified.
# Unlike R8 on Android, this ProGuard does not read the consumer rules shipped inside dependencies,
# so what the libraries need through reflection or service loading is listed here.

-keep class io.buszi.nascope.MainKt { public static void main(java.lang.String[]); }

# OkHttp probes optional TLS providers and GraalVM hooks that are not on the classpath.
-dontwarn okhttp3.internal.platform.**
-dontwarn okhttp3.internal.graal.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**
-dontwarn org.graalvm.**
-dontwarn com.oracle.svm.**

# kotlinx.serialization: generated serializers are looked up by name.
-keepattributes *Annotation*, InnerClasses, Signature
-keep,includedescriptorclasses class io.buszi.nascope.**$$serializer { *; }
-keepclassmembers class io.buszi.nascope.** { *** Companion; }
-keepclasseswithmembers class io.buszi.nascope.** { kotlinx.serialization.KSerializer serializer(...); }

# Coroutines: the Swing dispatcher is found through a service file.
-keep class kotlinx.coroutines.internal.MainDispatcherFactory { *; }
-keep class kotlinx.coroutines.swing.SwingDispatcherFactory { *; }

# DataStore preferences: the generated protobuf messages are read by field name.
-keepclassmembers class * extends androidx.datastore.preferences.protobuf.GeneratedMessageLite { <fields>; }

# OS keyring through JNA.
-keep class com.sun.jna.** { *; }
-keep class * implements com.sun.jna.** { *; }
-keep class com.github.javakeyring.** { *; }

# Ktor loads its extensions through ServiceLoader.
-keep class * implements io.ktor.serialization.kotlinx.KotlinxSerializationExtensionProvider { *; }
-keep class io.ktor.serialization.kotlinx.** { *; }

# Linux keyring backend (Secret Service over D-Bus): D-Bus builds exception and message classes reflectively.
-keep class org.freedesktop.** { *; }
-keep class org.purejava.** { *; }
-dontwarn org.freedesktop.**
-dontwarn org.purejava.**

