plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.devtools.ksp")
}

import java.io.FileInputStream
import java.util.Properties

val keystorePropertiesFile =
    listOf(
        rootProject.file("keystore.properties"),
        rootProject.file("swiftsave-android/keystore.properties"),
    ).firstOrNull { it.exists() } ?: rootProject.file("keystore.properties")
val keystoreProperties = Properties()
val hasReleaseKeystore =
    keystorePropertiesFile.exists().also { exists ->
        if (exists) {
            keystoreProperties.load(FileInputStream(keystorePropertiesFile))
        }
    }
val allowDebugReleaseSigning =
    providers.gradleProperty("allowDebugReleaseSigning")
        .map(String::toBoolean)
        .getOrElse(false)

android {
    namespace = "com.downme.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.downme.app"
        minSdk = 24
        targetSdk = 35
        versionCode = 2
        versionName = "1.0.1"

        // Real phones only (drops x86/x86_64 emulator libs — saves ~100 MB in the universal APK).
        ndk {
            abiFilters += listOf("arm64-v8a", "armeabi-v7a")
        }

        vectorDrawables {
            useSupportLibrary = true
        }
    }

    signingConfigs {
        if (hasReleaseKeystore) {
            create("release") {
                keyAlias = keystoreProperties.getProperty("keyAlias")
                keyPassword = keystoreProperties.getProperty("keyPassword")
                storeFile = rootProject.file(keystoreProperties.getProperty("storeFile")!!)
                storePassword = keystoreProperties.getProperty("storePassword")
            }
        }
    }

    buildTypes {
        release {
            // Match debug behavior: R8 + resource shrinking break bundled Python/yt-dlp on device.
            isMinifyEnabled = false
            isShrinkResources = false
            signingConfig =
                if (hasReleaseKeystore) {
                    signingConfigs.getByName("release")
                } else if (allowDebugReleaseSigning) {
                    signingConfigs.getByName("debug")
                } else {
                    null
                }
            proguardFiles(
                getDefaultProguardFile("proguard-android.txt"),
                "proguard-rules.pro",
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    splits {
        abi {
            isEnable = true
            reset()
            include("arm64-v8a", "armeabi-v7a")
            // Also builds a smaller universal ARM APK; per-ABI APKs are smallest for your website.
            isUniversalApk = true
        }
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
        jniLibs {
            useLegacyPackaging = true
        }
    }
}

gradle.taskGraph.whenReady {
    val buildsPublicRelease =
        allTasks.any { task ->
            task.name in setOf("assembleRelease", "bundleRelease") ||
                task.name.contains("packageRelease", ignoreCase = true)
        }
    if (buildsPublicRelease && !hasReleaseKeystore && !allowDebugReleaseSigning) {
        throw GradleException(
            "Refusing to build a public release without keystore.properties. " +
                "Create a release keystore first, or use -PallowDebugReleaseSigning=true for local testing only.",
        )
    }
}

val youtubedlVersion = "0.18.1"

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.12.01")
    implementation(composeBom)
    androidTestImplementation(composeBom)

    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.activity:activity-ktx:1.9.3")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")

    implementation("androidx.navigation:navigation-compose:2.8.4")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")

    implementation("androidx.datastore:datastore-preferences:1.1.1")

    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")

    implementation("io.coil-kt:coil-compose:2.7.0")

    implementation("androidx.media3:media3-exoplayer:1.5.1")
    implementation("androidx.media3:media3-ui:1.5.1")

    // On-device yt-dlp + FFmpeg: youtubedl-android bundles Python, yt-dlp, and FFmpeg for Android.
    implementation("io.github.junkfood02.youtubedl-android:library:$youtubedlVersion")
    implementation("io.github.junkfood02.youtubedl-android:ffmpeg:$youtubedlVersion")

    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
