package os.kei.core.icon

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.content.pm.PackageManager.ComponentEnabledSetting
import os.kei.BuildConfig
import os.kei.core.log.AppLogger
import os.kei.core.notification.identity.NotificationAppIconResolver
import os.kei.core.prefs.LauncherIconDesign
import os.kei.core.prefs.UiPrefs

object LauncherIconController {
    @Synchronized
    fun recoverAfterUpdate(context: Context): Result<Boolean> {
        val appContext = context.applicationContext
        return try {
            val switcher = LauncherIconSwitcher(PackageManagerIconComponents(appContext.packageManager))
            // A late background update must not overwrite a newer, valid user selection.
            if (switcher.hasSingleEnabledEntry(appContext.packageName)) {
                Result.success(false)
            } else {
                applyDesign(appContext, UiPrefs.getLauncherIconDesign())
            }
        } catch (error: Exception) {
            AppLogger.w("LauncherIcon", "Unable to recover launcher entry after update", error)
            Result.failure(error)
        }
    }

    @Synchronized
    fun applyDesign(
        context: Context,
        design: LauncherIconDesign,
    ): Result<Boolean> {
        val appContext = context.applicationContext
        return LauncherIconSwitcher(PackageManagerIconComponents(appContext.packageManager))
            .apply(design.componentStateSpecs(appContext.packageName))
            .onSuccess {
                NotificationAppIconResolver.applyDesign(design)
            }
            .onFailure { error ->
                AppLogger.w("LauncherIcon", "Unable to apply ${design.storageId} launcher icon", error)
            }
    }
}

internal interface LauncherIconComponents {
    fun enabledSetting(component: LauncherIconComponentSpec): Int
    fun manifestEnabled(component: LauncherIconComponentSpec): Boolean
    fun setAtomically(settings: List<LauncherIconComponentStateSpec>)
    fun set(setting: LauncherIconComponentStateSpec)
}

internal class LauncherIconSwitcher(private val components: LauncherIconComponents) {
    fun hasSingleEnabledEntry(packageName: String): Boolean =
        LauncherIconDesign.entries.count { snapshot(it.componentSpec(packageName)).enabled } == 1

    fun apply(settings: List<LauncherIconComponentStateSpec>): Result<Boolean> {
        try {
            val before = settings.associate { it.component to snapshot(it.component) }
            val changes = settings.filter { before.getValue(it.component).enabled != it.isEnabled }
            if (changes.isEmpty()) return Result.success(false)

            try {
                components.setAtomically(changes)
                check(matches(settings)) { "Launcher component batch did not take effect" }
            } catch (batchError: Exception) {
                try {
                    // OEM fallback must establish the new entry before removing any old entry.
                    val selected = settings.single { it.isEnabled }
                    if (!snapshot(selected.component).enabled) components.set(selected)
                    check(snapshot(selected.component).enabled) { "Selected launcher entry is disabled" }
                    settings.filterNot { it.isEnabled }.forEach { inactive ->
                        if (snapshot(inactive.component).enabled) components.set(inactive)
                    }
                    check(matches(settings)) { "Launcher components did not converge" }
                } catch (switchError: Exception) {
                    if (switchError !== batchError) switchError.addSuppressed(batchError)
                    try {
                        restore(before)
                    } catch (restoreError: Exception) {
                        if (restoreError !== switchError) switchError.addSuppressed(restoreError)
                    }
                    return Result.failure(switchError)
                }
            }
            return Result.success(true)
        } catch (error: Exception) {
            return Result.failure(error)
        }
    }

    private fun snapshot(component: LauncherIconComponentSpec): ComponentSnapshot {
        val setting = components.enabledSetting(component)
        val enabled =
            when (setting) {
                PackageManager.COMPONENT_ENABLED_STATE_DEFAULT -> components.manifestEnabled(component)
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED -> true
                else -> false
            }
        return ComponentSnapshot(setting, enabled)
    }

    private fun matches(settings: List<LauncherIconComponentStateSpec>): Boolean =
        settings.all { snapshot(it.component).enabled == it.isEnabled }

    private fun restore(before: Map<LauncherIconComponentSpec, ComponentSnapshot>) {
        val previouslyEnabled = before.filterValues { it.enabled }
        // A pre-existing broken configuration has no entry to restore. Keep any recovered entry.
        if (previouslyEnabled.isEmpty()) return
        previouslyEnabled.forEach { (component, state) ->
            restoreSetting(component, state.setting)
            check(snapshot(component).enabled) { "Previous launcher entry could not be restored" }
        }
        before.filterValues { !it.enabled }.forEach { (component, state) ->
            restoreSetting(component, state.setting)
        }
    }

    private fun restoreSetting(component: LauncherIconComponentSpec, setting: Int) {
        if (components.enabledSetting(component) != setting) {
            components.set(LauncherIconComponentStateSpec(component, setting))
        }
    }

    private data class ComponentSnapshot(val setting: Int, val enabled: Boolean)
}

private val LauncherIconComponentStateSpec.isEnabled: Boolean
    get() = enabledState == PackageManager.COMPONENT_ENABLED_STATE_ENABLED

private class PackageManagerIconComponents(private val packageManager: PackageManager) : LauncherIconComponents {
    override fun enabledSetting(component: LauncherIconComponentSpec): Int =
        packageManager.getComponentEnabledSetting(component.componentName())

    override fun manifestEnabled(component: LauncherIconComponentSpec): Boolean =
        packageManager.getActivityInfo(
            component.componentName(),
            PackageManager.MATCH_DISABLED_COMPONENTS,
        ).enabled

    override fun setAtomically(settings: List<LauncherIconComponentStateSpec>) {
        packageManager.setComponentEnabledSettings(
            settings.map { setting ->
                ComponentEnabledSetting(
                    setting.component.componentName(),
                    setting.enabledState,
                    PackageManager.DONT_KILL_APP,
                )
            },
        )
    }

    override fun set(setting: LauncherIconComponentStateSpec) {
        packageManager.setComponentEnabledSetting(
            setting.component.componentName(),
            setting.enabledState,
            PackageManager.DONT_KILL_APP,
        )
    }
}

internal data class LauncherIconComponentStateSpec(
    val component: LauncherIconComponentSpec,
    val enabledState: Int,
)

internal fun LauncherIconDesign.componentStateSpecs(packageName: String): List<LauncherIconComponentStateSpec> =
    buildList {
        add(
            LauncherIconComponentStateSpec(
                component = componentSpec(packageName),
                enabledState = PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
            ),
        )
        LauncherIconDesign.entries
            .filter { it != this@componentStateSpecs }
            .forEach { inactiveDesign ->
                add(
                    LauncherIconComponentStateSpec(
                        component = inactiveDesign.componentSpec(packageName),
                        enabledState = PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                    ),
                )
            }
    }

internal data class LauncherIconComponentSpec(
    val packageName: String,
    val className: String,
)

internal fun LauncherIconDesign.componentName(packageName: String): ComponentName =
    componentSpec(packageName).let { spec ->
        spec.componentName()
    }

internal fun LauncherIconComponentSpec.componentName(): ComponentName = ComponentName(packageName, className)

internal fun LauncherIconDesign.componentSpec(packageName: String): LauncherIconComponentSpec =
    LauncherIconComponentSpec(
        packageName = packageName,
        className = qualifiedAliasClassName(),
    )

internal fun LauncherIconDesign.qualifiedAliasClassName(): String = "${BuildConfig.MANIFEST_COMPONENT_PACKAGE}.$aliasClassName"
