package os.kei.ui.page.main.os

import com.tencent.mmkv.MMKV

internal object OsUiStateStore {
    private const val KV_ID = "os_ui_state"
    private const val LEGACY_KV_ID = "system_ui_state"

    private const val KEY_OVERVIEW = "expanded_os_overview"
    private const val KEY_TOP_INFO = "expanded_os_top_info"
    private const val KEY_SHELL_RUNNER = "expanded_os_shell_runner"
    private const val KEY_GOOGLE_SYSTEM_SERVICE = "expanded_os_google_system_service"
    private const val KEY_SYSTEM_TABLE = "expanded_os_system_table"
    private const val KEY_SECURE_TABLE = "expanded_os_secure_table"
    private const val KEY_GLOBAL_TABLE = "expanded_os_global_table"
    private const val KEY_ANDROID_PROPS = "expanded_os_android_props"
    private const val KEY_JAVA_PROPS = "expanded_os_java_props"
    private const val KEY_LINUX_ENV = "expanded_os_linux_env"

    private const val LEGACY_KEY_OVERVIEW = "expanded_overview"
    private const val LEGACY_KEY_TOP_INFO = "expanded_top_info"
    private const val LEGACY_KEY_SHELL_RUNNER = "expanded_shell_runner"
    private const val LEGACY_KEY_GOOGLE_SYSTEM_SERVICE = "expanded_google_system_service"
    private const val LEGACY_KEY_SYSTEM_TABLE = "expanded_system_table"
    private const val LEGACY_KEY_SECURE_TABLE = "expanded_secure_table"
    private const val LEGACY_KEY_GLOBAL_TABLE = "expanded_global_table"
    private const val LEGACY_KEY_ANDROID_PROPS = "expanded_android_props"
    private const val LEGACY_KEY_JAVA_PROPS = "expanded_java_props"
    private const val LEGACY_KEY_LINUX_ENV = "expanded_linux_env"
    private val store: MMKV by lazy { MMKV.mmkvWithID(KV_ID) }
    private val legacyStore: MMKV by lazy { MMKV.mmkvWithID(LEGACY_KV_ID) }

    private fun readBool(kv: MMKV, legacyKv: MMKV, key: String, legacyKey: String, defaultValue: Boolean): Boolean {
        if (kv.containsKey(key)) return kv.decodeBool(key, defaultValue)
        return legacyKv.decodeBool(legacyKey, defaultValue)
    }

    private fun readBool(key: String, legacyKey: String, defaultValue: Boolean): Boolean {
        val kv = store
        val legacyKv = legacyStore
        return readBool(kv, legacyKv, key, legacyKey, defaultValue)
    }

    fun loadSnapshot(): OsUiSnapshot {
        val kv = store
        val legacyKv = legacyStore
        return OsUiSnapshot(
            topInfoExpanded = readBool(kv, legacyKv, KEY_TOP_INFO, LEGACY_KEY_TOP_INFO, false),
            shellRunnerExpanded = readBool(
                kv,
                legacyKv,
                KEY_SHELL_RUNNER,
                LEGACY_KEY_SHELL_RUNNER,
                true
            ),
            googleSystemServiceExpanded = readBool(
                kv,
                legacyKv,
                KEY_GOOGLE_SYSTEM_SERVICE,
                LEGACY_KEY_GOOGLE_SYSTEM_SERVICE,
                false
            ),
            systemTableExpanded = readBool(
                kv,
                legacyKv,
                KEY_SYSTEM_TABLE,
                LEGACY_KEY_SYSTEM_TABLE,
                false
            ),
            secureTableExpanded = readBool(
                kv,
                legacyKv,
                KEY_SECURE_TABLE,
                LEGACY_KEY_SECURE_TABLE,
                false
            ),
            globalTableExpanded = readBool(
                kv,
                legacyKv,
                KEY_GLOBAL_TABLE,
                LEGACY_KEY_GLOBAL_TABLE,
                false
            ),
            androidPropsExpanded = readBool(
                kv,
                legacyKv,
                KEY_ANDROID_PROPS,
                LEGACY_KEY_ANDROID_PROPS,
                false
            ),
            javaPropsExpanded = readBool(
                kv,
                legacyKv,
                KEY_JAVA_PROPS,
                LEGACY_KEY_JAVA_PROPS,
                false
            ),
            linuxEnvExpanded = readBool(kv, legacyKv, KEY_LINUX_ENV, LEGACY_KEY_LINUX_ENV, true),
            visibleCards = OsCardVisibilityStore.loadVisibleCards()
        )
    }

