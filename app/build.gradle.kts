import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.example.keios"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.example.keios"
        minSdk = 31
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
    }
    compileSdkMinor = 0
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.activity:activity-compose:1.10.1")
    implementation("com.google.android.material:material:1.12.0")

    implementation("androidx.compose.ui:ui:1.8.1")
    implementation("androidx.compose.foundation:foundation:1.8.1")
    implementation("androidx.compose.ui:ui-tooling-preview:1.8.1")
    debugImplementation("androidx.compose.ui:ui-tooling:1.8.1")

    implementation("top.yukonga.miuix.kmp:miuix-ui-android:0.9.0")
    implementation("top.yukonga.miuix.kmp:miuix-preference-android:0.9.0")
    implementation("top.yukonga.miuix.kmp:miuix-icons-android:0.9.0")
    implementation("top.yukonga.miuix.kmp:miuix-shapes-android:0.9.0")
    implementation("top.yukonga.miuix.kmp:miuix-blur-android:0.9.0")
    implementation("top.yukonga.miuix.kmp:miuix-navigation3-ui-android:0.9.0")

    implementation("dev.rikka.shizuku:api:13.1.5")
    implementation("dev.rikka.shizuku:provider:13.1.5")
}
