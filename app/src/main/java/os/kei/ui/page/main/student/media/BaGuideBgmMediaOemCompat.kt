package os.kei.ui.page.main.student

import androidx.annotation.DrawableRes
import os.kei.R
import os.kei.core.system.findPropString

internal val BA_GUIDE_BGM_MEDIA_SMALL_ICON_RES: Int = R.drawable.ic_launcher_monochrome

internal object BaGuideBgmMediaOemCompat {
    @DrawableRes
    fun mediaSmallIconRes(): Int = BA_GUIDE_BGM_MEDIA_SMALL_ICON_RES

    fun mediaIslandDragShareSupported(): Boolean {
        return mediaIslandDragShareSupported(
            versionName = findPropString("ro.mi.os.version.name"),
            versionIncremental = findPropString("ro.mi.os.version.incremental")
        )
    }

    internal fun mediaIslandDragShareSupported(
        versionName: String,
        versionIncremental: String = ""
    ): Boolean {
        return sequenceOf(versionName, versionIncremental)
            .mapNotNull { version -> hyperOsVersion.find(version.trim())?.groupValues?.get(1)?.toIntOrNull() }
            .any { major -> major >= 3 }
    }

    private val hyperOsVersion = Regex("^OS(\\d+)(?:\\.|$)", RegexOption.IGNORE_CASE)
}
