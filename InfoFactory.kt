package yuki.star.voyager.platform.voyager

import android.os.Build
import yuki.star.voyager.platform.factory.findJavaPropString
import yuki.star.voyager.platform.factory.findPropString
import yuki.star.voyager.platform.voyager.Terminal.exec

object InfoFactory {
    val procVersion
        get() = exec("cat /proc/version").trim()

    val vendorBuildSecurityPatch
        get() = findPropString("ro.vendor.build.security_patch")

    val abSlot
        get() = findPropString("ro.boot.slot_suffix")

    val toyboxVersion
        get() = exec("toybox --version").trim()

    val systemBuildId
        get() = findPropString("ro.system.build.id")

    val vendorBuildId
        get() = findPropString("ro.vendor.build.id")

    val productBuildId
        get() = findPropString("ro.product.build.id")

    val odmBuildVersion
        get() = findPropString("ro.odm.build.version")

    val systemExtBuildId
        get() = findPropString("ro.system_ext.build.id")

    val systemDlkmBuildId
        get() = findPropString("ro.system_dlkm.build.id")

    val vendorDlkmBuildId
        get() = findPropString("ro.vendor_dlkm.build.id")

    val displayBuildId
        get() = findPropString("ro.build.display.id")

    val descriptionBuildId
        get() = findPropString("ro.build.description")

    val productSdk
        get() = findPropString("ro.product.build.version.sdk")

    val systemSdk
        get() = findPropString("ro.system.build.version.sdk")

    val systemDlkmSdk
        get() = findPropString("ro.system_dlkm.build.version.sdk")

    val systemExtSdk
        get() = findPropString("ro.system_ext.build.version.sdk")

    val minSupportedSdk
        get() = findPropString("ro.build.version.min_supported_target_sdk")

    val vendorSdk
        get() = findPropString("ro.vendor.build.version.sdk")

    val vendorDlkmSdk
        get() = findPropString("ro.vendor_dlkm.build.version.sdk")

    val productRelease
        get() = findPropString("ro.product.build.version.release")

    val systemRelease
        get() = findPropString("ro.system.build.version.release")

    val systemDlkmRelease
        get() = findPropString("ro.system_dlkm.build.version.release")

    val systemExtRelease
        get() = findPropString("ro.system_ext.build.version.release")

    val vendorRelease
        get() = findPropString("ro.vendor.build.version.release")

    val allCodeNames
        get() = findPropString("ro.build.version.all_codenames")

    val productReleaseOrCodename
        get() = findPropString("ro.product.build.version.release_or_codename")

    val systemReleaseOrCodename
        get() = findPropString("ro.system.build.version.release_or_codename")

    val systemDlkmReleaseOrCodename
        get() = findPropString("ro.system_dlkm.build.version.release_or_codename")

    val systemExtReleaseOrCodename
        get() = findPropString("ro.system_ext.build.version.release_or_codename")

    val vendorReleaseOrCodename
        get() = findPropString("ro.vendor.build.version.release_or_codename")

    val vendorDlkmReleaseOrCodename
        get() = findPropString("ro.vendor_dlkm.build.version.release_or_codename")

    val boardFirstApiLevel
        get() = findPropString("ro.board.first_api_level")

    val productFirstApiLevel
        get() = findPropString("ro.product.first_api_level")

    val buildDate
        get() = findPropString("ro.build.date")

    val odmDate
        get() = findPropString("ro.odm.build.date")

    val systemDate
        get() = findPropString("ro.system.build.date")

    val systemDlkmDate
        get() = findPropString("ro.system_dlkm.build.date")

    val systemExtDate
        get() = findPropString("ro.system_ext.build.date")

    val vendorDate
        get() = findPropString("ro.vendor.build.date")

    val vendorDlkmDate
        get() = findPropString("ro.vendor_dlkm.build.date")

    val productDate
        get() = findPropString("ro.product.build.date")

    val modemBuildDate
        get() = findPropString("persist.radio.modem_build_datetime")

    val modemReleaseDate
        get() = findPropString("persist.radio.modem_release_datetime")

    val miOSIncremental
        get() = findPropString("ro.mi.os.version.incremental")

    val miOSVersionName
        get() = findPropString("ro.mi.os.version.name")

    val miuiVersionName
        get() = findPropString("ro.miui.ui.version.name")

    val miuiVersionCode
        get() = findPropString("ro.miui.ui.version.code")

    val productMarketName
        get() = findPropString("ro.product.marketname")

    val productProductManufacturer
        get() = findPropString("ro.product.product.manufacturer")

    val deviceName
        get() = findPropString("persist.sys.device_name")

    val gki
        get() = findPropString("persist.sys.device_config_gki").toBoolean()

    val fuse
        get() = findPropString("persist.sys.fuse").toBoolean()

    val locale
        get() = findPropString("persist.sys.locale")

    val zygote
        get() = findPropString("ro.zygote")

    val miSupportZygote32Lazyload
        get() = findPropString("ro.vendor.mi_support_zygote32_lazyload").toBoolean()

    val unicodeVersion
        get() = findJavaPropString("android.icu.unicode.version")

    val opensslVersion
        get() = findJavaPropString("android.openssl.version")

    val backgroundBlurSupported
        get() = findPropString("persist.sys.background_blur_supported").toBoolean()

    val backgroundVersion
        get() = findPropString("persist.sys.background_blur_version")

    val fboSupported
        get() = findPropString("persist.sys.stability.miui_fbo_enable").toBoolean()

    private val selinux
        get() = exec("cat /sys/fs/selinux/enforce").trim()

    val selinuxBoolean
        get() = selinux == "1"

    val selinuxPolicy: String
        get() {
            return when (selinux) {
                "0" -> "Permissive"
                "1" -> "Enforcing"
                else -> "Unknown"
            }
        }

    val preview
        get() = Build.VERSION.PREVIEW_SDK_INT != 0

    val fileSaf
        get() = exec("pm resolve-activity -a android.intent.action.CREATE_DOCUMENT -c android.intent.category.OPENABLE -t application/zip").trim()

    val fileSafStatus
        get() = fileSaf != "No activity found"
}