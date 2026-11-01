import com.google.firebase.crashlytics.buildtools.gradle.CrashlyticsExtension
import dev.detekt.gradle.extensions.DetektExtension
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.google.services)
    alias(libs.plugins.firebase.crashlytics)
    alias(libs.plugins.ktlint)
    alias(libs.plugins.detekt)
}

val versionProps =
    rootProject.file("version.properties").inputStream().use { stream ->
        Properties().apply { load(stream) }
    }

android {
    namespace = "com.kingpaging.qwelcome"
    // androidx.lifecycle 2.11.0 requires compiling against API 37+
    compileSdk = 37
    testBuildType = providers.gradleProperty("testBuildType").orElse("debug").get()
    require(testBuildType in setOf("debug", "smoke")) { "testBuildType must be debug or smoke" }

    defaultConfig {
        applicationId = "com.kingpaging.qwelcome"
        minSdk = 26
        targetSdk = 36
        versionCode =
            requireNotNull(versionProps.getProperty("VERSION_CODE")) {
                "VERSION_CODE missing from version.properties"
            }.toInt()
        versionName =
            requireNotNull(versionProps.getProperty("VERSION_NAME")) {
                "VERSION_NAME missing from version.properties"
            }

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        create("release") {
            // Read from environment variables (CI) or local properties
            val envKeystorePath = System.getenv("KEYSTORE_FILE")
            val keystoreFile =
                when {
                    // CI: use environment variable path (relative to root project)
                    envKeystorePath != null -> rootProject.file(envKeystorePath)
                    // Local: check for keystore in root project
                    else -> rootProject.file("qwelcome-release.keystore")
                }

            if (keystoreFile.exists()) {
                storeFile = keystoreFile
                storePassword = System.getenv("KEYSTORE_PASSWORD") ?: ""
                keyAlias = System.getenv("KEY_ALIAS") ?: "qwelcome"
                keyPassword = System.getenv("KEY_PASSWORD") ?: ""
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            signingConfig = signingConfigs.findByName("release")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
        create("smoke") {
            initWith(getByName("release"))
            proguardFile("proguard-smoke.pro")
            // Exercise release shrinking without requiring production signing credentials.
            signingConfig = signingConfigs.getByName("debug")
            matchingFallbacks += listOf("release")
            configure<CrashlyticsExtension> {
                mappingFileUploadEnabled = false
                nativeSymbolUploadEnabled = false
            }
        }
    }
    sourceSets {
        // Shared fakes are compiled into tests only, for every tested build type.
        getByName("test").kotlin.directories.add("src/sharedTest/kotlin")
        getByName("androidTest").kotlin.directories.add("src/sharedTest/kotlin")
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    testOptions {
        unitTests.isReturnDefaultValues = true
    }
}

tasks.withType<KotlinJvmCompile>().configureEach {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_11)
    }
}

extensions.configure<DetektExtension> {
    buildUponDefaultConfig.set(true)
    config.setFrom(file("detekt.yml"))
    baseline.set(file("detekt-baseline.xml"))
}

dependencies {
    implementation(project(":proto"))
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.process)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.datastore.core)
    implementation(libs.androidx.splashscreen)
    // Navigation Compose
    implementation(libs.androidx.navigation.compose)
    // QR Code generation with styling support
    implementation(libs.qrose)
    // JSON serialization for import/export
    implementation(libs.kotlinx.serialization.json)
    // Firebase (BOM manages versions)
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.crashlytics)
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
    testImplementation(libs.mockk)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.zxing.core)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.espresso.intents)
    androidTestUtil(libs.androidx.test.services)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    "smokeImplementation"(libs.androidx.compose.ui.test.manifest)
}
