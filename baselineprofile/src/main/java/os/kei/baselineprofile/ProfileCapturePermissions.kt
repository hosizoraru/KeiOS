package os.kei.baselineprofile

import android.app.AppOpsManager
import android.content.pm.PackageManager
import android.content.pm.PermissionInfo
import android.util.Log
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice

/** Runs once after UTP installs the two disposable APKs, before any compilation or journey. */
internal fun prepareProfileCapturePermissions() {
    val instrumentation = InstrumentationRegistry.getInstrumentation()
    val context = instrumentation.context
    check(context.packageName == "os.kei.baselineprofile.capture")
    val packageName = profileCaptureAppId()
    val device = UiDevice.getInstance(instrumentation)
    val manager = context.packageManager
    val info = manager.getPackageInfo(packageName, PackageManager.GET_PERMISSIONS)
    val requested = info.requestedPermissions.orEmpty()
    val operations = linkedSetOf<String>()
    requested.filter { it.startsWith("android.permission.") }.forEach { permission ->
        val permissionInfo = try {
            manager.getPermissionInfo(permission, 0)
        } catch (_: PackageManager.NameNotFoundException) {
            // A newer manifest may declare a permission absent from an API 35/36 device.
            Log.i("ProfilePermissions", "$packageName: $permission unsupported on this platform")
            return@forEach
        }
        if (permissionInfo.protection == PermissionInfo.PROTECTION_DANGEROUS) {
            val result = device.executeShellCommand("pm grant --user 0 $packageName $permission")
            check(manager.checkPermission(permission, packageName) == PackageManager.PERMISSION_GRANTED) {
                "Profile permission preflight failed for $permission: $result"
            }
            Log.i("ProfilePermissions", "$packageName: $permission granted")
        }
        AppOpsManager.permissionToOp(permission)?.let(operations::add)
    }
    // AOSP's QUERY_ALL_PACKAGES grant does not grant HyperOS's additional installed-app gate.
    // Read the ROM's constant instead of assuming that every OEM shares a numeric operation.
    if ("android.permission.QUERY_ALL_PACKAGES" in requested) {
        // App reflection is hidden-API filtered even for this public OEM field. Resolve it in a
        // separate adb-shell app_process, without changing device-wide hidden-API enforcement.
        val producerApk = context.applicationInfo.sourceDir
        check(producerApk.matches(Regex("/data/app/[A-Za-z0-9_./=+~-]+/base\\.apk")))
        val probe = device.executeShellCommand(
            "env CLASSPATH=$producerApk app_process /system/bin os.kei.baselineprofile.ProfilePermissionProbe",
        )
        if (!probe.lineSequence().any { it.trim() == "installedAppsOp=unsupported" }) {
            val installedAppsOp = Regex("(?m)^installedAppsOp=(\\d+)$").find(probe)?.groupValues?.get(1)
                ?: error("Could not resolve the device's installed-app AppOp: $probe")
            operations += installedAppsOp
        }
    }
    operations.forEach { operation ->
        val result = device.executeShellCommand("cmd appops set --user 0 $packageName $operation allow")
        val state = device.executeShellCommand("cmd appops get --user 0 $packageName $operation")
        check(Regex("(?m)^\\s*[^:\\n]+: allow(?:;|$)").containsMatchIn(state)) {
            "Profile AppOps preflight failed for $operation: set=$result; readback=$state"
        }
        Log.i("ProfilePermissions", "$packageName: $operation = ${state.trim()}")
    }
}

/** Shell-only reflection probe; it has no mutation capability and is absent from application APKs. */
object ProfilePermissionProbe {
    @JvmStatic
    fun main(args: Array<String>) {
        val hooks = try {
            Class.forName("com.miui.internal.os.MiuiHooks")
        } catch (_: ClassNotFoundException) {
            println("installedAppsOp=unsupported")
            return
        }
        val operation = hooks.getField("OP_GET_INSTALLED_APPS").getInt(null)
        check(operation >= 0)
        println("installedAppsOp=$operation")
    }
}

/** Avoid OEM heads-up notifications during input, while retaining the previous interruption mode. */
internal object ProfileCaptureInterruptions {
    private var previousMode: Int? = null

    fun begin() {
        profileCaptureAppId()
        val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        val previous = device.executeShellCommand("settings get global zen_mode").trim().toInt()
        check(previous in 0..3)
        previousMode = previous
        device.executeShellCommand("cmd notification set_dnd priority")
        waitForMode(device, 1)
        Log.i("ProfilePermissions", "DND priority enabled; previous zen_mode=$previous")
    }

    private fun waitForMode(device: UiDevice, mode: Int) {
        repeat(20) {
            if (device.executeShellCommand("settings get global zen_mode").trim() == mode.toString()) return
            android.os.SystemClock.sleep(100)
        }
        error("Could not establish DND mode $mode")
    }

    fun restore() {
        val previous = previousMode ?: return
        val mode = listOf("all", "priority", "none", "alarms")[previous]
        val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        device.executeShellCommand("cmd notification set_dnd $mode")
        waitForMode(device, previous)
        Log.i("ProfilePermissions", "DND restored to zen_mode=$previous")
        previousMode = null
    }
}
