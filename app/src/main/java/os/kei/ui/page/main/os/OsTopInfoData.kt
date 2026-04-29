package os.kei.ui.page.main.os

import android.content.Context
import androidx.annotation.StringRes
import os.kei.R

internal data class TopInfoTopic(
    val order: Int,
    @param:StringRes val titleRes: Int
)

internal fun topInfoTopicOf(key: String): TopInfoTopic {
    val k = key.lowercase()
    return when {
        k.startsWith("long_press_") -> TopInfoTopic(0, R.string.os_top_info_topic_long_press)
        k.contains("fbo") || k == "key_fbo_data" -> TopInfoTopic(1, R.string.os_top_info_topic_fbo)
        k.contains("dex2oat") || k.contains("dexopt") -> TopInfoTopic(2, R.string.os_top_info_topic_dex_optimization)
        k.startsWith("tango.") || k.contains("tango") -> TopInfoTopic(3, R.string.os_top_info_topic_tango)
        k.contains("aod") -> TopInfoTopic(4, R.string.os_top_info_topic_aod)
        k.contains("zygote") -> TopInfoTopic(5, R.string.os_top_info_topic_zygote)
        k.contains("density") -> TopInfoTopic(6, R.string.os_top_info_topic_display_density)
        k.contains("autofill") || k.contains("credential") -> TopInfoTopic(7, R.string.os_top_info_topic_autofill_credentials)
        k.startsWith("gsm.") || k.contains("gsm") -> TopInfoTopic(8, R.string.os_top_info_topic_cellular_network)
        k.contains("level") -> TopInfoTopic(9, R.string.os_top_info_topic_level)
        k.startsWith("adb_") || k.contains("adb") -> TopInfoTopic(10, R.string.os_top_info_topic_adb_debugging)
        k.startsWith("voice_") || k.contains("assistant") || k.contains("recognition") -> TopInfoTopic(11, R.string.os_top_info_topic_voice_assistant)
        k.startsWith("share_") -> TopInfoTopic(12, R.string.os_top_info_topic_sharing)
        k.contains("bluetooth") || k.contains("bt_") || k.contains("lc3") || k.contains("lea_") -> TopInfoTopic(13, R.string.os_top_info_topic_bluetooth_audio)
        k.contains("usb") -> TopInfoTopic(14, R.string.os_top_info_topic_usb)
        k.contains("vulkan") || k.contains("opengl") || k.contains("egl") || k.contains("graphics") || k.contains("hwui") -> TopInfoTopic(15, R.string.os_top_info_topic_graphics_rendering)
        k.startsWith("miui_") || k.startsWith("ro.miui") || k.startsWith("ro.mi.") || k.contains("xiaomi") -> TopInfoTopic(16, R.string.os_top_info_topic_miui_xiaomi)
        k.contains("version") || k.contains("build") || k.contains("fingerprint") || k.contains("security_patch") -> TopInfoTopic(17, R.string.os_top_info_topic_version_build)
        k.contains("time") || k.contains("timestamp") -> TopInfoTopic(18, R.string.os_top_info_topic_timestamp)
        k.startsWith("java.") || k.startsWith("android.") || k.startsWith("os.") || k.startsWith("user.") -> TopInfoTopic(19, R.string.os_top_info_topic_java_system_properties)
        k.startsWith("env.") || k == "uname-a" || k == "proc.version" || k == "toybox --version" || k == "getenforce" -> TopInfoTopic(20, R.string.os_top_info_topic_linux_environment)
        else -> TopInfoTopic(99, R.string.os_top_info_topic_other)
    }
}

internal fun sortTopInfoRows(rows: List<InfoRow>): List<InfoRow> {
    return rows.sortedWith(
        compareBy<InfoRow>(
            { topInfoTopicOf(it.key).order },
            { detectValueType(it.value).rank },
            { it.key.lowercase() }
        )
    )
}

internal fun groupTopInfoRows(context: Context, rows: List<InfoRow>): List<Pair<String, List<InfoRow>>> {
    val grouped = rows.groupBy { topInfoTopicOf(it.key) }
    return grouped.entries
        .sortedBy { it.key.order }
        .map { entry -> context.getString(entry.key.titleRes) to entry.value }
}

internal fun removeTopInfoRows(section: SectionKind, rows: List<InfoRow>): List<InfoRow> {
    val keySet = when (section) {
        SectionKind.SYSTEM -> TopInfoKeys.system
        SectionKind.SECURE -> TopInfoKeys.secure
        SectionKind.GLOBAL -> TopInfoKeys.global
        SectionKind.ANDROID -> TopInfoKeys.android
        SectionKind.JAVA -> TopInfoKeys.java
        SectionKind.LINUX -> TopInfoKeys.linux
    }
    return rows.filterNot { keySet.contains(it.key) }
}

private fun mapRows(rows: List<InfoRow>): Map<String, String> = rows.associate { it.key to it.value }

internal fun buildTopInfoRows(
    systemRows: List<InfoRow>,
    secureRows: List<InfoRow>,
    globalRows: List<InfoRow>,
    androidRows: List<InfoRow>,
    javaRows: List<InfoRow>,
    linuxRows: List<InfoRow>
): List<InfoRow> {
    val systemMap = mapRows(systemRows)
    val secureMap = mapRows(secureRows)
    val globalMap = mapRows(globalRows)
    val androidMap = mapRows(androidRows)
    val javaMap = mapRows(javaRows)
    val linuxMap = mapRows(linuxRows)

    val rows = mutableListOf<InfoRow>()
    TopInfoKeys.system.forEach { key -> systemMap[key]?.let { rows += InfoRow(key, it) } }
    TopInfoKeys.secure.forEach { key -> secureMap[key]?.let { rows += InfoRow(key, it) } }
    TopInfoKeys.global.forEach { key -> globalMap[key]?.let { rows += InfoRow(key, it) } }
    TopInfoKeys.android.forEach { key -> androidMap[key]?.let { rows += InfoRow(key, it) } }
    TopInfoKeys.java.forEach { key -> javaMap[key]?.let { rows += InfoRow(key, it) } }
    TopInfoKeys.linux.forEach { key -> linuxMap[key]?.let { rows += InfoRow(key, it) } }
    return sortTopInfoRows(cleanRows(rows))
}
