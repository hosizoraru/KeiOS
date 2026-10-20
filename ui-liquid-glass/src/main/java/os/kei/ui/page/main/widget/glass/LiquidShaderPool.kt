@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")

package os.kei.ui.page.main.widget.glass

import android.graphics.RuntimeShader
import androidx.compose.runtime.staticCompositionLocalOf
import com.kyant.backdrop.AndroidRuntimeShader
import com.kyant.backdrop.BackdropEffectScope
import com.kyant.backdrop.BackdropEffectScopeImpl

internal val LocalLiquidShaderPooling = staticCompositionLocalOf { false }

internal fun pooledLiquidEffects(
    enabled: Boolean,
    effects: BackdropEffectScope.() -> Unit,
): BackdropEffectScope.() -> Unit =
    if (enabled) { { withLiquidShaderPool(enabled = true, effects = effects) } } else effects

/**
 * Recycle compiled programs across synchronous effect creation / display-list recording. A borrower
 * exclusively owns its builders until that operation finishes. RenderEffect and recorded Paint
 * commands snapshot native uniforms, so retaining a mounted material does not require its builder.
 * Entries contain only program text and native builders, never a view, context or sampled backdrop.
 */
internal object LiquidShaderPool {
    private const val Capacity = 8
    private data class Entry(val program: String, val shader: RuntimeShader)
    private val idle = ThreadLocal.withInitial { ArrayDeque<Entry>() }

    fun acquire(program: String): RuntimeShader {
        val entries = idle.get()
        val iterator = entries.iterator()
        while (iterator.hasNext()) {
            val entry = iterator.next()
            if (entry.program == program) {
                iterator.remove()
                return entry.shader
            }
        }
        return RuntimeShader(program)
    }

    fun release(program: String, shader: RuntimeShader) {
        val entries = idle.get()
        if (entries.size == Capacity) entries.removeFirst()
        entries.addLast(Entry(program, shader))
    }
}

/** The library creates immutable native effects before returning from the supplied block. */
internal fun BackdropEffectScope.withLiquidShaderPool(
    enabled: Boolean,
    effects: BackdropEffectScope.() -> Unit,
) {
    if (!enabled) {
        effects()
        return
    }
    val borrowed = mutableMapOf<Pair<String, String>, AndroidRuntimeShader>()
    val source = this
    val scope = object : BackdropEffectScopeImpl() {
        override val shape get() = source.shape
        override fun obtainRuntimeShader(key: String, string: String): com.kyant.backdrop.RuntimeShader =
            borrowed.getOrPut(key to string) { AndroidRuntimeShader(LiquidShaderPool.acquire(string)) }
    }
    scope.density = density
    scope.fontScale = fontScale
    scope.size = size
    scope.layoutDirection = layoutDirection
    scope.padding = padding
    scope.renderEffect = renderEffect
    try {
        scope.effects()
        padding = scope.padding
        renderEffect = scope.renderEffect
    } finally {
        borrowed.forEach { (key, shader) -> LiquidShaderPool.release(key.second, shader.shader) }
    }
}
