package os.kei.ui.page.main.widget.glass

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotSame
import kotlin.test.assertSame

class LiquidOutlineProviderTest {
    @Test
    fun aFontScaleChangeInvalidatesAnOtherwiseIdenticalOutline() {
        var outlines = 0
        val shape = object : Shape {
            override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
                outlines++
                return Outline.Rectangle(Rect(0f, 0f, size.width, 10f * density.fontScale))
            }
        }
        val provider = LiquidOutlineProvider { shape }
        val size = Size(100f, 100f)
        val first = provider.shape.createOutline(size, LayoutDirection.Ltr, Density(2f, 1f))
        assertSame(first, provider.shape.createOutline(size, LayoutDirection.Ltr, Density(2f, 1f)))
        val changed = provider.shape.createOutline(size, LayoutDirection.Ltr, Density(2f, 2f))
        assertNotSame(first, changed)
        assertEquals(20f, (changed as Outline.Rectangle).rect.bottom)
        assertSame(changed, provider.shape.createOutline(size, LayoutDirection.Ltr, Density(2f, 2f)))
        assertEquals(2, outlines)
    }
}
