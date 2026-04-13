import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Properties

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

fun readLocalPropertyOrNull(key: String): String? {
    val localPropsFile = rootProject.file("local.properties")
    if (!localPropsFile.exists()) return null
    return runCatching {
        val props = Properties()
        localPropsFile.inputStream().use(props::load)
        props.getProperty(key)
    }.getOrNull()
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
// Machine-local overrides should live in ~/.gradle/gradle.properties (preferred) or local.properties.
// JDK resolution itself is intentionally not hardcoded here: the project already tracks a cross-platform
// Gradle daemon JVM (JetBrains Java 21) for macOS/Windows/Linux. Use org.gradle.java.home only as a
// developer-local fallback when Android Studio or Gradle cannot auto-resolve a suitable JDK.
// Useful local-only keys include:
// - miuix.version
// - keios.github.liveBenchmark
// - keios.github.api.token
// - keios.github.liveTargets
// - keios.github.forceGuest
val miuixVersion =
    providers.gradleProperty("miuix.version").orNull
        ?: readLocalPropertyOrNull("miuix.version")
        ?: "0.9.0-d9dc35b5-SNAPSHOT"
val composeVersion = "1.10.6"
val navigation3Version = "1.1.0"
val navigationCommonVersion = "2.9.7"
val backdropVersion = "1.0.6"
val capsuleVersion = "2.1.3"
val shapesVersion = "1.2.0"
val shizukuVersion = "13.1.5"
val mmkvVersion = "2.4.0"
val mcpKotlinSdkVersion = "0.11.0"
val ktorVersion = "3.4.2"
val okhttpVersion = "5.3.2"
val jsonVersion = "20251224"
val xmlPullVersion = "1.1.3.4d_b4_min"
val kxml2Version = "2.3.0"
val media3Version = "1.10.0"
val zoomImageVersion = "1.4.0"
val focusApiVersion = "1.4"
val projectJavaVersion = JavaVersion.VERSION_21
val projectJvmTarget = JvmTarget.JVM_21

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
        buildConfigField("String", "MIUIX_VERSION", "\"$miuixVersion\"")
        buildConfigField("String", "COMPOSE_VERSION", "\"$composeVersion\"")
        buildConfigField("String", "NAVIGATION3_VERSION", "\"$navigation3Version\"")
        buildConfigField("String", "BACKDROP_VERSION", "\"$backdropVersion\"")
        buildConfigField("String", "CAPSULE_VERSION", "\"$capsuleVersion\"")
        buildConfigField("String", "MMKV_VERSION", "\"$mmkvVersion\"")
        buildConfigField("String", "MCP_KOTLIN_SDK_VERSION", "\"$mcpKotlinSdkVersion\"")
        buildConfigField("String", "KTOR_VERSION", "\"$ktorVersion\"")
        buildConfigField("String", "OKHTTP_VERSION", "\"$okhttpVersion\"")
        buildConfigField("String", "MEDIA3_VERSION", "\"$media3Version\"")
        buildConfigField("String", "ZOOMIMAGE_VERSION", "\"$zoomImageVersion\"")

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
        sourceCompatibility = projectJavaVersion
        targetCompatibility = projectJavaVersion
    }

    buildFeatures {
        buildConfig = true
        compose = true
    }
    compileSdkMinor = 0

    testOptions {
        unitTests.all {
            // Keep unit tests on the desktop OkHttp platform. Live GitHub tests read secrets from
            // JVM properties, env vars, or ~/.gradle/gradle.properties; see README.md.
            it.systemProperty("okhttp.platform", "jdk9")
        }
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(projectJvmTarget)
    }
}

configurations.configureEach {
    resolutionStrategy.dependencySubstitution {
        substitute(module("top.yukonga.miuix.kmp:miuix-ui"))
            .using(module("top.yukonga.miuix.kmp:miuix-ui-android:$miuixVersion"))
        substitute(module("top.yukonga.miuix.kmp:miuix-preference"))
            .using(module("top.yukonga.miuix.kmp:miuix-preference-android:$miuixVersion"))
        substitute(module("top.yukonga.miuix.kmp:miuix-icons"))
            .using(module("top.yukonga.miuix.kmp:miuix-icons-android:$miuixVersion"))
        substitute(module("top.yukonga.miuix.kmp:miuix-shapes"))
            .using(module("top.yukonga.miuix.kmp:miuix-shapes-android:$miuixVersion"))
        substitute(module("top.yukonga.miuix.kmp:miuix-blur"))
            .using(module("top.yukonga.miuix.kmp:miuix-blur-android:$miuixVersion"))
        substitute(module("top.yukonga.miuix.kmp:miuix-navigation3-ui"))
            .using(module("top.yukonga.miuix.kmp:miuix-navigation3-ui-android:$miuixVersion"))
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.18.0")
    implementation("androidx.activity:activity-compose:1.13.0")
    implementation("com.google.android.material:material:1.13.0")

    implementation("androidx.compose.ui:ui:$composeVersion")
    implementation("androidx.compose.foundation:foundation:$composeVersion")
    implementation("androidx.navigation3:navigation3-runtime:$navigation3Version")
    implementation("androidx.navigation:navigation-common-ktx:$navigationCommonVersion")
    debugImplementation("androidx.compose.ui:ui-tooling:$composeVersion")

    implementation("top.yukonga.miuix.kmp:miuix-ui-android:$miuixVersion")
    implementation("top.yukonga.miuix.kmp:miuix-preference-android:$miuixVersion")
    implementation("top.yukonga.miuix.kmp:miuix-icons-android:$miuixVersion")
    implementation("top.yukonga.miuix.kmp:miuix-shapes-android:$miuixVersion")
    implementation("top.yukonga.miuix.kmp:miuix-blur-android:$miuixVersion")
    implementation("top.yukonga.miuix.kmp:miuix-navigation3-ui-android:$miuixVersion")
    implementation("io.github.kyant0:backdrop:$backdropVersion")
    implementation("io.github.kyant0:capsule:$capsuleVersion")
    implementation("io.github.kyant0:shapes:$shapesVersion")

    implementation("dev.rikka.shizuku:api:$shizukuVersion")
    implementation("dev.rikka.shizuku:provider:$shizukuVersion")
    implementation("com.tencent:mmkv:$mmkvVersion")
    implementation("io.modelcontextprotocol:kotlin-sdk:$mcpKotlinSdkVersion")
    implementation("io.ktor:ktor-server-cio:$ktorVersion")
    implementation("com.squareup.okhttp3:okhttp:$okhttpVersion")
    implementation("com.squareup.okhttp3:okhttp-dnsoverhttps:$okhttpVersion")
    implementation("androidx.media3:media3-exoplayer:$media3Version")
    implementation("androidx.media3:media3-ui:$media3Version")
    implementation("io.github.panpf.zoomimage:zoomimage-compose-coil3:$zoomImageVersion")
    implementation("com.xzakota.hyper.notification:focus-api:$focusApiVersion")

    // Keep kotlin-test aligned with the applied Kotlin plugin version to avoid version skew.
    testImplementation(kotlin("test"))
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.json:json:$jsonVersion")
    testImplementation("com.squareup.okhttp3:mockwebserver:$okhttpVersion")
    testImplementation("xmlpull:xmlpull:$xmlPullVersion")
    testImplementation("net.sf.kxml:kxml2:$kxml2Version")
}
