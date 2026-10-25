package os.kei.feature.github.install

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageInstaller
import android.content.pm.PackageManager
import android.os.IBinder
import android.os.IInterface
import android.os.Process
import org.lsposed.hiddenapibypass.LSPass
import rikka.shizuku.Shizuku
import rikka.shizuku.ShizukuBinderWrapper
import rikka.shizuku.SystemServiceHelper
import java.lang.reflect.Constructor
import java.lang.reflect.Method
import java.util.concurrent.ConcurrentHashMap

data class ShizukuPackageInstallerCapability(
    val available: Boolean,
    val failureReason: GitHubApkInstallFailureReason? = null,
    val message: String = ""
)

open class ShizukuPackageInstallerBridge {
    private data class CachedPackageInstaller(
        val packageName: String,
        val userId: Int,
        val installer: PackageInstaller
    )

    @Volatile
    private var cachedPackageInstaller: CachedPackageInstaller? = null

    open fun checkCapability(): ShizukuPackageInstallerCapability {
        val capability = runCatching {
            when {
                !Shizuku.pingBinder() -> ShizukuPackageInstallerCapability(
                    available = false,
                    failureReason = GitHubApkInstallFailureReason.ShizukuUnavailable,
                    message = "Shizuku service is not running"
                )

                Shizuku.checkSelfPermission() != PackageManager.PERMISSION_GRANTED ->
                    ShizukuPackageInstallerCapability(
                        available = false,
                        failureReason = GitHubApkInstallFailureReason.ShizukuPermissionMissing,
                        message = "KeiOS has no Shizuku permission"
                    )

                Shizuku.checkRemotePermission(Manifest.permission.INSTALL_PACKAGES) !=
                        PackageManager.PERMISSION_GRANTED ->
                    ShizukuPackageInstallerCapability(
                        available = false,
                        failureReason =
                            GitHubApkInstallFailureReason.RemoteInstallPermissionMissing,
                        message = "Shizuku remote process has no INSTALL_PACKAGES permission"
                    )

                else -> ShizukuPackageInstallerCapability(available = true)
            }
        }.getOrElse { error ->
            ShizukuPackageInstallerCapability(
                available = false,
                failureReason = GitHubApkInstallFailureReason.ShizukuUnavailable,
                message = error.userMessage()
            )
        }
        if (!capability.available) {
            cachedPackageInstaller = null
        }
        return capability
    }

    open fun packageInstaller(context: Context): PackageInstaller {
        val appContext = context.applicationContext
        val packageName = appContext.packageName
        val userId = currentUserId()
        cachedPackageInstaller
            ?.takeIf { it.packageName == packageName && it.userId == userId && Shizuku.pingBinder() }
            ?.let { return it.installer }

        enableHiddenApiAccess()
        val packageBinder = systemServiceBinder("package")
        val packageManager = asInterface(
            stubClassName = "android.content.pm.IPackageManager\$Stub",
            binder = ShizukuBinderWrapper(packageBinder)
        )
        val packageInstaller = invokeNoArg(packageManager, "getPackageInstaller")
        val wrappedPackageInstaller = asInterface(
            stubClassName = "android.content.pm.IPackageInstaller\$Stub",
            binder = ShizukuBinderWrapper(packageInstaller.asBinderCompat())
        )
        return newPackageInstaller(
            packageInstaller = wrappedPackageInstaller,
            installerPackageName = packageName,
            userId = userId
        ).also { installer ->
            cachedPackageInstaller = CachedPackageInstaller(
                packageName = packageName,
                userId = userId,
                installer = installer
            )
        }
    }

    open fun wrapSession(session: PackageInstaller.Session): PackageInstaller.Session {
        enableHiddenApiAccess()
        wrapBinderBackedField(
            target = session,
            fieldName = "mSession",
            stubClassName = "android.content.pm.IPackageInstallerSession\$Stub"
        )
        return session
    }

    open fun applySessionParams(
        params: PackageInstaller.SessionParams,
        context: Context,
        appPackageName: String
    ) {
        params.setInstallReason(PackageManager.INSTALL_REASON_USER)
        params.setApplicationEnabledSettingPersistent()
        params.setOriginatingUid(context.applicationInfo.uid)
        params.setPackageSource(PackageInstaller.PACKAGE_SOURCE_DOWNLOADED_FILE)
        if (appPackageName.isNotBlank()) {
            params.setAppPackageName(appPackageName)
        }
        invokeIfPresent(
            target = params,
            methodName = "setInstallerPackageName",
            parameterTypes = arrayOf(String::class.java),
            args = arrayOf(context.packageName)
        )
        applyInstallFlags(params)
    }

