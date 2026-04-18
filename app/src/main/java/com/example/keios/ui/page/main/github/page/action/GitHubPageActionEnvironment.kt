package com.example.keios.ui.page.main

import android.content.Context
import android.widget.Toast
import androidx.annotation.StringRes
import com.example.keios.core.background.AppBackgroundScheduler
import com.example.keios.feature.github.data.local.GitHubTrackStore
import com.example.keios.ui.page.main.github.query.DownloaderOption
import kotlinx.coroutines.CoroutineScope

internal class GitHubPageActionEnvironment(
    val context: Context,
    val scope: CoroutineScope,
    val state: GitHubPageState,
    val systemDmOption: DownloaderOption,
    val openLinkFailureMessage: String
) {
    fun string(@StringRes resId: Int, vararg args: Any): String {
        return context.getString(resId, *args)
    }

    fun toast(@StringRes resId: Int, vararg args: Any) {
        Toast.makeText(context, string(resId, *args), Toast.LENGTH_SHORT).show()
    }

    fun toast(message: String?) {
        if (message.isNullOrBlank()) return
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }

    fun saveTrackedItems() {
        GitHubTrackStore.save(state.trackedItems.toList())
        AppBackgroundScheduler.scheduleGitHubRefresh(context)
    }
}
