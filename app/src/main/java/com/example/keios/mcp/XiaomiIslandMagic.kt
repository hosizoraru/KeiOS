package com.example.keios.mcp

import android.app.Notification
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationManagerCompat
import java.lang.reflect.Method
import rikka.shizuku.Shizuku
import rikka.shizuku.ShizukuBinderWrapper

object XiaomiIslandMagic {
    private const val TAG = "XiaomiIslandMagic"
    private const val XMSF_PACKAGE = "com.xiaomi.xmsf"
    private const val FIREWALL_CHAIN_OEM_DENY_3 = 9
    private const val FIREWALL_RULE_DEFAULT = 0
    private const val FIREWALL_RULE_DENY = 2
    private const val MAGIC_HOLD_MS = 120L
    private val magicLock = Any()
    @Volatile
    private var aidlSupportKnown: Boolean? = null

    fun notify(
        context: Context,
        notificationId: Int,
        notification: Notification
    ) {
        val manager = NotificationManagerCompat.from(context)
        val eligibility = getEligibility(context)
        if (!eligibility.eligible) {
            Log.d(TAG, "Xiaomi magic skipped: ${eligibility.reason}")
            manager.notify(notificationId, notification)
            return
        }

        val uid = eligibility.xmsfUid ?: run {
            manager.notify(notificationId, notification)
            return
        }

        Thread {
            synchronized(magicLock) {
                var blocked = false
                try {
                    blocked = setPackageNetworkingBlockedByUid(uid = uid, blocked = true)
                    if (!blocked) {
                        Log.w(TAG, "AIDL firewall block failed; fallback to cmd connectivity.")
                        blocked = runLegacyCommandMagic(enable = false)
                    }
                    manager.notify(notificationId, notification)
                    Thread.sleep(MAGIC_HOLD_MS)
                } catch (t: Throwable) {
                    Log.e(TAG, "Xiaomi magic notify failed", t)
                } finally {
                    runCatching {
                        if (blocked) {
                            if (!setPackageNetworkingBlockedByUid(uid = uid, blocked = false)) {
                                runLegacyCommandMagic(enable = true)
                            }
                        }
                    }.onFailure {
                        Log.e(TAG, "Failed to restore Xiaomi network rule", it)
                    }
                }
            }
        }.start()
    }

    private data class Eligibility(
        val eligible: Boolean,
        val reason: String,
        val xmsfUid: Int? = null
    )

    private fun getEligibility(context: Context): Eligibility {
        val maker = Build.MANUFACTURER.lowercase()
        val brand = Build.BRAND.lowercase()
        val isXiaomi = maker.contains("xiaomi") || brand.contains("xiaomi") || brand.contains("redmi") || brand.contains("poco")
        if (!isXiaomi) return Eligibility(false, "not_xiaomi")
        if (!Shizuku.pingBinder()) return Eligibility(false, "shizuku_binder_unavailable")
        if (Shizuku.checkSelfPermission() != PackageManager.PERMISSION_GRANTED) return Eligibility(false, "shizuku_permission_denied")

        val shizukuUid = runCatching {
            val uidMethod = Shizuku::class.java.methods.firstOrNull {
                it.name == "getUid" && it.parameterTypes.isEmpty()
            } ?: return@runCatching -1
            (uidMethod.invoke(null) as? Int) ?: -1
        }.getOrDefault(-1)
        if (shizukuUid != 2000 && shizukuUid != 0) {
            return Eligibility(false, "unsupported_shizuku_uid_$shizukuUid")
        }

        val xmsfUid = runCatching {
            context.packageManager.getApplicationInfo(XMSF_PACKAGE, 0).uid
        }.getOrNull() ?: return Eligibility(false, "xmsf_uid_not_found")

        return Eligibility(true, "ok", xmsfUid)
    }

