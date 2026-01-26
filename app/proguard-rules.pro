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
-keep class com.kingpaging.qwelcome.data.** { *; }

# Keep ViewModel classes
-keep class com.kingpaging.qwelcome.viewmodel.** { *; }

# Keep QR code library classes
-keep class io.github.alexzhirkevich.qrose.** { *; }

# Kotlin serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt

-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Keep serializable classes in the app package
-keep,includedescriptorclasses class com.kingpaging.qwelcome.**$$serializer { *; }
-keepclassmembers class com.kingpaging.qwelcome.** {
    *** Companion;
}
-keepclasseswithmembers class com.kingpaging.qwelcome.** {
    kotlinx.serialization.KSerializer serializer(...);
}

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
