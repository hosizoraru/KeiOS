# ---------- KeiOS R8 baseline ----------
# Keep enum member names stable because multiple stores persist enum.name across app restarts/upgrades.
-keepclassmembernames enum com.example.keios.** { *; }

# Keep manifest component class names stable for runtime introspection in About page.
-keepnames class com.example.keios.KeiOSApp
-keepnames class com.example.keios.MainActivity
-keepnames class com.example.keios.ui.page.main.github.share.GitHubShareImportActivity
-keepnames class com.example.keios.ui.page.main.os.shell.OsShellRunnerActivity
-keepnames class com.example.keios.ui.page.main.student.GuideVideoFullscreenActivity
-keepnames class com.example.keios.mcp.service.McpKeepAliveService
-keepnames class com.example.keios.feature.notification.NotificationActionReceiver
-keepnames class com.example.keios.core.background.AppBackgroundTickReceiver

# ShizukuApiUtils reflects these no-arg static method names.
# Keep only the reflective surface so the rest can still be optimized/shrunk.
-keepclassmembers class rikka.shizuku.Shizuku {
    public static *** getUid(...);
    public static *** getVersion(...);
    public static *** getServerPatchVersion(...);
    public static *** getSELinuxContext(...);
    public static *** getLatestServiceVersion(...);
    private static *** newProcess(java.lang.String[],java.lang.String[],java.lang.String);
}

# Keep annotation/signature metadata used by Kotlin + library runtime features.
-keepattributes Signature,InnerClasses,EnclosingMethod,*Annotation*

# Drop release log calls to reduce overhead and method count.
-assumenosideeffects class android.util.Log {
    public static int v(...);
    public static int d(...);
    public static int i(...);
    public static int w(...);
    public static int e(...);
}

# Ktor debug probe references JDK-only management APIs on Android.
-dontwarn java.lang.management.ManagementFactory
-dontwarn java.lang.management.RuntimeMXBean
