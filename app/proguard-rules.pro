# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Keep line numbers for debugging stack traces
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Keep data classes used for settings and customer data
-keep class com.example.allowelcome.data.** { *; }

# Keep ViewModel classes
-keep class com.example.allowelcome.viewmodel.** { *; }

# Keep QR code library classes
-keep class io.github.alexzhirkevich.qrose.** { *; }

# Kotlin serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt

# DataStore
-keep class androidx.datastore.** { *; }

# Compose runtime - rely on library's consumer rules, only suppress warnings
-dontwarn androidx.compose.**

# Kotlin Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembernames class kotlinx.** {
    volatile <fields>;
}