    fun topInfoExpanded(defaultValue: Boolean = false): Boolean =
        readBool(KEY_TOP_INFO, LEGACY_KEY_TOP_INFO, defaultValue)

    fun overviewExpanded(defaultValue: Boolean = true): Boolean =
        readBool(KEY_OVERVIEW, LEGACY_KEY_OVERVIEW, defaultValue)

    fun shellRunnerExpanded(defaultValue: Boolean = true): Boolean =
        readBool(KEY_SHELL_RUNNER, LEGACY_KEY_SHELL_RUNNER, defaultValue)

    fun osSystemTableExpanded(defaultValue: Boolean = false): Boolean =
        readBool(KEY_SYSTEM_TABLE, LEGACY_KEY_SYSTEM_TABLE, defaultValue)

    fun googleSystemServiceExpanded(defaultValue: Boolean = false): Boolean =
        readBool(KEY_GOOGLE_SYSTEM_SERVICE, LEGACY_KEY_GOOGLE_SYSTEM_SERVICE, defaultValue)

    fun secureTableExpanded(defaultValue: Boolean = false): Boolean =
        readBool(KEY_SECURE_TABLE, LEGACY_KEY_SECURE_TABLE, defaultValue)

    fun globalTableExpanded(defaultValue: Boolean = false): Boolean =
        readBool(KEY_GLOBAL_TABLE, LEGACY_KEY_GLOBAL_TABLE, defaultValue)

    fun androidPropsExpanded(defaultValue: Boolean = false): Boolean =
        readBool(KEY_ANDROID_PROPS, LEGACY_KEY_ANDROID_PROPS, defaultValue)

    fun javaPropsExpanded(defaultValue: Boolean = false): Boolean =
        readBool(KEY_JAVA_PROPS, LEGACY_KEY_JAVA_PROPS, defaultValue)

    fun linuxEnvExpanded(defaultValue: Boolean = true): Boolean =
        readBool(KEY_LINUX_ENV, LEGACY_KEY_LINUX_ENV, defaultValue)

    fun setTopInfoExpanded(value: Boolean) {
        store.encode(KEY_TOP_INFO, value)
    }

    fun setOverviewExpanded(value: Boolean) {
        store.encode(KEY_OVERVIEW, value)
    }

    fun setShellRunnerExpanded(value: Boolean) {
        store.encode(KEY_SHELL_RUNNER, value)
    }

    fun setOsSystemTableExpanded(value: Boolean) {
        store.encode(KEY_SYSTEM_TABLE, value)
    }

    fun setGoogleSystemServiceExpanded(value: Boolean) {
        store.encode(KEY_GOOGLE_SYSTEM_SERVICE, value)
    }

    fun setSecureTableExpanded(value: Boolean) {
        store.encode(KEY_SECURE_TABLE, value)
    }

    fun setGlobalTableExpanded(value: Boolean) {
        store.encode(KEY_GLOBAL_TABLE, value)
    }

    fun setAndroidPropsExpanded(value: Boolean) {
        store.encode(KEY_ANDROID_PROPS, value)
    }

    fun setJavaPropsExpanded(value: Boolean) {
        store.encode(KEY_JAVA_PROPS, value)
    }

    fun setLinuxEnvExpanded(value: Boolean) {
        store.encode(KEY_LINUX_ENV, value)
    }

    fun saveExpandedStates(snapshot: OsUiSnapshot) {
        store.encode(KEY_TOP_INFO, snapshot.topInfoExpanded)
        store.encode(KEY_SHELL_RUNNER, snapshot.shellRunnerExpanded)
        store.encode(KEY_SYSTEM_TABLE, snapshot.systemTableExpanded)
        store.encode(KEY_SECURE_TABLE, snapshot.secureTableExpanded)
        store.encode(KEY_GLOBAL_TABLE, snapshot.globalTableExpanded)
        store.encode(KEY_ANDROID_PROPS, snapshot.androidPropsExpanded)
        store.encode(KEY_JAVA_PROPS, snapshot.javaPropsExpanded)
        store.encode(KEY_LINUX_ENV, snapshot.linuxEnvExpanded)
    }

    fun storageFootprintBytes(): Long = store.totalSize() + legacyStore.totalSize()

    fun actualDataBytes(): Long = store.actualSize() + legacyStore.actualSize()

    fun configBytesEstimated(): Long {
        val snapshot = loadSnapshot()
        val boolBytes = 8L
        val cardBytes = snapshot.visibleCards.sumOf { it.name.length.toLong() * 2 + 4L }
        return boolBytes * 9 + cardBytes
    }
}
