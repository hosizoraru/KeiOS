import com.android.build.api.variant.BuildConfigField
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import os.kei.buildlogic.AppSemVer
import os.kei.buildlogic.countGeneratedProfileRules
import os.kei.buildlogic.miuixVersion
import os.kei.buildlogic.readGradleOrLocalPropertyOrNull
import os.kei.buildlogic.resolveKeiosVersionMetadata

val versionMetadata = resolveKeiosVersionMetadata(AppSemVer(major = 1, minor = 17, patch = 0))
val releaseVersion = versionMetadata.releaseVersion
val benchmarkVersion = versionMetadata.benchmarkVersion
val versionAnchorTag = versionMetadata.versionAnchorTag
val gitVersionSnapshot = versionMetadata.gitVersionSnapshot
val buildTimestampMillisProvider = versionMetadata.buildTimestampMillisProvider
val commitTimestampMillis = versionMetadata.commitTimestampMillis
val releaseVersionName = versionMetadata.releaseVersionName
val releaseVersionCode = versionMetadata.releaseVersionCode
val nonReleaseVersionName = versionMetadata.nonReleaseVersionName
val preReleaseVersionCode = versionMetadata.preReleaseVersionCode

// Machine-local overrides should live in ~/.gradle/gradle.properties (preferred) or local.properties.
// JDK resolution itself is intentionally not hardcoded here: the project already tracks a cross-platform
// Gradle daemon JVM (JetBrains Java 21) for macOS/Windows/Linux. Use org.gradle.java.home only as a
// developer-local fallback when Android Studio or Gradle cannot auto-resolve a suitable JDK.
// Useful local-only keys include:
// - miuix.version
// - keios.release.storeFile
// - keios.release.storePassword
// - keios.release.keyAlias
// - keios.release.keyPassword
val miuixVersion = miuixVersion()
val coreKtxVersion = libs.versions.androidx.core.get()
val activityComposeVersion = libs.versions.activity.compose.get()
val materialVersion = libs.versions.material.get()
val composeVersion = libs.versions.compose.get()
val constraintLayoutComposeVersion = libs.versions.constraintlayout.compose.get()
val navigationEventVersion = libs.versions.navigation.event.get()
val backdropVersion = libs.versions.backdrop.get()
val capsuleVersion = libs.versions.capsule.get()
val shapesVersion = libs.versions.shapes.get()
val releaseSigningStoreFile = readGradleOrLocalPropertyOrNull("keios.release.storeFile")?.trim().orEmpty()
val releaseSigningStorePassword = readGradleOrLocalPropertyOrNull("keios.release.storePassword")?.trim().orEmpty()
val releaseSigningKeyAlias = readGradleOrLocalPropertyOrNull("keios.release.keyAlias")?.trim().orEmpty()
val releaseSigningKeyPassword = readGradleOrLocalPropertyOrNull("keios.release.keyPassword")?.trim().orEmpty()
val releaseSigningConfigured =
    releaseSigningStoreFile.isNotBlank() &&
        releaseSigningStorePassword.isNotBlank() &&
        releaseSigningKeyAlias.isNotBlank() &&
        releaseSigningKeyPassword.isNotBlank()
val shizukuVersion = libs.versions.shizuku.get()
val hiddenApiBypassVersion = libs.versions.hidden.api.bypass.get()
val mmkvVersion = libs.versions.mmkv.get()
val mcpKotlinSdkVersion = libs.versions.mcp.kotlin.sdk.get()
val ktorVersion = libs.versions.ktor.get()
val okhttpVersion = libs.versions.okhttp.get()
val media3Version = libs.versions.media3.get()
val dav4jvmVersion = libs.versions.dav4jvm.get()
val coil3Version = libs.versions.coil3.get()
val zoomImageVersion = libs.versions.zoomimage.get()
val lucideIconsVersion = libs.versions.lucide.icons.get()
val documentFileVersion = libs.versions.documentfile.get()
val uCropVersion = libs.versions.ucrop.get()
val focusApiVersion = libs.versions.focus.api.get()
val metricsPerformanceVersion = libs.versions.metrics.performance.get()
val profileInstallerVersion = libs.versions.profileinstaller.get()
val lifecycleViewModelComposeVersion = libs.versions.lifecycle.get()
val projectCompileSdk = libs.versions.compile.sdk.get().toInt()
val projectMinSdk = libs.versions.min.sdk.get().toInt()
val projectTargetSdk = libs.versions.target.sdk.get().toInt()
val projectGradleVersion = gradle.gradleVersion
val projectJavaVersion = JavaVersion.toVersion(libs.versions.java.get())
val projectJvmTarget = JvmTarget.fromTarget(libs.versions.java.get())
val r8DexStartupOptimizationProperty = "android.experimental.r8.dex-startup-optimization"

