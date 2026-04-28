package os.kei.core.intent

import android.app.ActivityOptions
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Bundle

object PendingIntentLaunchOptionsCompat {
    fun getUserVisibleActivity(
        context: Context,
        requestCode: Int,
        intent: Intent,
        flags: Int
    ): PendingIntent {
        return PendingIntent.getActivity(
            context,
            requestCode,
            intent,
            flags,
            userVisibleActivityOptions()
        )
    }

    private fun userVisibleActivityOptions(): Bundle {
        val options = ActivityOptions.makeBasic()
        setBackgroundActivityStartModeIfAvailable(
            options = options,
            methodName = "setPendingIntentCreatorBackgroundActivityStartMode",
            mode = ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOW_ALWAYS
        )
        return options.toBundle()
    }

    private fun setBackgroundActivityStartModeIfAvailable(
        options: ActivityOptions,
        methodName: String,
        mode: Int
    ) {
        runCatching {
            ActivityOptions::class.java
                .getMethod(methodName, Int::class.javaPrimitiveType)
                .invoke(options, mode)
        }
    }
}
