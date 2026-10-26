package os.kei.ui.page.main.os.store

import os.kei.core.prefs.KeiMmkv

internal interface OsCardKeyValueStore {
    fun decodeString(key: String, default: String? = null): String?
    fun decodeBool(key: String, default: Boolean): Boolean
    fun decodeLong(key: String, default: Long): Long
    fun encode(key: String, value: String)
    fun encode(key: String, value: Boolean)
    fun encode(key: String, value: Long)
    fun removeValueForKey(key: String)
}

internal class MmkvOsCardKeyValueStore(private val id: String) : OsCardKeyValueStore {
    private val delegate by lazy { KeiMmkv.byId(id) }
    override fun decodeString(key: String, default: String?) = delegate.decodeString(key, default)
    override fun decodeBool(key: String, default: Boolean) = delegate.decodeBool(key, default)
    override fun decodeLong(key: String, default: Long) = delegate.decodeLong(key, default)
    override fun encode(key: String, value: String) { delegate.encode(key, value) }
    override fun encode(key: String, value: Boolean) { delegate.encode(key, value) }
    override fun encode(key: String, value: Long) { delegate.encode(key, value) }
    override fun removeValueForKey(key: String) { delegate.removeValueForKey(key) }
}