    private fun wrapBinderBackedField(
        target: Any,
        fieldName: String,
        stubClassName: String
    ) {
        val field = target.javaClass.findDeclaredField(fieldName)
        field.isAccessible = true
        val current = field.get(target) ?: return
        val binder = current.asBinderCompat()
        val wrappedBinder = ShizukuBinderWrapper(binder)
        val wrappedInterface = asInterface(stubClassName, wrappedBinder)
        field.set(target, wrappedInterface)
    }

    @SuppressLint("DiscouragedPrivateApi", "PrivateApi")
    private fun systemServiceBinder(name: String): IBinder {
        return SystemServiceHelper.getSystemService(name)
            ?: error("System service '$name' is unavailable")
    }

    private fun asInterface(stubClassName: String, binder: IBinder): IInterface {
        return asInterfaceMethods.getOrPut(stubClassName) {
            Class.forName(stubClassName).getDeclaredMethod("asInterface", IBinder::class.java)
        }
            .invoke(null, binder) as? IInterface
            ?: error("Failed to create interface for $stubClassName")
    }

    private fun invokeNoArg(target: Any, methodName: String): IInterface {
        return noArgMethods.getOrPut("${target.javaClass.name}#$methodName") {
            target.javaClass.methods.firstOrNull { method ->
                method.name == methodName && method.parameterTypes.isEmpty()
            } ?: error("Failed to locate ${target.javaClass.name}.$methodName")
        }
            .invoke(target) as? IInterface
            ?: error("Failed to call ${target.javaClass.name}.$methodName")
    }

    private fun Any.asBinderCompat(): IBinder {
        if (this is IInterface) return asBinder()
        return javaClass.methods
            .firstOrNull { method ->
                method.name == "asBinder" &&
                        method.parameterTypes.isEmpty() &&
                        IBinder::class.java.isAssignableFrom(method.returnType)
            }
            ?.invoke(this) as? IBinder
            ?: error("${javaClass.name} does not expose asBinder()")
    }

    @SuppressLint("PrivateApi")
    private fun newPackageInstaller(
        packageInstaller: IInterface,
        installerPackageName: String,
        userId: Int
    ): PackageInstaller {
        return packageInstallerConstructor.newInstance(
            packageInstaller,
            installerPackageName,
            null,
            userId
        ) as PackageInstaller
    }

    internal fun currentUserId(): Int {
        return Process.myUid() / 100000
    }

    private fun applyInstallFlags(params: PackageInstaller.SessionParams) {
        val field = params.javaClass.findDeclaredField("installFlags")
        field.isAccessible = true
        var flags = field.getInt(params)
        listOf(
            "INSTALL_ALLOW_TEST",
            "INSTALL_REPLACE_EXISTING",
            "INSTALL_REQUEST_DOWNGRADE",
            "INSTALL_FULL_APP",
            "INSTALL_BYPASS_LOW_TARGET_SDK_BLOCK",
            "INSTALL_REQUEST_UPDATE_OWNERSHIP"
        ).forEach { fieldName ->
            flags = flags or packageManagerFlag(fieldName)
        }
        field.setInt(params, flags)
    }

    private fun packageManagerFlag(name: String): Int {
        return runCatching {
            PackageManager::class.java.findDeclaredField(name).let { field ->
                field.isAccessible = true
                field.getInt(null)
            }
        }.getOrDefault(0)
    }

    private fun invokeIfPresent(
        target: Any,
        methodName: String,
        parameterTypes: Array<Class<*>>,
        args: Array<Any>
    ) {
        runCatching {
            target.javaClass.getMethod(methodName, *parameterTypes).invoke(target, *args)
        }
    }

    private fun enableHiddenApiAccess() {
        if (hiddenApiAccessEnabled) return
        synchronized(hiddenApiAccessLock) {
            if (hiddenApiAccessEnabled) return
            runCatching {
                LSPass.setHiddenApiExemptions("")
            }.onSuccess {
                hiddenApiAccessEnabled = true
            }
        }
    }

    private companion object {
        private val asInterfaceMethods = ConcurrentHashMap<String, Method>()
        private val noArgMethods = ConcurrentHashMap<String, Method>()
        private val hiddenApiAccessLock = Any()

        @Volatile
        private var hiddenApiAccessEnabled = false

        private val packageInstallerConstructor: Constructor<PackageInstaller> by lazy {
            val packageInstallerClass = Class.forName("android.content.pm.IPackageInstaller")
            PackageInstaller::class.java.getDeclaredConstructor(
                packageInstallerClass,
                String::class.java,
                String::class.java,
                Int::class.javaPrimitiveType!!
            ).apply {
                isAccessible = true
            }
        }
    }
}

private fun Class<*>.findDeclaredField(name: String): java.lang.reflect.Field {
    var current: Class<*>? = this
    while (current != null) {
        runCatching {
            return current.getDeclaredField(name)
        }
        current = current.superclass
    }
    throw NoSuchFieldException(name)
}

private fun Throwable.userMessage(): String = message?.trim()?.ifBlank { null } ?: javaClass.simpleName
