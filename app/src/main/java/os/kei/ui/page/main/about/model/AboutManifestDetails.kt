package os.kei.ui.page.main.about.model

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.annotation.StringRes
import os.kei.R

private data class AboutExplainRes(
    @get:StringRes val titleRes: Int,
    @get:StringRes val purposeRes: Int,
    @get:StringRes val usedInRes: Int
)

data class AboutPermissionEntry(
    val name: String,
    val title: String,
    val granted: Boolean,
    val purpose: String,
    val usedIn: String
)

enum class AboutComponentType(@get:StringRes val titleRes: Int) {
    Service(R.string.about_component_type_service),
    Receiver(R.string.about_component_type_receiver),
    Provider(R.string.about_component_type_provider)
}

data class AboutComponentExtraEntry(
    @get:StringRes val labelRes: Int,
    val value: String
)

data class AboutComponentEntry(
    val type: AboutComponentType,
    val name: String,
    val exported: Boolean,
    val purpose: String,
    val usedIn: String,
    val extra: List<AboutComponentExtraEntry> = emptyList()
)

private val permissionExplainMap = mapOf(
    "android.permission.QUERY_ALL_PACKAGES" to AboutExplainRes(
        titleRes = R.string.about_permission_query_all_packages_title,
        purposeRes = R.string.about_permission_query_all_packages_purpose,
        usedInRes = R.string.about_permission_query_all_packages_used_in
    ),
    "android.permission.INTERNET" to AboutExplainRes(
        titleRes = R.string.about_permission_internet_title,
        purposeRes = R.string.about_permission_internet_purpose,
        usedInRes = R.string.about_permission_internet_used_in
    ),
    "android.permission.ACCESS_NETWORK_STATE" to AboutExplainRes(
        titleRes = R.string.about_permission_access_network_state_title,
        purposeRes = R.string.about_permission_access_network_state_purpose,
        usedInRes = R.string.about_permission_access_network_state_used_in
    ),
    "android.permission.ACCESS_LOCAL_NETWORK" to AboutExplainRes(
        titleRes = R.string.about_permission_access_local_network_title,
        purposeRes = R.string.about_permission_access_local_network_purpose,
        usedInRes = R.string.about_permission_access_local_network_used_in
    ),
    "android.permission.NEARBY_WIFI_DEVICES" to AboutExplainRes(
        titleRes = R.string.about_permission_nearby_wifi_devices_title,
        purposeRes = R.string.about_permission_nearby_wifi_devices_purpose,
        usedInRes = R.string.about_permission_nearby_wifi_devices_used_in
    ),
    "android.permission.USE_LOOPBACK_INTERFACE" to AboutExplainRes(
        titleRes = R.string.about_permission_use_loopback_interface_title,
        purposeRes = R.string.about_permission_use_loopback_interface_purpose,
        usedInRes = R.string.about_permission_use_loopback_interface_used_in
    ),
    "android.permission.POST_NOTIFICATIONS" to AboutExplainRes(
        titleRes = R.string.about_permission_post_notifications_title,
        purposeRes = R.string.about_permission_post_notifications_purpose,
        usedInRes = R.string.about_permission_post_notifications_used_in
    ),
    "android.permission.POST_PROMOTED_NOTIFICATIONS" to AboutExplainRes(
        titleRes = R.string.about_permission_post_promoted_notifications_title,
        purposeRes = R.string.about_permission_post_promoted_notifications_purpose,
        usedInRes = R.string.about_permission_post_promoted_notifications_used_in
    ),
    "android.permission.FOREGROUND_SERVICE" to AboutExplainRes(
        titleRes = R.string.about_permission_foreground_service_title,
        purposeRes = R.string.about_permission_foreground_service_purpose,
        usedInRes = R.string.about_permission_foreground_service_used_in
    ),
    "android.permission.FOREGROUND_SERVICE_SPECIAL_USE" to AboutExplainRes(
        titleRes = R.string.about_permission_foreground_service_special_use_title,
        purposeRes = R.string.about_permission_foreground_service_special_use_purpose,
        usedInRes = R.string.about_permission_foreground_service_special_use_used_in
    )
)

private val componentExplainMap = mapOf(
    "os.kei.mcp.service.McpKeepAliveService" to AboutExplainRes(
        titleRes = R.string.about_component_mcp_keep_alive_title,
        purposeRes = R.string.about_component_mcp_keep_alive_purpose,
        usedInRes = R.string.about_component_mcp_keep_alive_used_in
    ),
    "os.kei.feature.notification.NotificationActionReceiver" to AboutExplainRes(
        titleRes = R.string.about_component_notification_receiver_title,
        purposeRes = R.string.about_component_notification_receiver_purpose,
        usedInRes = R.string.about_component_notification_receiver_used_in
    ),
    "os.kei.core.background.AppBackgroundTickReceiver" to AboutExplainRes(
        titleRes = R.string.about_component_background_tick_receiver_title,
        purposeRes = R.string.about_component_background_tick_receiver_purpose,
        usedInRes = R.string.about_component_background_tick_receiver_used_in
    ),
    "rikka.shizuku.ShizukuProvider" to AboutExplainRes(
        titleRes = R.string.about_component_shizuku_provider_title,
        purposeRes = R.string.about_component_shizuku_provider_purpose,
        usedInRes = R.string.about_component_shizuku_provider_used_in
    )
)

fun loadPackageDetailInfo(context: Context): PackageInfo? {
    val flags = PackageManager.GET_PERMISSIONS or
        PackageManager.GET_SERVICES or
        PackageManager.GET_RECEIVERS or
        PackageManager.GET_PROVIDERS
    return runCatching {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.packageManager.getPackageInfo(
                context.packageName,
                PackageManager.PackageInfoFlags.of(flags.toLong())
            )
        } else {
            @Suppress("DEPRECATION")
            context.packageManager.getPackageInfo(context.packageName, flags)
        }
    }.getOrNull()
}

