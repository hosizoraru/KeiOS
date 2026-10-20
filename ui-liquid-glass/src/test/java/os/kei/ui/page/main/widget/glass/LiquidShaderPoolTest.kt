@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")

package os.kei.ui.page.main.widget.glass

import android.app.Application
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.kyant.backdrop.BackdropEffectScopeImpl
import com.kyant.shapes.RoundedRectangle
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import kotlin.test.assertEquals
import kotlin.test.assertNotSame
import kotlin.test.assertSame

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(application = Application::class, sdk = [35])
class LiquidShaderPoolTest {
    @Test
    fun nestedBorrowersNeverShareAMutableBuilder() {
        val program = "layout(color) uniform half4 tint; half4 main(float2 p) { return tint; }"
        val first = LiquidShaderPool.acquire(program)
        val second = LiquidShaderPool.acquire(program)
        try {
            assertNotSame(first, second, "Nested borrowers must own different mutable builders")
        } finally {
            LiquidShaderPool.release(program, second)
            LiquidShaderPool.release(program, first)
        }
    }

    @Test
    fun effectScopePreservesGeometryAndAnExistingEffectChain() {
        val expectedShape = RoundedRectangle(12.dp)
        val source = object : BackdropEffectScopeImpl() {
            override val shape get() = expectedShape
        }
        source.density = 2.75f
        source.fontScale = 1.2f
        source.size = Size(400f, 600f)
        source.layoutDirection = LayoutDirection.Rtl
        source.padding = 7f
        val initial = BlurEffect(3f, 3f)
        val next = BlurEffect(initial, 5f, 5f)
        source.renderEffect = initial
        source.withLiquidShaderPool(enabled = true) {
            assertEquals(2.75f, density)
            assertEquals(1.2f, fontScale)
            assertEquals(Size(400f, 600f), size)
            assertEquals(LayoutDirection.Rtl, layoutDirection)
            assertSame(expectedShape, shape)
            assertSame(initial, renderEffect)
            padding += 13f
            renderEffect = next
        }
        assertEquals(20f, source.padding)
        assertSame(next, source.renderEffect)
    }
}
