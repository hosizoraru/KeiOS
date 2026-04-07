package com.example.keios.ui.utils

import java.io.BufferedReader
import java.io.InputStreamReader

private inline fun <T> safeOf(default: T, block: () -> T): T = runCatching(block).getOrDefault(default)

/**
 * 获取系统 Prop 值
 * @param key Key
 * @param default 默认值
 * @return [String]
 */
fun findPropString(key: String, default: String = ""): String = safeOf(default) {
    val clazz = Class.forName("android.os.SystemProperties")
    val method = clazz.getMethod("get", String::class.java, String::class.java)
    method.invoke(null, key, default) as? String ?: default
}

fun findJavaPropString(key: String, default: String = ""): String = safeOf(default) {
    System.getProperties().getProperty(key) ?: default
}

val getAllJavaPropString: Map<String, String>
    get() = safeOf(emptyMap()) {
        val props = System.getProperties()
        props.stringPropertyNames().associateWith { props.getProperty(it) ?: "" }
    }

val getAllSystemProperties: Map<String, String>
    get() = safeOf(emptyMap()) {
        val process = Runtime.getRuntime().exec("getprop")
        process.inputStream.use { stream ->
            BufferedReader(InputStreamReader(stream)).use { reader ->
                buildMap {
                    val pattern = Regex("^\\[(.+)]\\:\\s\\[(.*)]$")
                    reader.forEachLine { line ->
                        val match = pattern.find(line) ?: return@forEachLine
                        put(match.groupValues[1], match.groupValues[2])
                    }
                }
            }
        }
    }

