# KeiOS

## Local Build Notes

This project intentionally keeps machine-specific paths and secrets out of version control.

- The project now targets Java 21 consistently for Gradle daemon, Java compilation, and Kotlin JVM bytecode.
- This project does not hardcode a macOS-only or Windows-only JDK path.
- Gradle daemon JVM is tracked in `gradle/gradle-daemon-jvm.properties` and currently targets JetBrains Java 21 for macOS, Windows, and Linux.
- In Android Studio, importing the project should normally work with the IDE's bundled JBR or the daemon JVM/toolchain resolution.
- Put local Gradle/JDK overrides in `~/.gradle/gradle.properties`, not in the tracked project files.
- Only if automatic JDK resolution fails on your machine should you add `org.gradle.java.home=/path/to/your/jdk` to `~/.gradle/gradle.properties`.
- `miuix.version` can be overridden from `~/.gradle/gradle.properties` or `local.properties`.

Example `~/.gradle/gradle.properties`:

```properties
org.gradle.java.home=/path/to/your/jdk
miuix.version=0.9.0
```

Fallback examples:

- macOS Android Studio JBR:
  `/Applications/Android Studio.app/Contents/jbr/Contents/Home`
- Windows Android Studio JBR:
  `C:\\Program Files\\Android\\Android Studio\\jbr`

## GitHub Live Benchmark Test

The live GitHub benchmark unit test is opt-in and reads local-only values from:

1. JVM system properties
2. Environment variables
3. `~/.gradle/gradle.properties`

Useful keys:

```properties
keios.github.liveBenchmark=true
keios.github.api.token=ghp_xxx
keios.github.liveTargets=topjohnwu/Magisk,neovim/neovim,shadowsocks/shadowsocks-android
keios.github.forceGuest=false
```

`gpr.key` is also accepted as a fallback token for the live benchmark test.

Run the live benchmark from Android Studio or CLI:

```bash
./gradlew :app:testDebugUnitTest --tests "com.example.keios.feature.github.data.remote.GitHubStrategyLiveBenchmarkTest"
```
