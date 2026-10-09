package os.kei.ui.page.main.student.model3d

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import os.kei.core.concurrency.AppDispatchers
import os.kei.ui.page.main.student.rendering.GuideViewerRenderingStore
import os.kei.ui.page.main.ba.BaStandaloneActivityTheme
import os.kei.ui.page.main.widget.sheet.SceneBackdropHost
import os.kei.ui.page.main.student.section.gallery.GuideWebMemoryLobbyLoading
import os.kei.ui.page.main.widget.isAppInDarkTheme
import top.yukonga.miuix.kmp.theme.ColorSchemeMode
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.ThemeController

/** Only a bundled catalog identity crosses the intent; callers cannot supply executable pages or arbitrary asset URLs. */
class GuideModel3dActivity : ComponentActivity() {
    private var resource by mutableStateOf<BaModel3dResource?>(null)
    private var customBackground by mutableStateOf<Int?>(null)
    private var appearanceLoaded = false
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val contentId = intent.getLongExtra("content_id", 0)
        lifecycleScope.launch {
            val loaded = withContext(AppDispatchers.fileIo) {
                BaModel3dCatalog.load(applicationContext).forContentId(contentId) to BaModel3dAppearanceStore.loadBackground()
            }
            customBackground = loaded.second
            appearanceLoaded = true
            resource = loaded.first
            if (resource == null) finish()
        }
        setContent {
            BaStandaloneActivityTheme {
              val defaultBackground = model3dDefaultBackground(MiuixTheme.colorScheme.background,
                  MiuixTheme.colorScheme.primary, isAppInDarkTheme())
              val background = model3dBackground(customBackground, defaultBackground)
              val darkContent = model3dUsesDarkContent(background)
              val mediaTheme = remember(darkContent) { ThemeController(if (darkContent) ColorSchemeMode.Light else ColorSchemeMode.Dark) }
              SideEffect {
                  WindowCompat.getInsetsController(window, window.decorView).apply {
                      isAppearanceLightStatusBars = darkContent
                      isAppearanceLightNavigationBars = darkContent
                  }
              }
              // Media chrome follows background contrast; the surrounding application's theme still owns defaults.
              MiuixTheme(controller = mediaTheme) {
                SceneBackdropHost(backgroundColor = background) {
                  Box(Modifier.fillMaxSize().background(background)) {
                    val model = resource
                    if (model == null) GuideWebMemoryLobbyLoading(true, MiuixTheme.colorScheme.onBackground)
                    else GuideModel3dScreen(model, background, customBackground, { customBackground = it },
                        rendering = remember { GuideViewerRenderingStore.preferences.value.model },
                        onBackgroundDismiss = { lifecycleScope.launch(AppDispatchers.fileIo) { BaModel3dAppearanceStore.saveBackground(customBackground) } },
                        onDismiss = ::finish, onControlsVisible = { visible ->
                        WindowCompat.getInsetsController(window, window.decorView).apply {
                            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                            if (visible) show(WindowInsetsCompat.Type.systemBars()) else hide(WindowInsetsCompat.Type.systemBars())
                        }
                    })
                  }
                }
              }
            }
        }
    }
    override fun onStop() {
        // One small write at a lifecycle boundary also covers closing immediately after a color gesture.
        if (appearanceLoaded) BaModel3dAppearanceStore.saveBackground(customBackground)
        super.onStop()
    }
    companion object {
        internal fun launch(context: Context, resource: BaModel3dResource) {
            context.startActivity(Intent(context, GuideModel3dActivity::class.java).putExtra("content_id", resource.contentId))
        }
    }
}
