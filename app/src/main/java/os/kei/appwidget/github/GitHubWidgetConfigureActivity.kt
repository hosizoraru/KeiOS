package os.kei.appwidget.github

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.content.BroadcastReceiver
import android.content.IntentFilter
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import os.kei.R
import os.kei.core.prefs.AppThemeMode
import os.kei.core.prefs.UiPrefs
import os.kei.core.concurrency.AppDispatchers
import os.kei.ui.page.main.widget.glass.LocalLiquidControlsEnabled
import top.yukonga.miuix.kmp.theme.ColorSchemeMode
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.ThemeController

class GitHubWidgetConfigureActivity : ComponentActivity() {
    private var snapshot by mutableStateOf(GitHubWidgetSnapshot())
    private var showNames by mutableStateOf(false)
    private var loading by mutableStateOf(true)
    private var saving by mutableStateOf(false)
    private var message by mutableStateOf<String?>(null)
    private var widgetId = AppWidgetManager.INVALID_APPWIDGET_ID
    private var isGuide = false
    private var pendingPinToken: String? = null
    private var pinAdded by mutableStateOf(false)
    private var pinRequested by mutableStateOf(false)
    private var pinReceiverRegistered = false
    private val pinReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) = refreshPinStatus()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setResult(RESULT_CANCELED)
        val manager = AppWidgetManager.getInstance(this)
        widgetId = intent.getIntExtra(
            AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID,
        )
        isGuide = widgetId == AppWidgetManager.INVALID_APPWIDGET_ID &&
            intent.action != AppWidgetManager.ACTION_APPWIDGET_CONFIGURE
        pendingPinToken = savedInstanceState?.getString("pinToken")
        pinRequested = pendingPinToken != null
        if (!isGuide && (widgetId <= 0 || manager.getAppWidgetInfo(widgetId)?.provider !=
                GitHubWidgetUpdater.provider(this))) {
            finish()
            return
        }
        enableEdgeToEdge()
        setContent {
            val mode = remember {
                when (UiPrefs.loadSnapshot().appThemeMode) {
                    AppThemeMode.FOLLOW_SYSTEM -> ColorSchemeMode.System
                    AppThemeMode.LIGHT -> ColorSchemeMode.Light
                    AppThemeMode.DARK -> ColorSchemeMode.Dark
                }
            }
            MiuixTheme(controller = remember(mode) { ThemeController(mode) }) {
                CompositionLocalProvider(LocalLiquidControlsEnabled provides false) {
                    BackHandler(enabled = saving) { }
                    GitHubWidgetConfigurationScreen(
                        snapshot = snapshot,
                        showProjectNames = showNames,
                        isGuide = isGuide,
                        ready = !loading,
                        saving = saving,
                        pinSupported = runCatching { manager.isRequestPinAppWidgetSupported }.getOrDefault(false),
                        pinRequested = pinRequested,
                        pinAdded = pinAdded,
                        message = message,
                        onShowProjectNamesChange = { showNames = it },
                        onConfirm = { if (isGuide) requestPin() else saveConfiguration() },
                        onOpenHome = {
                            runCatching {
                                startActivity(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME))
                            }
                        },
                        onClose = { if (!saving) finish() },
                    )
                }
            }
        }
        lifecycleScope.launch {
            try {
                val loaded = withContext(AppDispatchers.fileIo) {
                    GitHubWidgetPublisher.publish(applicationContext) to
                        GitHubWidgetStateStore(applicationContext).configuration(widgetId)
                }
                snapshot = loaded.first
                showNames = savedInstanceState?.getBoolean("showNames") ?: loaded.second.showProjectNames
            } catch (error: Exception) {
                if (error is CancellationException) throw error
                message = getString(R.string.github_widget_load_failed)
            } finally {
                loading = false
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putBoolean("showNames", showNames)
        outState.putString("pinToken", pendingPinToken)
        super.onSaveInstanceState(outState)
    }

    override fun onStart() {
        super.onStart()
        if (isGuide && !isFinishing) {
            registerReceiver(
                pinReceiver, IntentFilter(GitHubWidgetPinning.confirmationAction(this)),
                Context.RECEIVER_NOT_EXPORTED,
            )
            pinReceiverRegistered = true
            refreshPinStatus()
        }
    }

    override fun onStop() {
        if (pinReceiverRegistered) {
            unregisterReceiver(pinReceiver)
            pinReceiverRegistered = false
        }
        super.onStop()
    }

    private fun refreshPinStatus() {
        val token = pendingPinToken ?: return
        lifecycleScope.launch {
            try {
                val confirmed = withContext(AppDispatchers.fileIo) {
                    val id = GitHubWidgetStateStore(applicationContext).pinResult(token)
                    id != null && id in GitHubWidgetUpdater.ids(applicationContext)
                }
                if (token == pendingPinToken) {
                    pinAdded = confirmed
                    if (confirmed) message = getString(R.string.github_widget_pin_added)
                }
            } catch (error: Exception) {
                if (error is CancellationException) throw error
            }
        }
    }

    private fun saveConfiguration() {
        if (loading || saving) return
        saving = true
        message = null
        lifecycleScope.launch {
            try {
                withContext(AppDispatchers.fileIo) {
                    GitHubWidgetStateStore(applicationContext).configure(
                        widgetId, GitHubWidgetConfiguration(showProjectNames = showNames),
                    )
                    GitHubWidgetUpdater.update(applicationContext, intArrayOf(widgetId))
                }
                setResult(
                    RESULT_OK,
                    Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId),
                )
                finish()
            } catch (error: Exception) {
                if (error is CancellationException) throw error
                message = getString(R.string.github_widget_save_failed)
            } finally {
                saving = false
            }
        }
    }

    private fun requestPin() {
        if (loading || saving) return
        saving = true
        message = null
        pinAdded = false
        lifecycleScope.launch {
            try {
                pendingPinToken = GitHubWidgetPinning.request(
                    this@GitHubWidgetConfigureActivity, snapshot, GitHubWidgetConfiguration(showNames),
                )
                pinRequested = pendingPinToken != null
                message = getString(
                    if (pinRequested) R.string.github_widget_pin_requested else R.string.github_widget_pin_unavailable,
                )
                refreshPinStatus()
            } catch (error: Exception) {
                if (error is CancellationException) throw error
                message = getString(R.string.github_widget_pin_unavailable)
            } finally {
                saving = false
            }
        }
    }

    companion object {
        fun launch(context: Context) {
            context.startActivity(
                Intent(context, GitHubWidgetConfigureActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
        }
    }
}
