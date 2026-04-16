# ---------- KeiOS R8 baseline ----------
# Keep enum member names stable because multiple stores persist enum.name across app restarts/upgrades.
-keepclassmembernames enum com.example.keios.** { *; }

# Keep manifest component class names stable for runtime introspection in About page.
-keepnames class com.example.keios.MainActivity
-keepnames class com.example.keios.mcp.McpKeepAliveService
-keepnames class com.example.keios.feature.notification.NotificationActionReceiver
-keepnames class com.example.keios.core.background.AppBackgroundTickReceiver

# ShizukuApiUtils uses reflection on Shizuku method names.
-keep class rikka.shizuku.Shizuku { *; }

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
