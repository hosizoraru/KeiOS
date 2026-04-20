package com.example.keios.core.shortcut

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import com.example.keios.MainActivity

class ShortcutDispatchActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        dispatchShortcutAction(intent)
        finish()
        overridePendingTransition(0, 0)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        dispatchShortcutAction(intent)
        finish()
        overridePendingTransition(0, 0)
    }

    private fun dispatchShortcutAction(intent: Intent?) {
        val shortcutAction = intent?.getStringExtra(MainActivity.EXTRA_SHORTCUT_ACTION)
            ?.trim()
            ?.takeIf { it.isNotBlank() }
            ?: return
        sendBroadcast(
            Intent(this, ShortcutActionReceiver::class.java).apply {
                action = ShortcutActionReceiver.ACTION_EXECUTE_SHORTCUT
                putExtra(MainActivity.EXTRA_SHORTCUT_ACTION, shortcutAction)
            }
        )
    }
}
