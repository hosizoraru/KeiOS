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

data class AppSemVer(
    val major: Int,
    val minor: Int,
    val patch: Int
) {
    val name: String = "$major.$minor.$patch"

    fun toVersionCode(commitCount: Int): Int {
        return (major * 10_000_000) + (minor * 100_000) + (patch * 1_000) + commitCount
    }
}

fun parseSemVerTagOrNull(raw: String?): AppSemVer? {
    val normalized = raw?.trim().orEmpty()
    val match = Regex("""^v?(\d+)\.(\d+)\.(\d+)$""").matchEntire(normalized) ?: return null
    val (major, minor, patch) = match.destructured
    return AppSemVer(
        major = major.toInt(),
        minor = minor.toInt(),
        patch = patch.toInt()
    )
}

fun latestMergedSemVerTagOrNull(): String? {
    return runGitCommandOrNull("tag", "--merged", "HEAD", "--sort=-v:refname")
        ?.lineSequence()
        ?.map { it.trim() }
        ?.firstOrNull { parseSemVerTagOrNull(it) != null }
}

data class GitVersionSnapshot(
    val relativeCommitCount: Int,
    val totalCommitCount: Int,
    val shortHash: String,
    val branchName: String,
    val worktreeDirty: Boolean,
    val gitAvailable: Boolean
)

fun readLocalPropertyOrNull(key: String): String? {
    val localPropsFile = rootProject.file("local.properties")
    if (!localPropsFile.exists()) return null
    return runCatching {
        val props = Properties()
        localPropsFile.inputStream().use(props::load)
        props.getProperty(key)
    }.getOrNull()
}

fun gitRelativeCommitCountOrNull(anchorTag: String): Int? {
    return runGitCommandOrNull("rev-list", "--count", "$anchorTag..HEAD")?.toIntOrNull()
}

fun gitTotalCommitCountOrNull(): Int? {
    return runGitCommandOrNull("rev-list", "--count", "HEAD")?.toIntOrNull()
}

val fallbackReleaseVersion = AppSemVer(major = 1, minor = 0, patch = 0)
val versionAnchorTag = latestMergedSemVerTagOrNull() ?: "v${fallbackReleaseVersion.name}"
val releaseVersion = parseSemVerTagOrNull(versionAnchorTag) ?: fallbackReleaseVersion
val nonReleaseVersion = releaseVersion.copy(patch = releaseVersion.patch + 1)
val gitShortHashValue = runGitCommandOrNull("rev-parse", "--short", "HEAD")
val gitBranchNameValue = runGitCommandOrNull("rev-parse", "--abbrev-ref", "HEAD")
val gitDirtyValue = runGitCommandOrNull("status", "--porcelain")?.isNotBlank() ?: false
val gitRelativeCommitCount = gitRelativeCommitCountOrNull(versionAnchorTag)
val gitTotalCommitCount = gitTotalCommitCountOrNull()
val gitVersionSnapshot = if (
    gitShortHashValue != null &&
    gitBranchNameValue != null &&
    gitRelativeCommitCount != null &&
    gitTotalCommitCount != null
) {
    GitVersionSnapshot(
        relativeCommitCount = gitRelativeCommitCount,
        totalCommitCount = gitTotalCommitCount,
        shortHash = gitShortHashValue,
        branchName = gitBranchNameValue,
        worktreeDirty = gitDirtyValue,
        gitAvailable = true
    )
} else {
    GitVersionSnapshot(
        relativeCommitCount = 0,
        totalCommitCount = 0,
        shortHash = "unknown",
        branchName = "unknown",
        worktreeDirty = false,
        gitAvailable = false
    )
}
val buildTimestampMillis = System.currentTimeMillis()
val releaseVersionName = releaseVersion.name
val releaseVersionCode = releaseVersion.toVersionCode(commitCount = 0)
val nonReleaseVersionName = if (gitVersionSnapshot.gitAvailable) {
    "${nonReleaseVersion.name}+${gitVersionSnapshot.relativeCommitCount}.g${gitVersionSnapshot.shortHash}"
} else {
    "${nonReleaseVersion.name}+0.unknown"
}
val nonReleaseVersionCode = nonReleaseVersion.toVersionCode(
    commitCount = gitVersionSnapshot.relativeCommitCount
)
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
        ?: "0.9.0-81ad71b1-SNAPSHOT"
