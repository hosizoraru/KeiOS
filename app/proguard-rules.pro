# ---------- KeiOS R8 baseline ----------
# Keep enum member names stable because multiple stores persist enum.name across app restarts/upgrades.
-keepclassmembernames enum os.kei.** { *; }

# Keep internal component class names stable for manifest lookups and About-page introspection.
# This covers future KeiOS activities/services/receivers/providers without needing manual updates.
-keepnames class os.kei.** extends android.app.Application
-keepnames class os.kei.** extends android.app.Activity
-keepnames class os.kei.** extends android.app.Service
-keepnames class os.kei.** extends android.content.BroadcastReceiver
-keepnames class os.kei.** extends android.content.ContentProvider

# Keep navigation key class names stable across save/restore boundaries.
# The route surface is tiny, and preserving names keeps release-only navigation debugging clearer.
-keepnames class os.kei.ui.navigation.KeiosRoute
-keepnames class os.kei.ui.navigation.KeiosRoute$*

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

# focus-api ships without consumer rules, but its HyperOS payload builder relies on:
# 1) kotlinx.serialization for miui.focus.param JSON
# 2) a sealed factory whose fully qualified subclass name becomes the JSON "type"
# 3) declaredFields reflection when copying template state
# Preserve the library surface so release builds keep the same island/live-update payload shape.
-keep class com.xzakota.hyper.notification.** { *; }
-keepnames class com.xzakota.hyper.notification.focus.FocusNotification
-keepnames class com.xzakota.hyper.notification.focus.FocusNotification$FocusTemplateFactory
-keepnames class com.xzakota.hyper.notification.focus.FocusNotification$FocusTemplateFactory$*
-keep class com.xzakota.hyper.notification.**$$serializer { *; }
-keepclassmembers class com.xzakota.hyper.notification.** {
    public static ** Companion;
    public static ** INSTANCE;
    public static *** serializer(...);
    <fields>;
}

# Keep our thin Focus builders readable and stable around the payload boundary.
# The system only consumes the Bundle/JSON emitted by focus-api; these helpers should not
# be reshaped in ways that make release-only notification debugging opaque.
-keep class os.kei.core.notification.focus.** { *; }
-keep class os.kei.mcp.framework.notification.builder.MiIslandNotificationBuilder { *; }
-keep class os.kei.mcp.framework.notification.builder.MiIslandNotificationBuilder$* { *; }
-keep class os.kei.feature.github.notification.GitHubRefreshNotificationHelper { *; }
-keep class os.kei.feature.github.notification.GitHubRefreshNotificationHelper$* { *; }

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
