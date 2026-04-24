package os.kei.ui.page.main.widget.chrome

internal class ScrollChromeVisibilityController(
    private val hideThresholdPx: Float,
    private val showThresholdPx: Float = hideThresholdPx * 0.6f
) {
    private var hideDistancePx = 0f
    private var showDistancePx = 0f

    fun update(
        deltaY: Float,
        visible: Boolean,
        onVisibleChange: (Boolean) -> Unit
    ) {
        when {
            deltaY < -1f -> {
                showDistancePx = 0f
                if (visible) {
                    hideDistancePx = (hideDistancePx + -deltaY).coerceAtMost(hideThresholdPx)
                    if (hideDistancePx >= hideThresholdPx) {
                        onVisibleChange(false)
                        reset()
                    }
                }
            }

            deltaY > 1f -> {
                hideDistancePx = 0f
                if (!visible) {
                    showDistancePx = (showDistancePx + deltaY).coerceAtMost(showThresholdPx)
                    if (showDistancePx >= showThresholdPx) {
                        onVisibleChange(true)
                        reset()
                    }
                }
            }
        }
    }

    fun showNow(
        visible: Boolean,
        onVisibleChange: (Boolean) -> Unit
    ) {
        reset()
        if (!visible) {
            onVisibleChange(true)
        }
    }

    fun reset() {
        hideDistancePx = 0f
        showDistancePx = 0f
    }
}
