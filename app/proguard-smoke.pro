# Only the smoke build reads this file. Production release rules are unchanged.
#
# AGP removes dependencies shared with the tested app from the test APK, but
# app R8 cannot see separate runner/test calls or subclasses of those dependencies.
# Preserve public/protected library ABI, including constructors and overridable
# methods: allowoptimization could finalize a method overridden in the test APK.
# Application implementations still shrink, optimize and obfuscate normally.
# This smoke validates app minification, not release library optimization/pruning.
#
# Kotlin APIs used by AndroidJUnitRunner and the Kotlin/Compose test harness.
-keep class kotlin.** {
    public protected *;
}
-keep class kotlinx.coroutines.* {
    public protected *;
}
-keep class kotlinx.coroutines.flow.** {
    public protected *;
}
-keep class kotlinx.coroutines.channels.** {
    public protected *;
}
# kotlinx-coroutines-test directly constructs ThreadSafeHeap and subclasses
# coroutine runtime types, so the internal package crosses the same ABI boundary.
# The optional coroutines debug-agent package is deliberately not retained.
-keep class kotlinx.coroutines.internal.** {
    public protected *;
}

# Test-library inline helpers also cross these coroutine runtime package APIs.
-keep class kotlinx.coroutines.sync.** {
    public protected *;
}
-keep class kotlinx.coroutines.selects.** {
    public protected *;
}
-keep class kotlinx.coroutines.intrinsics.** {
    public protected *;
}
-keep class kotlinx.coroutines.scheduling.** {
    public protected *;
}
-keep class kotlinx.coroutines.android.** {
    public protected *;
}

# AndroidX runtime APIs shared by the app, ActivityScenario and Compose tests.
-keep class androidx.tracing.** {
    public protected *;
}
-keep class androidx.activity.** {
    public protected *;
}
-keep class androidx.lifecycle.** {
    public protected *;
}
-keep class androidx.savedstate.** {
    public protected *;
}
-keep class androidx.core.** {
    public protected *;
}
-keep class androidx.collection.** {
    public protected *;
}
-keep class androidx.compose.runtime.** {
    public protected *;
}
-keep class androidx.compose.ui.** {
    public protected *;
}
-keep class androidx.navigation.** {
    public protected *;
}

# Exact app harness entry points invoked directly by the three smoke test classes.
# Other application classes continue to use the production R8 configuration.
-keepclassmembers,allowoptimization class com.kingpaging.qwelcome.QWelcomeApplication {
    public com.kingpaging.qwelcome.di.AppContainer getAppContainer();
}
-keep,allowoptimization class com.kingpaging.qwelcome.di.AppContainer {
    public *;
}
-keep,allowoptimization class com.kingpaging.qwelcome.di.CompositionLocalsKt {
    public *;
}
-keep,allowoptimization class com.kingpaging.qwelcome.navigation.AppNavGraphKt {
    public *;
}
-keep,allowoptimization class com.kingpaging.qwelcome.ui.theme.CyberpunkThemeKt {
    public *;
}
-keep,allowoptimization class com.kingpaging.qwelcome.util.WifiQrGenerator {
    public *;
}

# Test APK fakes implement these interfaces outside app R8's closed-world view.
-keep interface com.kingpaging.qwelcome.navigation.Navigator {
    public *;
}
-keep interface com.kingpaging.qwelcome.util.SoundPlayer {
    public *;
}

# Tests pass typed route objects and inspect editor arguments across the APK ABI.
-keep,allowoptimization class com.kingpaging.qwelcome.navigation.Routes$Main {
    public *;
}
-keep,allowoptimization class com.kingpaging.qwelcome.navigation.Routes$Settings {
    public *;
}
-keep,allowoptimization class com.kingpaging.qwelcome.navigation.Routes$Export {
    public *;
}
-keep,allowoptimization class com.kingpaging.qwelcome.navigation.Routes$Import {
    public *;
}
-keep,allowoptimization class com.kingpaging.qwelcome.navigation.Routes$TemplateList {
    public *;
}
-keep,allowoptimization class com.kingpaging.qwelcome.navigation.Routes$TemplateEditor {
    public *;
}
