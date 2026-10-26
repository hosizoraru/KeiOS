package os.kei.ui.animation

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.MutatorMutex
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.systemGestureExclusion
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.unit.IntSize
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.android.awaitFrame
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import os.kei.core.ui.gesture.inspectDragGestures
import kotlin.math.abs

class DampedDragAnimation(
    private val animationScope: CoroutineScope,
    val initialValue: Float,
    val valueRange: ClosedRange<Float>,
    val visibilityThreshold: Float,
    val initialScale: Float,
    val pressedScale: Float,
    val animationsEnabled: Boolean = true,
    val gestureKey: Any? = Unit,
    val canDrag: (Offset) -> Boolean = { true },
    val consumeDragChanges: Boolean = false,
    /**
     * Registers this node's bounds through [androidx.compose.foundation.systemGestureExclusion].
     *
     * No production call site sets it, and a bottom-bar drag starting ~18dp from the screen edge is
     * still taken by the system back gesture on HyperOS (measured on 5eea1f50: Settings popped
     * instead of switching tabs, unchanged whether the exclusion sat on the selection pill or on the
     * bar's whole footprint). HyperOS appears to ignore app exclusion rects for its own back
     * gesture, so treat this as an AOSP-only lever, and remember the platform caps exclusions at
     * 200dp per edge — a `true` default would spend that budget on every damped drag at once.
     */
    val excludeFromSystemGestures: Boolean = false,
    val dragOrientation: Orientation? = null,
    val dragTouchSlop: Float = 0f,
    val onDragStarted: DampedDragAnimation.(position: Offset) -> Unit,
    val onDragStopped: DampedDragAnimation.() -> Unit,
    val onDragCancelled: DampedDragAnimation.() -> Unit = {},
    val onDrag: DampedDragAnimation.(size: IntSize, dragAmount: Offset) -> Unit,
) {
    private val valueAnimationSpec = spring(1f, 1000f, visibilityThreshold)
    private val velocityAnimationSpec = spring(0.5f, 300f, visibilityThreshold * 10f)
    private val pressProgressAnimationSpec = spring(1f, 1000f, 0.001f)
    private val scaleXAnimationSpec = spring(0.6f, 250f, 0.001f)
    private val scaleYAnimationSpec = spring(0.7f, 250f, 0.001f)

    private val valueAnimation = Animatable(initialValue, visibilityThreshold)
    private val velocityAnimation = Animatable(0f, 5f)
    private val pressProgressAnimation = Animatable(0f, 0.001f)
    private val scaleXAnimation = Animatable(initialScale, 0.001f)
    private val scaleYAnimation = Animatable(initialScale, 0.001f)

    private val mutatorMutex = MutatorMutex()
    private val velocityTracker = VelocityTracker()
    private var interactionGeneration = 0L
    private var pressJob: Job? = null
    private var releaseJob: Job? = null

    val value: Float get() = valueAnimation.value
    val progress: Float
        get() {
            val distance = valueRange.endInclusive - valueRange.start
            return if (distance == 0f) 0f else (value - valueRange.start) / distance
        }
    val targetValue: Float get() = valueAnimation.targetValue
    val pressProgress: Float get() = pressProgressAnimation.value
    val deformationProgress: Float get() = if (animationsEnabled) pressProgressAnimation.value else 0f
    val scaleX: Float get() = if (animationsEnabled) scaleXAnimation.value else initialScale
    val scaleY: Float get() = if (animationsEnabled) scaleYAnimation.value else initialScale
    val velocity: Float get() = if (animationsEnabled) velocityAnimation.value else 0f

    val modifier: Modifier =
        Modifier
            .then(if (excludeFromSystemGestures) Modifier.systemGestureExclusion() else Modifier)
            .pointerInput(this@DampedDragAnimation, gestureKey) {
                var accumulatedDrag = Offset.Zero
                var axisAccepted = dragOrientation == null
                var axisRejected = false
                var initialDownPosition = Offset.Zero
                var dragStartedDispatched = false
                inspectDragGestures(
                    onDragStart = { down ->
                        accumulatedDrag = Offset.Zero
                        axisAccepted = dragOrientation == null
                        axisRejected = false
                        initialDownPosition = down.position
                        dragStartedDispatched = false
                        if (axisAccepted) {
                            onDragStarted(initialDownPosition)
                            dragStartedDispatched = true
                        }
                        press()
                    },
                    onDragEnd = {
                        try {
                            if (axisRejected) {
                                onDragCancelled()
                            } else {
                                if (!dragStartedDispatched) {
                                    onDragStarted(initialDownPosition)
                                    dragStartedDispatched = true
                                }
                                onDragStopped()
                            }
                        } finally {
                            release()
                        }
                    },
                    onDragCancel = {
                        try {
                            onDragCancelled()
                        } finally {
                            release()
                        }
                    },
                ) { change, dragAmount ->
                    if (!axisAccepted && !axisRejected && dragOrientation != null) {
                        accumulatedDrag += dragAmount
                        val resolvedTouchSlop =
                            dragTouchSlop.takeIf { it.isFinite() && it >= 0f } ?: 0f
                        if (
                            accumulatedDrag != Offset.Zero &&
                            accumulatedDrag.getDistance() >= resolvedTouchSlop
                        ) {
                            axisAccepted =
                                when (dragOrientation) {
                                    Orientation.Horizontal -> {
                                        abs(accumulatedDrag.x) >= abs(accumulatedDrag.y)
                                    }

                                    Orientation.Vertical -> {
                                        abs(accumulatedDrag.y) >= abs(accumulatedDrag.x)
                                    }
                                }
                            axisRejected = !axisAccepted
                        }
                    }
                    if (axisRejected || !axisAccepted) return@inspectDragGestures
                    if (!dragStartedDispatched) {
                        onDragStarted(initialDownPosition)
                        dragStartedDispatched = true
                    }
                    val position = change.position
                    val previousPosition = change.previousPosition
                    if (canDrag(position) && canDrag(previousPosition)) {
                        onDrag(size, dragAmount)
                        if (consumeDragChanges && dragAmount != Offset.Zero) {
                            change.consume()
                        }
                    }
                }
            }

    fun press() {
        velocityTracker.resetTracking()
        interactionGeneration++
        releaseJob?.cancel()
        pressJob?.cancel()
        pressJob =
            animationScope.launch(start = CoroutineStart.UNDISPATCHED) {
                if (animationsEnabled) {
                    launch { pressProgressAnimation.animateTo(1f, pressProgressAnimationSpec) }
                    launch { scaleXAnimation.animateTo(pressedScale, scaleXAnimationSpec) }
                    launch { scaleYAnimation.animateTo(pressedScale, scaleYAnimationSpec) }
                } else {
                    pressProgressAnimation.snapTo(1f)
                    scaleXAnimation.snapTo(initialScale)
                    scaleYAnimation.snapTo(initialScale)
                    velocityAnimation.snapTo(0f)
                }
            }
    }

    private suspend fun snapReleasedState() {
        pressProgressAnimation.snapTo(0f)
        scaleXAnimation.snapTo(initialScale)
        scaleYAnimation.snapTo(initialScale)
        velocityAnimation.snapTo(0f)
    }

    private fun isCurrentInteraction(generation: Long): Boolean = generation == interactionGeneration

    private suspend fun awaitValueSettled(generation: Long) {
        if (!isCurrentInteraction(generation) || value == targetValue) return
        val rangeSpan = abs(valueRange.endInclusive - valueRange.start)
        val threshold = maxOf(visibilityThreshold, rangeSpan * 0.025f, 1e-6f)
        snapshotFlow { valueAnimation.value to valueAnimation.targetValue }
            .filter { (current, target) ->
                !isCurrentInteraction(generation) || abs(current - target) < threshold
            }.first()
    }

    private suspend fun animateReleasedState(generation: Long) {
        if (!isCurrentInteraction(generation)) return
        coroutineScope {
            launch { pressProgressAnimation.animateTo(0f, pressProgressAnimationSpec) }
            launch { scaleXAnimation.animateTo(initialScale, scaleXAnimationSpec) }
            launch { scaleYAnimation.animateTo(initialScale, scaleYAnimationSpec) }
            launch { velocityAnimation.animateTo(0f, velocityAnimationSpec) }
        }
    }

    private suspend fun settleValueImmediately(targetValue: Float) {
        mutatorMutex.mutate {
            valueAnimation.snapTo(targetValue)
            velocityAnimation.snapTo(0f)
        }
    }

    fun release() {
        if (!animationsEnabled) {
            pressJob?.cancel()
        }
        releaseJob?.cancel()
        val generation = ++interactionGeneration
        releaseJob =
            animationScope.launch(start = CoroutineStart.UNDISPATCHED) {
                if (!animationsEnabled) {
                    snapReleasedState()
                    return@launch
                }
                awaitFrame()
                awaitValueSettled(generation)
                animateReleasedState(generation)
            }
    }

    fun updateValue(value: Float) {
        val targetValue = value.coerceIn(valueRange)
        if (!animationsEnabled) {
            animationScope.launch(start = CoroutineStart.UNDISPATCHED) {
                settleValueImmediately(targetValue)
            }
            return
        }
        animationScope.launch {
            mutatorMutex.mutate {
                valueAnimation.animateTo(targetValue, valueAnimationSpec) { updateVelocity() }
            }
        }
    }

    fun snapToValue(
        value: Float,
        updateVelocity: Boolean = true,
    ) {
        val targetValue = value.coerceIn(valueRange)
        animationScope.launch(start = CoroutineStart.UNDISPATCHED) {
            mutatorMutex.mutate {
                valueAnimation.snapTo(targetValue)
                if (animationsEnabled && updateVelocity) {
                    updateVelocity()
                } else {
                    velocityAnimation.snapTo(0f)
                }
            }
        }
    }

    fun animateToValue(value: Float, pressFeedback: Boolean = true) {
        val targetValue = value.coerceIn(valueRange)
        if (!animationsEnabled) {
            animationScope.launch(start = CoroutineStart.UNDISPATCHED) {
                settleValueImmediately(targetValue)
            }
            return
        }
        animationScope.launch {
            mutatorMutex.mutate {
                if (pressFeedback) press()
                launch { valueAnimation.animateTo(targetValue, valueAnimationSpec) }
                if (velocity != 0f) {
                    launch { velocityAnimation.animateTo(0f, velocityAnimationSpec) }
                }
                if (pressFeedback) release()
            }
        }
    }

    private fun updateVelocity() {
        if (!animationsEnabled) {
            animationScope.launch(start = CoroutineStart.UNDISPATCHED) {
                velocityAnimation.snapTo(0f)
            }
            return
        }
        val rangeSpan = valueRange.endInclusive - valueRange.start
        if (!rangeSpan.isFinite() || abs(rangeSpan) <= 1e-6f) {
            animationScope.launch { velocityAnimation.snapTo(0f) }
            return
        }
        velocityTracker.addPosition(
            System.currentTimeMillis(),
            Offset(value, 0f),
        )
        val rawVelocity = velocityTracker.calculateVelocity().x / rangeSpan
        val targetVelocity = rawVelocity.takeIf(Float::isFinite) ?: 0f
        animationScope.launch { velocityAnimation.animateTo(targetVelocity, velocityAnimationSpec) }
    }
}