    private fun setPackageNetworkingBlockedByUid(uid: Int, blocked: Boolean): Boolean {
        if (aidlSupportKnown == false) return false
        return runCatching {
            val serviceManager = Class.forName("android.os.ServiceManager")
            val getService = serviceManager.getMethod("getService", String::class.java)
            val rawBinder = getService.invoke(null, Context.CONNECTIVITY_SERVICE) as? IBinder ?: return false
            val wrappedBinder = ShizukuBinderWrapper(rawBinder)

            val stubClass = Class.forName("android.net.IConnectivityManager\$Stub")
            val asInterface = stubClass.getMethod("asInterface", IBinder::class.java)
            val connectivity = asInterface.invoke(null, wrappedBinder) ?: return false

            val setChainEnabled = resolveMethod(connectivity, "setFirewallChainEnabled", 2) ?: run {
                aidlSupportKnown = false
                Log.w(TAG, "No method setFirewallChainEnabled(int, boolean) found. " +
                    "Available firewall methods: ${dumpFirewallMethods(connectivity)}")
                return false
            }
            val setUidRule = resolveMethod(connectivity, "setUidFirewallRule", 3) ?: run {
                aidlSupportKnown = false
                Log.w(TAG, "No method setUidFirewallRule(int, int, int) found. " +
                    "Available firewall methods: ${dumpFirewallMethods(connectivity)}")
                return false
            }
            if (blocked) {
                setChainEnabled.invoke(connectivity, FIREWALL_CHAIN_OEM_DENY_3, true)
                setUidRule.invoke(connectivity, FIREWALL_CHAIN_OEM_DENY_3, uid, FIREWALL_RULE_DENY)
                Log.d(TAG, "AIDL firewall blocked xmsf uid=$uid")
            } else {
                setUidRule.invoke(connectivity, FIREWALL_CHAIN_OEM_DENY_3, uid, FIREWALL_RULE_DEFAULT)
                Log.d(TAG, "AIDL firewall restored xmsf uid=$uid")
            }
            aidlSupportKnown = true
            true
        }.getOrElse {
            Log.w(TAG, "AIDL firewall invoke failed", it)
            false
        }
    }

    private fun resolveMethod(target: Any, name: String, paramCount: Int): Method? {
        val methods = (target.javaClass.methods + target.javaClass.declaredMethods).distinctBy {
            "${it.name}#${it.parameterTypes.joinToString(",") { t -> t.name }}"
        }
        return methods.firstOrNull { it.name == name && it.parameterTypes.size == paramCount }?.also {
            it.isAccessible = true
        }
    }

    private fun dumpFirewallMethods(target: Any): String {
        return (target.javaClass.methods + target.javaClass.declaredMethods)
            .filter { it.name.contains("Firewall", ignoreCase = true) }
            .joinToString("; ") { m ->
                "${m.name}(${m.parameterTypes.joinToString(",") { it.simpleName }})"
            }
            .ifBlank { "none" }
    }

    private fun runLegacyCommandMagic(enable: Boolean): Boolean {
        val command = if (enable) {
            "cmd connectivity set-package-networking-enabled true $XMSF_PACKAGE"
        } else {
            "cmd connectivity set-chain3-enabled true; cmd connectivity set-package-networking-enabled false $XMSF_PACKAGE"
        }
        return runShizukuCommand(command)
    }

    private fun runShizukuCommand(command: String): Boolean {
        return runCatching {
            val method = Shizuku::class.java.getDeclaredMethod(
                "newProcess",
                Array<String>::class.java,
                Array<String>::class.java,
                String::class.java
            )
            method.isAccessible = true
            val process = method.invoke(null, arrayOf("sh", "-c", command), null, null) as Process
            // Shizuku remote process may throw "process hasn't exited" when forcing exitValue checks.
            // For short cmd connectivity commands, fire-and-wait briefly is more stable on HyperOS.
            Thread.sleep(160)
            runCatching { process.destroy() }
            Log.d(TAG, "Shizuku command executed: $command")
            true
        }.getOrElse {
            Log.e(TAG, "Shizuku command exception: $command", it)
            false
        }
    }
}
