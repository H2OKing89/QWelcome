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
# Official rules are applied via consumer rules from the library.
# These additional rules ensure @Serializable classes work with R8 full mode.
-keepattributes *Annotation*, InnerClasses, RuntimeVisibleAnnotations, AnnotationDefault
-dontnote kotlinx.serialization.AnnotationsKt

# Keep serializer for @Serializable classes in app package (conditional keeps)
-if @kotlinx.serialization.Serializable class com.kingpaging.qwelcome.**
-keepclassmembers class <1> {
    static <1>$Companion Companion;
}

-if @kotlinx.serialization.Serializable class com.kingpaging.qwelcome.** {
    static **$* *;
}
-keepclassmembers class <1>$<2> {
    kotlinx.serialization.KSerializer serializer(...);
}

# Keep serializer descriptor (official rule)
-keepclassmembers public class **$$serializer {
    private ** descriptor;
}

# Protobuf-lite generated classes
-keep class * extends com.google.protobuf.GeneratedMessageLite { *; }

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

# Firebase Crashlytics - keep file names and line numbers for stack traces
-keepattributes SourceFile,LineNumberTable
-keep public class * extends java.lang.Exception
# Keep custom Crashlytics keys
-keepattributes *Annotation*
