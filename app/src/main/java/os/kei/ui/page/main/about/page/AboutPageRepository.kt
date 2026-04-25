package os.kei.ui.page.main.about.page

import android.content.Context
import android.content.pm.PackageInfo
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import os.kei.core.system.ShizukuApiUtils
import os.kei.ui.page.main.about.model.AboutComponentEntry
import os.kei.ui.page.main.about.model.AboutPermissionEntry
import os.kei.ui.page.main.about.model.buildComponentEntries
import os.kei.ui.page.main.about.model.buildPermissionEntries
import os.kei.ui.page.main.about.model.loadPackageDetailInfo

internal data class AboutPageDetailsState(
    val packageDetailInfo: PackageInfo? = null,
    val permissionEntries: List<AboutPermissionEntry> = emptyList(),
    val componentEntries: List<AboutComponentEntry> = emptyList(),
    val shizukuDetailMap: Map<String, String> = emptyMap(),
    val loaded: Boolean = false
)

internal class AboutPageRepository(
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    suspend fun loadDetails(
        context: Context,
        notificationPermissionGranted: Boolean,
        shizukuApiUtils: ShizukuApiUtils
    ): AboutPageDetailsState {
        return withContext(ioDispatcher) {
            val packageDetailInfo = loadPackageDetailInfo(context)
            AboutPageDetailsState(
                packageDetailInfo = packageDetailInfo,
                permissionEntries = buildPermissionEntries(
                    context = context,
                    packageInfo = packageDetailInfo,
                    notificationPermissionGranted = notificationPermissionGranted
                ),
                componentEntries = buildComponentEntries(
                    context = context,
                    packageInfo = packageDetailInfo
                ),
                shizukuDetailMap = shizukuApiUtils.detailedRows().toMap(),
                loaded = true
            )
        }
    }
}
