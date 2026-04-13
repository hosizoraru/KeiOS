pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
// Cross-platform Gradle daemon JVM resolution is handled by Foojay + gradle/gradle-daemon-jvm.properties.
// Do not commit a machine-specific org.gradle.java.home path; keep that as a local override only when
// Android Studio / Gradle automatic JDK resolution is unavailable on the developer machine.
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        maven(url = "https://maven.pkg.github.com/compose-miuix-ui/miuix") {
            credentials {
                username = providers.gradleProperty("gpr.user")
                    .orElse(System.getenv("GITHUB_ACTOR"))
                    .get()
                password = providers.gradleProperty("gpr.key")
                    .orElse(System.getenv("GITHUB_TOKEN"))
                    .get()
            }
        }
        mavenCentral()
        maven(url = "https://jitpack.io")
    }
}

rootProject.name = "KeiOSDemo"
include(":app")
