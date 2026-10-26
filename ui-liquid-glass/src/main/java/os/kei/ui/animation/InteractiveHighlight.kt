package os.kei.ui.animation

import android.annotation.SuppressLint
import android.graphics.RuntimeShader
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.spring
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.util.fastCoerceIn
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.android.awaitFrame
import kotlinx.coroutines.launch
import org.intellij.lang.annotations.Language
import os.kei.core.ui.gesture.inspectDragGestures

@SuppressLint("NewApi")
class InteractiveHighlight(
    val animationScope: CoroutineScope,
    val position: (size: Size, offset: Offset) -> Offset = { _, offset -> offset },
    val highlightColor: Color = Color.White,
    val highlightStrength: Float = 1f,
    val highlightRadiusScale: Float = 1.2f,
    val consumeDragChanges: Boolean = false,
    val animationsEnabled: Boolean = true,
) {
    private val pressProgressAnimationSpec = spring(0.5f, 300f, 0.001f)
    private val positionAnimationSpec = spring(0.5f, 300f, Offset.VisibilityThreshold)

    private val pressProgressAnimation = Animatable(0f, 0.001f)
    private val positionAnimation = Animatable(Offset.Zero, Offset.VectorConverter, Offset.VisibilityThreshold)

    private var startPosition = Offset.Zero
    private var pendingDragPosition: Offset? = null
    private var dragPositionSyncJob: Job? = null
    private var pressJob: Job? = null
    private var releaseJob: Job? = null
    private var interactionGeneration = 0L
    val pressProgress: Float get() = pressProgressAnimation.value
    val deformationProgress: Float get() = if (animationsEnabled) pressProgressAnimation.value else 0f
    val offset: Offset get() = if (animationsEnabled) positionAnimation.value - startPosition else Offset.Zero

    /**
     * Current touch position in the composable's coordinate system.
     * Useful for positioning shader effects (e.g., radialRefraction).
     */
    val touchPosition: Offset get() = positionAnimation.value

    @Language("AGSL")
    private val shader =
        RuntimeShader(
            """
    uniform float2 size;
    layout(color) uniform half4 color;
    uniform float radius;
    uniform float2 position;

    half4 main(float2 coord) {
        float dist = distance(coord, position);
        float intensity = smoothstep(radius, radius * 0.5, dist);
        return color * intensity;
    }""",
        )

    // The shader object is stable and its uniforms are mutated in place each
    // frame, so the ShaderBrush wrapper (fixed-shader, size-independent) can be
    // allocated once instead of re-wrapped on every draw frame.
    private val shaderBrush = ShaderBrush(shader)

    val modifier: Modifier =
        Modifier.drawWithContent {
            val progress = pressProgressAnimation.value
            if (progress > 0f) {
                val strength = highlightStrength.fastCoerceIn(0.1f, 3f)
                drawRect(
                    highlightColor.copy((0.06f * progress * strength).fastCoerceIn(0f, 0.45f)),
                    blendMode = BlendMode.Plus,
                )
                shader.apply {
                    val point = position(size, positionAnimation.value)
                    val radiusScale = highlightRadiusScale.fastCoerceIn(0.4f, 1.8f)
                    setFloatUniform("size", size.width, size.height)
                    setColorUniform("color", highlightColor.copy((0.12f * progress * strength).fastCoerceIn(0f, 0.55f)).toArgb())
                    setFloatUniform("radius", size.minDimension * radiusScale)
                    setFloatUniform(
                        "position",
                        point.x.fastCoerceIn(0f, size.width),
                        point.y.fastCoerceIn(0f, size.height),
                    )
                }
                drawRect(
                    shaderBrush,
                    blendMode = BlendMode.Plus,
                )
            }
            drawContent()
        }

    val gestureModifier: Modifier =
        Modifier.pointerInput(this@InteractiveHighlight) {
            inspectDragGestures(
                onDragStart = { down ->
                    interactionGeneration++
                    releaseJob?.cancel()
                    pressJob?.cancel()
                    startPosition = down.position
                    pendingDragPosition = down.position
                    dragPositionSyncJob?.cancel()
                    dragPositionSyncJob = null
                    pressJob =
                        animationScope.launch(start = CoroutineStart.UNDISPATCHED) {
                            if (animationsEnabled) {
                                launch { pressProgressAnimation.animateTo(1f, pressProgressAnimationSpec) }
                                launch { positionAnimation.snapTo(startPosition) }
                            } else {
                                pressProgressAnimation.snapTo(1f)
                                positionAnimation.snapTo(startPosition)
                            }
                        }
                },
                onDragEnd = {
                    releaseHighlight()
                },
                onDragCancel = {
                    releaseHighlight()
                },
            ) { change, dragAmount ->
                if (consumeDragChanges && dragAmount != Offset.Zero) {
                    change.consume()
                }
                pendingDragPosition = change.position
                if (!animationsEnabled) {
                    dragPositionSyncJob?.cancel()
                    dragPositionSyncJob = null
                    animationScope.launch(start = CoroutineStart.UNDISPATCHED) {
                        positionAnimation.snapTo(change.position)
                    }
                    pendingDragPosition = null
                    return@inspectDragGestures
                }
                if (dragPositionSyncJob?.isActive != true) {
                    val syncJob =
                        animationScope.launch(start = CoroutineStart.LAZY) {
                            try {
                                while (true) {
                                    awaitFrame()
                                    val nextPosition = pendingDragPosition ?: break
                                    pendingDragPosition = null
                                    positionAnimation.snapTo(nextPosition)
                                }
                            } finally {
                                if (dragPositionSyncJob === coroutineContext[Job]) {
                                    dragPositionSyncJob = null
                                }
                            }
                        }
                    dragPositionSyncJob = syncJob
                    syncJob.start()
                }
            }
        }

    private fun releaseHighlight() {
        val generation = ++interactionGeneration
        pressJob?.cancel()
        releaseJob?.cancel()
        pendingDragPosition = null
        dragPositionSyncJob?.cancel()
        dragPositionSyncJob = null
        releaseJob =
            animationScope.launch(start = CoroutineStart.UNDISPATCHED) {
                if (animationsEnabled) {
                    launch {
                        pressProgressAnimation.animateTo(0f, pressProgressAnimationSpec)
                    }
                    launch { positionAnimation.animateTo(startPosition, positionAnimationSpec) }
                } else if (generation == interactionGeneration) {
                    pressProgressAnimation.snapTo(0f)
                    positionAnimation.snapTo(startPosition)
                }
            }
    }
}