val coreKtxVersion = "1.18.0"
val activityComposeVersion = "1.13.0"
val materialVersion = "1.13.0"
val composeVersion = "1.10.6"
val navigation3Version = "1.1.0"
val navigationCommonVersion = "2.9.7"
val backdropVersion = "1.0.6"
val capsuleVersion = "2.1.3"
val shapesVersion = "1.2.0"
val liquidGlassVersion = "1.0.3"
val shizukuVersion = "13.1.5"
val mmkvVersion = "2.4.0"
val mcpKotlinSdkVersion = "0.11.1"
val ktorVersion = "3.4.2"
val okhttpVersion = "5.3.2"
val jsonVersion = "20251224"
val xmlPullVersion = "1.1.3.4d_b4_min"
val kxml2Version = "2.3.0"
val media3Version = "1.10.0"
val coil3Version = "3.4.0"
val zoomImageVersion = "1.4.0"
val lucideIconsVersion = "2.2.1"
val documentFileVersion = "1.1.0"
val uCropVersion = "2.2.11"
val focusApiVersion = "1.4"
val metricsPerformanceVersion = "1.0.0"
val lifecycleViewModelComposeVersion = "2.10.0"
val robolectricVersion = "4.16.1"
val androidTestExtJunitVersion = "1.3.0"
val roborazziVersion = "1.59.0"
val projectCompileSdk = 37
val projectMinSdk = 35
val projectTargetSdk = 37
val projectGradleVersion = gradle.gradleVersion
val projectJavaVersion = JavaVersion.VERSION_21
val projectJvmTarget = JvmTarget.JVM_21