fun buildPermissionEntries(
    context: Context,
    packageInfo: PackageInfo?,
    notificationPermissionGranted: Boolean
): List<AboutPermissionEntry> {
    val names = packageInfo?.requestedPermissions?.toList().orEmpty()
    val flags = packageInfo?.requestedPermissionsFlags
    if (names.isEmpty()) return emptyList()
    return names.mapIndexed { index, permissionName ->
        val explain = permissionExplainMap[permissionName]
        val flagGranted = flags?.getOrNull(index)?.let {
            (it and PackageInfo.REQUESTED_PERMISSION_GRANTED) != 0
        } ?: true
        val granted = if (permissionName == android.Manifest.permission.POST_NOTIFICATIONS) {
            notificationPermissionGranted
        } else {
            flagGranted
        }
        AboutPermissionEntry(
            name = permissionName,
            title = explain?.let { context.getString(it.titleRes) }
                ?: permissionName.substringAfterLast('.'),
            granted = granted,
            purpose = explain?.let { context.getString(it.purposeRes) }
                ?: context.getString(R.string.about_permission_fallback_purpose),
            usedIn = explain?.let { context.getString(it.usedInRes) }
                ?: context.getString(R.string.about_permission_fallback_used_in)
        )
    }
}

fun buildComponentEntries(context: Context, packageInfo: PackageInfo?): List<AboutComponentEntry> {
    val services = packageInfo?.services.orEmpty().map { service ->
        val explain = componentExplainMap[service.name]
        AboutComponentEntry(
            type = AboutComponentType.Service,
            name = explain?.let { context.getString(it.titleRes) } ?: service.name.substringAfterLast('.'),
            exported = service.exported,
            purpose = explain?.let { context.getString(it.purposeRes) }
                ?: context.getString(R.string.about_component_fallback_purpose),
            usedIn = explain?.let { context.getString(it.usedInRes) }
                ?: context.getString(R.string.about_component_fallback_used_in),
            extra = listOf(
                AboutComponentExtraEntry(
                    labelRes = R.string.about_component_label_class,
                    value = service.name
                ),
                AboutComponentExtraEntry(
                    labelRes = R.string.about_component_label_fgs_type,
                    value = formatFgsType(context, service)
                )
            )
        )
    }
    val receivers = packageInfo?.receivers.orEmpty().map { receiver ->
        val explain = componentExplainMap[receiver.name]
        AboutComponentEntry(
            type = AboutComponentType.Receiver,
            name = explain?.let { context.getString(it.titleRes) } ?: receiver.name.substringAfterLast('.'),
            exported = receiver.exported,
            purpose = explain?.let { context.getString(it.purposeRes) }
                ?: context.getString(R.string.about_component_fallback_purpose),
            usedIn = explain?.let { context.getString(it.usedInRes) }
                ?: context.getString(R.string.about_component_fallback_used_in),
            extra = listOf(
                AboutComponentExtraEntry(
                    labelRes = R.string.about_component_label_class,
                    value = receiver.name
                )
            )
        )
    }
    val providers = packageInfo?.providers.orEmpty().map { provider ->
        val explain = componentExplainMap[provider.name]
        AboutComponentEntry(
            type = AboutComponentType.Provider,
            name = explain?.let { context.getString(it.titleRes) } ?: provider.name.substringAfterLast('.'),
            exported = provider.exported,
            purpose = explain?.let { context.getString(it.purposeRes) }
                ?: context.getString(R.string.about_component_fallback_purpose),
            usedIn = explain?.let { context.getString(it.usedInRes) }
                ?: context.getString(R.string.about_component_fallback_used_in),
            extra = listOf(
                AboutComponentExtraEntry(
                    labelRes = R.string.about_component_label_class,
                    value = provider.name
                ),
                AboutComponentExtraEntry(
                    labelRes = R.string.about_component_label_authority,
                    value = provider.authority.orEmpty().ifBlank { context.getString(R.string.common_na) }
                )
            )
        )
    }
    return services + receivers + providers
}

private fun formatFgsType(context: Context, serviceInfo: ServiceInfo): String {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return context.getString(R.string.common_na)
    val type = serviceInfo.foregroundServiceType
    if (type == 0) return context.getString(R.string.about_component_fgs_none)
    val labels = buildList {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE &&
            (type and ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE) != 0
        ) {
            add("specialUse")
        }
        if ((type and ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC) != 0) add("dataSync")
        if ((type and ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK) != 0) add("mediaPlayback")
        if ((type and ServiceInfo.FOREGROUND_SERVICE_TYPE_PHONE_CALL) != 0) add("phoneCall")
        if ((type and ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION) != 0) add("location")
        if ((type and ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE) != 0) add("connectedDevice")
        if ((type and ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION) != 0) add("mediaProjection")
        if ((type and ServiceInfo.FOREGROUND_SERVICE_TYPE_CAMERA) != 0) add("camera")
        if ((type and ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE) != 0) add("microphone")
        if ((type and ServiceInfo.FOREGROUND_SERVICE_TYPE_HEALTH) != 0) add("health")
        if ((type and ServiceInfo.FOREGROUND_SERVICE_TYPE_REMOTE_MESSAGING) != 0) add("remoteMessaging")
        if ((type and ServiceInfo.FOREGROUND_SERVICE_TYPE_SYSTEM_EXEMPTED) != 0) add("systemExempted")
    }
    return labels.joinToString(" | ").ifBlank { type.toString() }
}