val baselineProfileRuleCount = countGeneratedProfileRules("baseline-prof.txt")
val startupProfileRuleCount = countGeneratedProfileRules("startup-prof.txt")

plugins {
    id("keios.android.application")
    alias(libs.plugins.androidx.baselineprofile)
    alias(libs.plugins.roborazzi)
    id("keios.android.compose")
    id("keios.miuix")
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "os.kei"

    signingConfigs {
        getByName("debug") {
            storeFile = file("signing/keios-ci-debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
        if (releaseSigningConfigured) {
            create("release") {
                storeFile = file(releaseSigningStoreFile)
                storePassword = releaseSigningStorePassword
                keyAlias = releaseSigningKeyAlias
                keyPassword = releaseSigningKeyPassword
            }
        }
    }

    defaultConfig {
        applicationId = "os.kei"
        versionCode = releaseVersionCode
        versionName = releaseVersionName
        ndk {
            //noinspection ChromeOsAbiSupport
            abiFilters += "arm64-v8a"
        }
        buildConfigField("String", "CORE_KTX_VERSION", "\"$coreKtxVersion\"")
        buildConfigField("String", "ACTIVITY_COMPOSE_VERSION", "\"$activityComposeVersion\"")
        buildConfigField("String", "MATERIAL_VERSION", "\"$materialVersion\"")
        buildConfigField("String", "MIUIX_VERSION", "\"$miuixVersion\"")
        buildConfigField("String", "MIUIX_NAV_VERSION", "\"miuix-nav $miuixVersion\"")
        buildConfigField("String", "COMPOSE_VERSION", "\"$composeVersion\"")
        buildConfigField("String", "CONSTRAINT_LAYOUT_COMPOSE_VERSION", "\"$constraintLayoutComposeVersion\"")
        buildConfigField("String", "NAVIGATION_EVENT_VERSION", "\"$navigationEventVersion\"")
        buildConfigField("String", "BACKDROP_VERSION", "\"$backdropVersion\"")
        buildConfigField("String", "CAPSULE_VERSION", "\"$capsuleVersion\"")
        buildConfigField("String", "SHAPES_VERSION", "\"$shapesVersion\"")
        buildConfigField("String", "HIDDENAPI_BYPASS_VERSION", "\"$hiddenApiBypassVersion\"")
        buildConfigField("String", "MMKV_VERSION", "\"$mmkvVersion\"")
        buildConfigField("String", "MCP_KOTLIN_SDK_VERSION", "\"$mcpKotlinSdkVersion\"")
        buildConfigField("String", "KTOR_VERSION", "\"$ktorVersion\"")
        buildConfigField("String", "OKHTTP_VERSION", "\"$okhttpVersion\"")
        buildConfigField("String", "MEDIA3_VERSION", "\"$media3Version\"")
        buildConfigField("String", "DAV4JVM_VERSION", "\"$dav4jvmVersion\"")
        buildConfigField("String", "ZOOMIMAGE_VERSION", "\"$zoomImageVersion\"")
        buildConfigField("String", "COIL3_VERSION", "\"$coil3Version\"")
        buildConfigField("String", "LUCIDE_ICONS_VERSION", "\"$lucideIconsVersion\"")
        buildConfigField("String", "UCROP_VERSION", "\"$uCropVersion\"")
        buildConfigField("String", "LIFECYCLE_VIEWMODEL_COMPOSE_VERSION", "\"$lifecycleViewModelComposeVersion\"")
        buildConfigField("String", "METRICS_PERFORMANCE_VERSION", "\"$metricsPerformanceVersion\"")
        buildConfigField("String", "PROFILE_INSTALLER_VERSION", "\"$profileInstallerVersion\"")
        buildConfigField("String", "DOCUMENTFILE_VERSION", "\"$documentFileVersion\"")
        buildConfigField("String", "SHIZUKU_VERSION", "\"$shizukuVersion\"")
        buildConfigField("String", "FOCUS_API_VERSION", "\"$focusApiVersion\"")
        buildConfigField("String", "GRADLE_VERSION", "\"$projectGradleVersion\"")
        buildConfigField("String", "BASE_VERSION_NAME", "\"${releaseVersion.name}\"")
        buildConfigField("String", "NEXT_VERSION_NAME", "\"${benchmarkVersion.name}\"")
        buildConfigField("String", "VERSION_ANCHOR_TAG", "\"$versionAnchorTag\"")
        buildConfigField("String", "MANIFEST_COMPONENT_PACKAGE", "\"$namespace\"")
        buildConfigField("long", "COMMIT_TIME_MILLIS", "${commitTimestampMillis}L")
        buildConfigField("int", "GIT_COMMIT_COUNT", gitVersionSnapshot.relativeCommitCount.toString())
        buildConfigField("int", "GIT_TOTAL_COMMIT_COUNT", gitVersionSnapshot.totalCommitCount.toString())
        buildConfigField("String", "GIT_SHORT_HASH", "\"${gitVersionSnapshot.shortHash}\"")
        buildConfigField("String", "GIT_BRANCH_NAME", "\"${gitVersionSnapshot.branchName}\"")
        buildConfigField("boolean", "GIT_WORKTREE_DIRTY", gitVersionSnapshot.worktreeDirty.toString())
        buildConfigField("boolean", "VERSION_GIT_AVAILABLE", gitVersionSnapshot.gitAvailable.toString())
        buildConfigField("int", "COMPILE_SDK_VERSION", projectCompileSdk.toString())
        buildConfigField("int", "MIN_SDK_VERSION", projectMinSdk.toString())
        buildConfigField("int", "TARGET_SDK_VERSION", projectTargetSdk.toString())
        buildConfigField("int", "BASELINE_PROFILE_RULE_COUNT", baselineProfileRuleCount.toString())
        buildConfigField("int", "STARTUP_PROFILE_RULE_COUNT", startupProfileRuleCount.toString())
        buildConfigField("String", "JAVA_VERSION", "\"${projectJavaVersion.majorVersion}\"")
        buildConfigField("String", "JVM_TARGET_VERSION", "\"${projectJvmTarget.target}\"")
        buildConfigField("String", "DEFAULT_LOG_LEVEL_ID", "\"off\"")

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        getByName("debug") {
            applicationIdSuffix = ".debug"
            buildConfigField("String", "DEFAULT_LOG_LEVEL_ID", "\"debug\"")
        }

        release {
            optimization.enable = true
            // Additive: AGP's own optimized defaults still arrive through optimization.enable, verified
            // by diffing R8's configuration.txt across this change.
            proguardFile("proguard-rules.pro")
            if (releaseSigningConfigured) {
                signingConfig = signingConfigs.getByName("release")
            }
            buildConfigField("String", "DEFAULT_LOG_LEVEL_ID", "\"off\"")
        }

        create("nonMinifiedRelease") {
            initWith(getByName("release"))
            // Connected profile tasks install and uninstall their target. Never share user data
            // with release, debug or the persistent diagnostic package on a physical device.
            applicationIdSuffix = ".profilecapture"
            versionNameSuffix = "-profilecapture"
            optimization.enable = false
            isMinifyEnabled = false
            isShrinkResources = false
            isDebuggable = false
            isJniDebuggable = false
            isProfileable = true
            enableAndroidTestCoverage = false
            enableUnitTestCoverage = false
            signingConfig =
                if (releaseSigningConfigured) {
                    signingConfigs.getByName("release")
                } else {
                    signingConfigs.getByName("debug")
                }
            matchingFallbacks += listOf("release")
            buildConfigField("String", "DEFAULT_LOG_LEVEL_ID", "\"off\"")
        }

        /**
         * A release build that installs *beside* the real one, for A/B measurement.
         *
         * Frame-time work means building the same app with one thing changed and comparing. Doing
         * that by overwriting `os.kei` means the device only ever holds one of the two, the previous
         * build has to be rebuilt to go back, and — the part that actually caused trouble — a
         * diagnostic with the glass switched off can be left sitting on the device looking like a
         * shipped regression.
         *
         * `.diag` keeps both installed at once, and `src/releaseDiagnostic/res` overrides the launcher
         * label so they are told apart at a glance — a `resValue` would collide with the `app_name`
         * that `src/main` already declares. Identical to release otherwise, R8 included, so the numbers are comparable:
         * a diagnostic that optimises differently from release measures the wrong app.
         */
        create("releaseDiagnostic") {
            initWith(getByName("release"))
            applicationIdSuffix = ".diag"
            versionNameSuffix = "-diag"
            signingConfig =
                if (releaseSigningConfigured) {
                    signingConfigs.getByName("release")
                } else {
                    signingConfigs.getByName("debug")
                }
            matchingFallbacks += listOf("release")
            buildConfigField("String", "DEFAULT_LOG_LEVEL_ID", "\"off\"")
        }

        create("benchmark") {
            initWith(getByName("release"))
            signingConfig =
                if (releaseSigningConfigured) {
                    signingConfigs.getByName("release")
                } else {
                    signingConfigs.getByName("debug")
                }
            matchingFallbacks += listOf("release")
            buildConfigField("String", "DEFAULT_LOG_LEVEL_ID", "\"off\"")
        }

        maybeCreate("benchmarkRelease").apply {
            initWith(getByName("benchmark"))
            matchingFallbacks.clear()
            matchingFallbacks += listOf("release")
            buildConfigField("String", "DEFAULT_LOG_LEVEL_ID", "\"off\"")
        }
    }


    buildFeatures {
        buildConfig = true
    }

    lint {
        abortOnError = true
        checkDependencies = false
    }

    packaging {
        jniLibs {
            excludes += "lib/*/libandroidx.graphics.path.so"
            keepDebugSymbols += "**/libmmkv.so"
        }
    }


    testOptions {
        unitTests.isIncludeAndroidResources = true
        unitTests.all {
            // Keep unit tests on the desktop OkHttp platform.
            it.systemProperty("okhttp.platform", "jdk9")
            // Two JVMs split :app's suite, which sets the whole project's wall time. Each fork pays its own
            // Robolectric startup, so more stops paying: `:app:testDebugUnitTest --rerun`, two runs each,
            // 2026-09-25 on a 12-core Mac: 1 fork 36s/31s, 2 forks 27s/26s, 4 forks 27s/25s, 6 forks 32s/29s.
            // CI's runner has 4 vCPUs.
            it.maxParallelForks = 2
        }
    }
}

androidComponents {
    finalizeDsl { android ->
        // AndroidX copies release's manifest onto its derived collector source set. Restore the
        // collector-only overlay after that copy; no launch harness is exported by release/debug.
        android.sourceSets.getByName("nonMinifiedRelease").manifest.srcFile(
            "src/nonMinifiedRelease/AndroidManifest.xml",
        )
    }
    beforeVariants(selector().withBuildType("nonMinifiedRelease")) { variant ->
        variant.isMinifyEnabled = false
        variant.shrinkResources = false
    }
    onVariants { variant ->
        variant.buildConfigFields?.put(
            "BUILD_TIME_MILLIS",
            buildTimestampMillisProvider.map { buildTimestampMillis ->
                BuildConfigField(
                    type = "long",
                    value = "${buildTimestampMillis}L",
                    comment = "Wall-clock timestamp captured while generating BuildConfig.",
                )
            },
        )
        // Generated startup profiles include D8/R8 synthetic lambda rules that R8 reports as
        // missing before minification. Keep ART baseline profiles enabled and skip dex layout
        // optimization so release-like builds stay quiet and deterministic.
        variant.experimentalProperties.put(r8DexStartupOptimizationProperty, false)
    }
    onVariants(selector().withBuildType("benchmark")) { variant ->
        variant.sources.baselineProfiles?.addStaticSourceDirectory("src/release/generated/baselineProfiles")
    }
    // The diagnostic build has to carry the same ART profile as release, or it is not the app being
    // measured. Without this it looked for `src/releaseDiagnostic/generated`, found nothing, and
    // shipped unprofiled — which measured the BA page ~9ms of RenderThread slower than release built
    // from the identical source.
    onVariants(selector().withBuildType("releaseDiagnostic")) { variant ->
        variant.sources.baselineProfiles?.addStaticSourceDirectory("src/release/generated/baselineProfiles")
    }
    onVariants(selector().withBuildType("benchmarkRelease")) { variant ->
        variant.sources.baselineProfiles?.addStaticSourceDirectory("src/release/generated/baselineProfiles")
    }
    onVariants(selector().withBuildType("release")) { variant ->
        variant.outputs.forEach { output ->
            output.versionName.set(releaseVersionName)
            output.versionCode.set(releaseVersionCode)
        }
    }
    onVariants(selector().withBuildType("debug")) { variant ->
        variant.outputs.forEach { output ->
            output.versionName.set(nonReleaseVersionName)
            output.versionCode.set(preReleaseVersionCode)
        }
    }
    onVariants(selector().withBuildType("nonMinifiedRelease")) { variant ->
        variant.outputs.forEach { output ->
            output.versionName.set(nonReleaseVersionName)
            output.versionCode.set(preReleaseVersionCode)
        }
    }
    onVariants(selector().withBuildType("benchmark")) { variant ->
        variant.outputs.forEach { output ->
            output.versionName.set(nonReleaseVersionName)
            output.versionCode.set(preReleaseVersionCode)
        }
    }
    onVariants(selector().withBuildType("benchmarkRelease")) { variant ->
        variant.outputs.forEach { output ->
            output.versionName.set(nonReleaseVersionName)
            output.versionCode.set(preReleaseVersionCode)
        }
    }
}


dependencies {
    baselineProfile(project(":baselineprofile"))

    implementation(project(":core-concurrency"))
    implementation(project(":core-download"))
    implementation(project(":core-log"))
    implementation(project(":core-io"))
    implementation(project(":core-json"))
    implementation(project(":core-notification"))
    implementation(project(":core-prefs"))
    implementation(project(":core-system"))
    implementation(project(":ui-pip"))
    implementation(project(":ui-liquid-glass"))
    implementation(project(":feature-mcp"))
    implementation(project(":feature-keepalive"))
    implementation(project(":feature-home"))
    implementation(project(":feature-os"))
    implementation(project(":feature-ba"))
    implementation(project(":feature-github"))
    implementation(project(":feature-webdav"))
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.profileinstaller)
    implementation(libs.google.material)

    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.constraintlayout.compose)
    implementation(libs.androidx.navigation.event)
    implementation(libs.androidx.navigation.event.compose)
    debugImplementation(libs.androidx.compose.ui.tooling)

    implementation("top.yukonga.miuix.kmp:miuix-ui-android:$miuixVersion")
    implementation("top.yukonga.miuix.kmp:miuix-preference-android:$miuixVersion")
    implementation("top.yukonga.miuix.kmp:miuix-icons-android:$miuixVersion")
    implementation("top.yukonga.miuix.kmp:miuix-blur-android:$miuixVersion")
    implementation("top.yukonga.miuix.kmp:miuix-nav-android:$miuixVersion")
    implementation(libs.kyant.backdrop)
    implementation(libs.kyant.capsule)
    implementation(libs.kyant.shapes)

    implementation(libs.shizuku.api)
    implementation(libs.shizuku.provider)
    implementation(libs.hidden.api.bypass)
    implementation(libs.mmkv)
    implementation(libs.okhttp)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.media3.exoplayer)
    implementation(libs.media3.session)
    implementation(libs.media3.ui)
    implementation(libs.zoomimage.compose.coil3)
    implementation(libs.coil.compose)
    implementation(libs.coil.gif)
    implementation(libs.lucide.icons)
    implementation(libs.ucrop)
    implementation(libs.androidx.metrics.performance)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.documentfile)
    implementation(libs.androidx.webkit)

    // Keep kotlin-test aligned with the Kotlin plugin version while keeping Android Studio's model explicit.
    testImplementation(libs.kotlin.test)
    testImplementation(libs.junit4)
    testImplementation(libs.androidx.compose.ui.test.junit4)
    testImplementation(libs.androidx.test.ext.junit)
    testImplementation(libs.org.json)
    testImplementation(libs.robolectric)
    testImplementation(libs.roborazzi)
    testImplementation(libs.roborazzi.compose)
    testImplementation(libs.okhttp.mockwebserver)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
