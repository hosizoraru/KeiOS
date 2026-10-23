package os.kei.core.icon

import android.app.Application
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Rect
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.Drawable
import androidx.test.core.app.ApplicationProvider
import java.io.File
import kotlin.math.abs
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import os.kei.R
import os.kei.core.notification.identity.NotificationAppIconResolver
import os.kei.core.prefs.LauncherIconDesign

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = Application::class, qualifiers = "mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class LauncherIconRenderingTest {
    private val context = ApplicationProvider.getApplicationContext<Application>()
    private val designs =
        listOf(
            "android" to (R.mipmap.ic_launcher_android to R.mipmap.ic_launcher_round_android),
            "apple" to (R.mipmap.ic_launcher_apple to R.mipmap.ic_launcher_round_apple),
        )

    @Test
    fun `public controller exposes one selected alias and matching notification artwork`() {
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER).setPackage(context.packageName)
        listOf(LauncherIconDesign.Apple, LauncherIconDesign.Android).forEach { design ->
            LauncherIconController.applyDesign(context, design).getOrThrow()
            val entries = context.packageManager.queryIntentActivities(intent, 0)
            assertEquals(1, entries.size)
            assertEquals(design.componentSpec(context.packageName).className, entries.single().activityInfo.name)
            val artwork = assertNotNull(NotificationAppIconResolver.largeIconBitmap(context))
            val selectedDrawable = assertNotNull(entries.single().loadIcon(context.packageManager))
            val expected = render(selectedDrawable, artwork.width)
            val output = File("build/reports/launcher-icons").apply { mkdirs() }
            File(output, "notification-${design.storageId}-expected.png").outputStream().use {
                expected.compress(Bitmap.CompressFormat.PNG, 100, it)
            }
            File(output, "notification-${design.storageId}-actual.png").outputStream().use {
                artwork.compress(Bitmap.CompressFormat.PNG, 100, it)
            }
            assertTrue(expected.sameAs(artwork), "Notification app artwork differs from the enabled ${design.storageId} alias")
        }
    }

    @Test
    fun `late update recovery preserves a valid user-selected entry`() {
        LauncherIconController.applyDesign(context, LauncherIconDesign.Apple).getOrThrow()

        assertEquals(false, LauncherIconController.recoverAfterUpdate(context).getOrThrow())

        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER).setPackage(context.packageName)
        val entries = context.packageManager.queryIntentActivities(intent, 0)
        assertEquals(listOf("os.kei.LauncherAppleDesigns"), entries.map { it.activityInfo.name })
    }

    @Test
    fun `reapplying the same design keeps the existing notification bitmap`() {
        LauncherIconController.applyDesign(context, LauncherIconDesign.Android).getOrThrow()
        val before = assertNotNull(NotificationAppIconResolver.largeIconBitmap(context))

        assertEquals(false, LauncherIconController.applyDesign(context, LauncherIconDesign.Android).getOrThrow())
        assertTrue(before === NotificationAppIconResolver.largeIconBitmap(context))
    }

    @Test
    fun `foreground and monochrome scale consistently across launcher icon sizes`() {
        artworkLayers().forEach { (name, drawable) ->
            val small = alphaBounds(render(drawable, 216))
            val large = alphaBounds(render(drawable, 432))
            listOf(small.left to large.left, small.top to large.top, small.right to large.right, small.bottom to large.bottom)
                .forEach { (first, second) ->
                    assertTrue(abs(first * 2 - second) <= 2, "$name changes proportion with size: $small, $large")
                }
        }
    }

    @Test
    fun `essential artwork remains inside the adaptive safe area`() {
        artworkLayers().forEach { (name, drawable) ->
            val image = render(drawable, 432)
            val bounds = alphaBounds(image)
            val safe = Rect(84, 84, 348, 348)
            assertTrue(safe.contains(bounds), "$name exceeds the central 66dp: $bounds")
            if (name.startsWith("production")) {
                assertTrue(bounds.width() >= 191 && bounds.height() >= 191, "$name is smaller than 48dp: $bounds")
            }
            var pixels = 0
            var clipped = 0
            for (y in 0 until image.height) {
                for (x in 0 until image.width) {
                    if (Color.alpha(image.getPixel(x, y)) <= 128) continue
                    pixels++
                    val dx = x + 0.5 - 216
                    val dy = y + 0.5 - 216
                    if (dx * dx + dy * dy > 132 * 132) clipped++
                }
            }
            assertTrue(clipped <= pixels * 0.0025, "$name loses $clipped/$pixels essential pixels under the safe circle")
        }
    }

    private fun artworkLayers(): List<Pair<String, Drawable>> =
        designs.flatMap { (name, resources) ->
            val icon = icon(resources.first)
            listOf("$name color" to icon.foreground, "$name monochrome" to assertNotNull(icon.monochrome))
        } + listOf(
            "production android" to assertNotNull(context.getDrawable(R.drawable.ic_launcher_foreground_inset)),
            "production apple" to assertNotNull(context.getDrawable(R.drawable.ic_launcher_apple_foreground_inset)),
            "production monochrome" to assertNotNull(context.getDrawable(R.drawable.ic_launcher_monochrome_inset)),
        )

    @Test
    fun `round and regular declarations retain matching color and themed artwork`() {
        val output = File("build/reports/launcher-icons").apply { mkdirs() }
        designs.forEach { (name, resources) ->
            val regular = icon(resources.first)
            val round = icon(resources.second)
            assertTrue(render(regular, 192).sameAs(render(round, 192)), "$name round icon differs")
            assertTrue(render(assertNotNull(regular.monochrome), 216).sameAs(render(assertNotNull(round.monochrome), 216)))
            File(output, "$name-color.png").outputStream().use {
                render(regular, 432).compress(Bitmap.CompressFormat.PNG, 100, it)
            }
            val themed =
                AdaptiveIconDrawable(
                    android.graphics.drawable.ColorDrawable(Color.rgb(224, 240, 226)),
                    assertNotNull(regular.monochrome).mutate().apply { setTint(Color.rgb(29, 78, 45)) },
                )
            File(output, "$name-themed.png").outputStream().use {
                render(themed, 432).compress(Bitmap.CompressFormat.PNG, 100, it)
            }
        }
    }

    private fun icon(resource: Int): AdaptiveIconDrawable =
        context.getDrawable(resource).let {
            assertTrue(it is AdaptiveIconDrawable)
            it
        }

    private fun render(drawable: Drawable, size: Int): Bitmap =
        Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888).also {
            drawable.setBounds(0, 0, size, size)
            drawable.draw(Canvas(it))
        }

    private fun alphaBounds(image: Bitmap): Rect {
        val bounds = Rect(image.width, image.height, 0, 0)
        for (y in 0 until image.height) {
            for (x in 0 until image.width) {
                if (Color.alpha(image.getPixel(x, y)) <= 128) continue
                bounds.left = minOf(bounds.left, x)
                bounds.top = minOf(bounds.top, y)
                bounds.right = maxOf(bounds.right, x + 1)
                bounds.bottom = maxOf(bounds.bottom, y + 1)
            }
        }
        assertTrue(bounds.width() > 0 && bounds.height() > 0, "Empty icon layer")
        return bounds
    }
}