plugins {
    id("com.android.application")
    id("io.github.takahirom.roborazzi")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "os.kei"
    compileSdk = projectCompileSdk

    signingConfigs {
        getByName("debug") {
            storeFile = file("signing/keios-ci-debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
    }

    defaultConfig {
        applicationId = "os.kei"
        minSdk = projectMinSdk
        targetSdk = projectTargetSdk
        versionCode = releaseVersionCode
        versionName = releaseVersionName
        ndk {
            abiFilters += "arm64-v8a"
        }
        buildConfigField("String", "CORE_KTX_VERSION", "\"$coreKtxVersion\"")
        buildConfigField("String", "ACTIVITY_COMPOSE_VERSION", "\"$activityComposeVersion\"")
        buildConfigField("String", "MATERIAL_VERSION", "\"$materialVersion\"")
        buildConfigField("String", "MIUIX_VERSION", "\"$miuixVersion\"")
        buildConfigField("String", "COMPOSE_VERSION", "\"$composeVersion\"")
        buildConfigField("String", "NAVIGATION3_VERSION", "\"$navigation3Version\"")
        buildConfigField("String", "NAVIGATION_COMMON_VERSION", "\"$navigationCommonVersion\"")
        buildConfigField("String", "BACKDROP_VERSION", "\"$backdropVersion\"")
        buildConfigField("String", "CAPSULE_VERSION", "\"$capsuleVersion\"")
        buildConfigField("String", "SHAPES_VERSION", "\"$shapesVersion\"")
        buildConfigField("String", "LIQUID_GLASS_VERSION", "\"$liquidGlassVersion\"")
        buildConfigField("String", "MMKV_VERSION", "\"$mmkvVersion\"")
        buildConfigField("String", "MCP_KOTLIN_SDK_VERSION", "\"$mcpKotlinSdkVersion\"")
        buildConfigField("String", "KTOR_VERSION", "\"$ktorVersion\"")
        buildConfigField("String", "OKHTTP_VERSION", "\"$okhttpVersion\"")
        buildConfigField("String", "MEDIA3_VERSION", "\"$media3Version\"")
        buildConfigField("String", "ZOOMIMAGE_VERSION", "\"$zoomImageVersion\"")
        buildConfigField("String", "COIL3_VERSION", "\"$coil3Version\"")
        buildConfigField("String", "LUCIDE_ICONS_VERSION", "\"$lucideIconsVersion\"")
        buildConfigField("String", "UCROP_VERSION", "\"$uCropVersion\"")
        buildConfigField("String", "LIFECYCLE_VIEWMODEL_COMPOSE_VERSION", "\"$lifecycleViewModelComposeVersion\"")
        buildConfigField("String", "METRICS_PERFORMANCE_VERSION", "\"$metricsPerformanceVersion\"")
        buildConfigField("String", "DOCUMENTFILE_VERSION", "\"$documentFileVersion\"")
        buildConfigField("String", "SHIZUKU_VERSION", "\"$shizukuVersion\"")
        buildConfigField("String", "FOCUS_API_VERSION", "\"$focusApiVersion\"")
        buildConfigField("String", "GRADLE_VERSION", "\"$projectGradleVersion\"")
        buildConfigField("String", "BASE_VERSION_NAME", "\"${releaseVersion.name}\"")
        buildConfigField("String", "NEXT_VERSION_NAME", "\"${nonReleaseVersion.name}\"")
        buildConfigField("String", "VERSION_ANCHOR_TAG", "\"$versionAnchorTag\"")
        buildConfigField("long", "BUILD_TIME_MILLIS", "${buildTimestampMillis}L")
        buildConfigField("int", "GIT_COMMIT_COUNT", gitVersionSnapshot.relativeCommitCount.toString())
        buildConfigField("int", "GIT_TOTAL_COMMIT_COUNT", gitVersionSnapshot.totalCommitCount.toString())
        buildConfigField("String", "GIT_SHORT_HASH", "\"${gitVersionSnapshot.shortHash}\"")
        buildConfigField("String", "GIT_BRANCH_NAME", "\"${gitVersionSnapshot.branchName}\"")
        buildConfigField("boolean", "GIT_WORKTREE_DIRTY", gitVersionSnapshot.worktreeDirty.toString())
        buildConfigField("boolean", "VERSION_GIT_AVAILABLE", gitVersionSnapshot.gitAvailable.toString())
        buildConfigField("int", "COMPILE_SDK_VERSION", projectCompileSdk.toString())
        buildConfigField("int", "MIN_SDK_VERSION", projectMinSdk.toString())
        buildConfigField("int", "TARGET_SDK_VERSION", projectTargetSdk.toString())
        buildConfigField("String", "JAVA_VERSION", "\"${projectJavaVersion.majorVersion}\"")
        buildConfigField("String", "JVM_TARGET_VERSION", "\"${projectJvmTarget.target}\"")
        buildConfigField("boolean", "LOG_DEBUG_DEFAULT", "false")

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        getByName("debug") {
            applicationIdSuffix = ".debug"
            buildConfigField("boolean", "LOG_DEBUG_DEFAULT", "true")
        }

        release {
            isMinifyEnabled = true
            isShrinkResources = true
            buildConfigField("boolean", "LOG_DEBUG_DEFAULT", "false")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }

        create("benchmark") {
            initWith(getByName("release"))
            applicationIdSuffix = ".benchmark"
            signingConfig = signingConfigs.getByName("debug")
            isDebuggable = false
            buildConfigField("boolean", "LOG_DEBUG_DEFAULT", "false")
            matchingFallbacks += listOf("release")
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
        unitTests.isIncludeAndroidResources = true
        unitTests.all {
            // Keep unit tests on the desktop OkHttp platform. Live GitHub tests read secrets from
            // JVM properties, env vars, or ~/.gradle/gradle.properties; see README.md.
            it.systemProperty("okhttp.platform", "jdk9")
        }
    }
}

androidComponents {
    onVariants(selector().withBuildType("release")) { variant ->
        variant.outputs.forEach { output ->
            output.versionName.set(releaseVersionName)
            output.versionCode.set(releaseVersionCode)
        }
    }
    onVariants(selector().withBuildType("debug")) { variant ->
        variant.outputs.forEach { output ->
            output.versionName.set(nonReleaseVersionName)
            output.versionCode.set(nonReleaseVersionCode)
        }
    }
    onVariants(selector().withBuildType("benchmark")) { variant ->
        variant.outputs.forEach { output ->
            output.versionName.set(nonReleaseVersionName)
            output.versionCode.set(nonReleaseVersionCode)
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
    implementation("androidx.core:core-ktx:$coreKtxVersion")
    implementation("androidx.activity:activity-compose:$activityComposeVersion")
    implementation("com.google.android.material:material:$materialVersion")

    implementation("androidx.compose.ui:ui:$composeVersion")
    implementation("androidx.compose.foundation:foundation:$composeVersion")
    implementation("androidx.compose.ui:ui-tooling-preview:$composeVersion")
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
    implementation("com.qmdeve.liquidglass:core:$liquidGlassVersion")

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
    implementation("io.coil-kt.coil3:coil-compose:$coil3Version")
    implementation("io.coil-kt.coil3:coil-gif:$coil3Version")
    implementation("com.composables:icons-lucide-android:$lucideIconsVersion")
    implementation("com.github.yalantis:ucrop:$uCropVersion")
    implementation("androidx.metrics:metrics-performance:$metricsPerformanceVersion")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:$lifecycleViewModelComposeVersion")
    implementation("androidx.documentfile:documentfile:$documentFileVersion")
    implementation("com.xzakota.hyper.notification:focus-api:$focusApiVersion")

    // Keep kotlin-test aligned with the applied Kotlin plugin version to avoid version skew.
    testImplementation(kotlin("test"))
    testImplementation("junit:junit:4.13.2")
    testImplementation("androidx.compose.ui:ui-test-junit4:$composeVersion")
    testImplementation("androidx.test.ext:junit:$androidTestExtJunitVersion")
    testImplementation("org.json:json:$jsonVersion")
    testImplementation("org.robolectric:robolectric:$robolectricVersion")
    testImplementation("io.github.takahirom.roborazzi:roborazzi:$roborazziVersion")
    testImplementation("io.github.takahirom.roborazzi:roborazzi-compose:$roborazziVersion")
    testImplementation("com.squareup.okhttp3:mockwebserver:$okhttpVersion")
    testImplementation("xmlpull:xmlpull:$xmlPullVersion")
    testImplementation("net.sf.kxml:kxml2:$kxml2Version")
    debugImplementation("androidx.compose.ui:ui-test-manifest:$composeVersion")
}
