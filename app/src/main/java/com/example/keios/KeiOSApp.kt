package com.example.keios

import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import coil3.ImageLoader
import coil3.SingletonImageLoader
import coil3.gif.AnimatedImageDecoder
import com.example.keios.core.background.AppBackgroundScheduler
import com.example.keios.core.system.AppPackageChangedEvent
import com.example.keios.core.system.AppPackageChangedEvents
import com.example.keios.feature.github.data.remote.GitHubVersionUtils
import com.tencent.mmkv.MMKV

class KeiOSApp : Application() {
    companion object {
        @Volatile
        private lateinit var instance: KeiOSApp

        val appContext: Application
            get() = instance
    }

    private val packageChangedReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                Intent.ACTION_PACKAGE_ADDED,
                Intent.ACTION_PACKAGE_REMOVED,
                Intent.ACTION_PACKAGE_REPLACED,
                Intent.ACTION_PACKAGE_CHANGED,
                Intent.ACTION_PACKAGE_FULLY_REMOVED -> {
                    val pkg = intent.data?.schemeSpecificPart?.trim().orEmpty()
                    if (pkg.isNotBlank()) {
                        AppPackageChangedEvents.publish(
                            AppPackageChangedEvent(
                                packageName = pkg,
                                action = intent.action.orEmpty()
                            )
                        )
                    }
                    GitHubVersionUtils.invalidateInstalledLaunchableAppsCache()
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        SingletonImageLoader.setSafe { context ->
            ImageLoader.Builder(context)
                .components {
                    add(AnimatedImageDecoder.Factory())
                }
                .build()
        }
        MMKV.initialize(this)
        AppBackgroundScheduler.scheduleAll(this)
        registerPackageChangedReceiver()
    }

    private fun registerPackageChangedReceiver() {
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_PACKAGE_ADDED)
            addAction(Intent.ACTION_PACKAGE_REMOVED)
            addAction(Intent.ACTION_PACKAGE_REPLACED)
            addAction(Intent.ACTION_PACKAGE_CHANGED)
            addAction(Intent.ACTION_PACKAGE_FULLY_REMOVED)
            addDataScheme("package")
        }
        registerReceiver(packageChangedReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
    }
}
