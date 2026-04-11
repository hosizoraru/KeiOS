import org.jetbrains.kotlin.gradle.dsl.JvmTarget

fun runGitCommandOrNull(vararg args: String): String? {
    return try {
        val process = ProcessBuilder(listOf("git", *args))
            .directory(rootDir)
            .redirectErrorStream(true)
            .start()
        val output = process.inputStream.bufferedReader().use { it.readText() }.trim()
        val exitCode = process.waitFor()
        if (exitCode == 0 && output.isNotEmpty()) output else null
    } catch (_: Exception) {
        null
    }
}

val baseVersionName = "1.0"
val gitCommitCount = runGitCommandOrNull("rev-list", "--count", "HEAD")?.toIntOrNull() ?: 1
val gitShortHash = runGitCommandOrNull("rev-parse", "--short", "HEAD") ?: "nogit"
val gitDirty = runGitCommandOrNull("status", "--porcelain").isNullOrBlank().not()
val autoVersionCode = 10_000 + gitCommitCount
val autoVersionName = buildString {
    append(baseVersionName)
    append(".")
    append(gitCommitCount)
    append("-")
    append(gitShortHash)
    if (gitDirty) append("-dirty")
}

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.example.keios"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.example.keios"
        minSdk = 35
        targetSdk = 37
        versionCode = autoVersionCode
        versionName = autoVersionName

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
    implementation("androidx.core:core-ktx:1.18.0")
    implementation("androidx.activity:activity-compose:1.13.0")
    implementation("com.google.android.material:material:1.13.0")

    implementation("androidx.compose.ui:ui:1.10.6")
    implementation("androidx.compose.foundation:foundation:1.10.6")
    implementation("androidx.navigation3:navigation3-runtime:1.1.0")
    implementation("androidx.navigation:navigation-common-ktx:2.9.7")
    debugImplementation("androidx.compose.ui:ui-tooling:1.10.6")

    implementation("top.yukonga.miuix.kmp:miuix-ui-android:0.9.0")
    implementation("top.yukonga.miuix.kmp:miuix-preference-android:0.9.0")
    implementation("top.yukonga.miuix.kmp:miuix-icons-android:0.9.0")
    implementation("top.yukonga.miuix.kmp:miuix-shapes-android:0.9.0")
    implementation("top.yukonga.miuix.kmp:miuix-blur-android:0.9.0")
    implementation("top.yukonga.miuix.kmp:miuix-navigation3-ui-android:0.9.0")
    implementation("io.github.kyant0:backdrop:1.0.6")
    implementation("io.github.kyant0:capsule:2.1.3")
    implementation("io.github.kyant0:shapes:1.2.0")

    implementation("dev.rikka.shizuku:api:13.1.5")
    implementation("dev.rikka.shizuku:provider:13.1.5")
    implementation("com.tencent:mmkv:2.4.0")
    implementation("io.modelcontextprotocol:kotlin-sdk:0.11.0")
    implementation("io.ktor:ktor-server-cio:3.4.2")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:okhttp-dnsoverhttps:4.12.0")
    implementation("androidx.media3:media3-exoplayer:1.5.1")
    implementation("com.xzakota.hyper.notification:focus-api:1.4")
}
