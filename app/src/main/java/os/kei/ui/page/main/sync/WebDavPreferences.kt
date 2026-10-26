package os.kei.ui.page.main.sync

import os.kei.core.prefs.KeiMmkv

internal interface WebDavPreferences {
    fun decodeString(key: String, default: String?): String?
    fun decodeInt(key: String, default: Int): Int
    fun decodeLong(key: String, default: Long): Long
    fun decodeBool(key: String, default: Boolean): Boolean
    fun encode(key: String, value: String)
    fun encode(key: String, value: Int)
    fun encode(key: String, value: Long)
    fun encode(key: String, value: Boolean)
    fun containsKey(key: String): Boolean
    fun removeValueForKey(key: String)
    fun removeValuesForKeys(keys: Array<String>) = keys.forEach(::removeValueForKey)
}

internal class MmkvWebDavPreferences : WebDavPreferences {
    private val delegate get() = KeiMmkv.byId("webdav_sync")
    override fun decodeString(key: String, default: String?) = delegate.decodeString(key, default)
    override fun decodeInt(key: String, default: Int) = delegate.decodeInt(key, default)
    override fun decodeLong(key: String, default: Long) = delegate.decodeLong(key, default)
    override fun decodeBool(key: String, default: Boolean) = delegate.decodeBool(key, default)
    override fun encode(key: String, value: String) { check(delegate.encode(key, value)) { "Could not persist WebDAV settings" } }
    override fun encode(key: String, value: Int) { check(delegate.encode(key, value)) { "Could not persist WebDAV settings" } }
    override fun encode(key: String, value: Long) { check(delegate.encode(key, value)) { "Could not persist WebDAV settings" } }
    override fun encode(key: String, value: Boolean) { check(delegate.encode(key, value)) { "Could not persist WebDAV settings" } }
    override fun containsKey(key: String) = delegate.containsKey(key)
    override fun removeValueForKey(key: String) = delegate.removeValueForKey(key)
    override fun removeValuesForKeys(keys: Array<String>) = delegate.removeValuesForKeys(keys)
}
