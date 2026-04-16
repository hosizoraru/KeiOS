package com.example.keios.core.system

import android.content.Context
import com.example.keios.BuildConfig
import java.io.File

object AppBuildEnv {
    val buildType: String = BuildConfig.BUILD_TYPE.ifBlank {
        if (BuildConfig.DEBUG) "debug" else "release"
    }

    val isDebugBuild: Boolean = BuildConfig.DEBUG

    val flavorFolderName: String = if (isDebugBuild) "debug" else "release"

    val displayName: String = if (isDebugBuild) "Debug" else "Release"

    fun uiDumpDirectory(context: Context): File {
        val externalFiles = context.getExternalFilesDir(null)
        return if (externalFiles != null) {
            File(externalFiles, "$flavorFolderName/ui")
        } else {
            File(context.filesDir, "ui_dump/$flavorFolderName")
        }
    }

    fun uiDumpShellDirectory(): String {
        return "/sdcard/Android/data/${BuildConfig.APPLICATION_ID}/files/$flavorFolderName/ui"
    }
}
