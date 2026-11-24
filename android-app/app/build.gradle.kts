import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("com.google.devtools.ksp")
}

android {
    namespace = "ir.esmail.espresso"
    compileSdk = 36

    defaultConfig {
        applicationId = "ir.esmail.espresso"
        minSdk = 24
        targetSdk = 35
        versionCode = 10
        versionName = "5.4.1"
        resourceConfigurations += listOf("fa", "en")
        vectorDrawables.useSupportLibrary = true
        // Real devices only need ARM. Opt in with -ParmOnly to drop x86/x86_64 and save ~78 MB
        // of native libs; leave it off for emulator testing / a universal APK.
        if (project.hasProperty("armOnly")) {
            ndk { abiFilters += listOf("arm64-v8a", "armeabi-v7a") }
        }
        // Emulator-only build: one ABI keeps the APK small enough for a slow adb bridge.
        if (project.hasProperty("x86Only")) {
            ndk { abiFilters += listOf("x86_64") }
        }
    }

    signingConfigs {
        // Credentials live in local.properties (git-ignored) or in CI secrets, never in git.
        // Without them `assembleRelease` still succeeds but produces an unsigned APK.
        val signingProps = Properties().apply {
            val file = rootProject.file("local.properties")
            if (file.exists()) file.inputStream().use { load(it) }
        }
        fun secret(key: String): String? = signingProps.getProperty(key) ?: System.getenv(key)

        val storePath = secret("RELEASE_STORE_FILE")
        val storePass = secret("RELEASE_STORE_PASSWORD")
        val aliasName = secret("RELEASE_KEY_ALIAS")
        val aliasPass = secret("RELEASE_KEY_PASSWORD")

        create("release") {
            if (storePath != null && storePass != null && aliasName != null && aliasPass != null) {
                storeFile = file(storePath)
                storePassword = storePass
                keyAlias = aliasName
                keyPassword = aliasPass
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            isShrinkResources = false
            signingConfig = signingConfigs.getByName("release")
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
        debug {
            applicationIdSuffix = ".debug"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources.excludes += setOf("/META-INF/{AL2.0,LGPL2.1}", "/META-INF/*.kotlin_module")
    }

    androidResources {
        noCompress += listOf("mp3", "json")
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        freeCompilerArgs.addAll(
            "-opt-in=androidx.compose.material3.ExperimentalMaterial3Api",
            "-opt-in=androidx.compose.foundation.ExperimentalFoundationApi",
            "-opt-in=androidx.compose.foundation.layout.ExperimentalLayoutApi",
            "-opt-in=kotlinx.coroutines.ExperimentalCoroutinesApi",
            "-opt-in=androidx.media3.common.util.UnstableApi",
        )
    }
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
    arg("room.generateKotlin", "true")
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.3")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.3")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.3")

    implementation("androidx.compose.ui:ui:1.7.6")
    implementation("androidx.compose.ui:ui-graphics:1.7.6")
    implementation("androidx.compose.ui:ui-tooling-preview:1.7.6")
    implementation("androidx.compose.foundation:foundation:1.7.6")
    implementation("androidx.compose.material3:material3:1.3.1")
    implementation("androidx.compose.material:material-icons-extended:1.7.6")
    implementation("androidx.compose.animation:animation:1.7.6")
    debugImplementation("androidx.compose.ui:ui-tooling:1.7.6")

    implementation("androidx.navigation:navigation-compose:2.8.5")

    implementation("androidx.media3:media3-exoplayer:1.4.1")
    implementation("androidx.media3:media3-session:1.4.1")
    implementation("androidx.media3:media3-common:1.4.1")

    implementation("androidx.room:room-runtime:2.7.2")
    implementation("androidx.room:room-ktx:2.7.2")
    ksp("androidx.room:room-compiler:2.7.2")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")

    // Fully bundled offline neural TTS (Piper voice ships in assets/tts/voice).
    implementation(files("libs/sherpa-onnx-2.0.3.aar"))
    implementation("com.github.csukuangfj.piper-phonemize:piper-phonemize:v1.6.1")

    // On-device OCR for the live screen-scan translation tool. The model downloads via
    // Play Services on first use; our own translation still goes through MyMemory online.
    implementation("com.google.mlkit:text-recognition:16.0.1")

    // Pure-JVM tests for the LRC parser and the lyrics/playback alignment maths.
    testImplementation("junit:junit:4.13.2")
}
