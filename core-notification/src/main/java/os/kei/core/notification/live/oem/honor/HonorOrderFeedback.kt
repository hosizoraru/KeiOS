package os.kei.core.notification.live.oem.honor

import org.json.JSONObject

/**
 * SDK-free order feedback, not the cloud GlobalReachInfo envelope.
 * Contract: https://developer.honor.com/cn/doc/guides/101454 (read 2026-10-23).
 * The caller supplies its assigned intentType and keeps orderNo stable across the lifecycle.
 */
data class HonorOrderFeedback(
    val intentType: String,
    val packageName: String,
    val orderNo: String,
    val status: Status,
    val startTimeMs: Long,
    val createTimeMs: Long,
    val presentation: HonorOrderPresentation,
    val endTimeMs: Long? = null,
) {
    enum class Status(val code: Int) {
        Show(1),
        Update(2),
        DismissUsingLatestEndTime(3),
        DismissUsingFirstEndTime(4),
    }

    // A missing endTime dismisses immediately. Status 3 uses the last received request's
    // deadline, and status 4 the first received request's deadline, not min/max timestamps.

    init {
        require(intentType.isNotBlank())
        require(packageName.isNotBlank())
        require(orderNo.isNotBlank())
        require(startTimeMs > 0 && createTimeMs > 0)
        require(endTimeMs == null || endTimeMs > 0)
    }

    fun toJson(): JSONObject =
        JSONObject().apply {
            put("intentType", intentType)
            put("packageName", packageName)
            put("orderNo", orderNo)
            put("statusCode", status.code)
            // Unlike cloud item timestamps, OrderFeedbackReq timestamps are strings.
            put("startTime", startTimeMs.toString())
            put("createTime", createTimeMs.toString())
            endTimeMs?.let { put("endTime", it.toString()) }
            put("orderDetail", presentation.toJson(packageName))
        }
}

/** A bounded subset of the published order schema: normal and progress layouts. */
data class HonorOrderPresentation(
    val businessType: String,
    val title: String,
    val content: String,
    val buttonText: String,
    val iconUrl: String,
    val deepLink: String,
    val capsuleBusinessName: String,
    val capsuleContent: String,
    val capsuleIconUrl: String,
    val templateColor: String,
    val progress: Int? = null,
) {
    init {
        require(businessType.isNotBlank() && title.isNotBlank() && content.isNotBlank())
        require(buttonText.isNotBlank() && deepLink.isNotBlank())
        require(iconUrl.isNotBlank() && capsuleIconUrl.isNotBlank())
        require(capsuleBusinessName.isNotBlank() && capsuleContent.isNotBlank())
        require(templateColor in setOf("0", "1", "2", "3", "4", "6", "7", "8", "9", "10"))
        require(progress == null || progress in 0..100)
    }

    internal fun toJson(packageName: String): JSONObject =
        JSONObject().apply {
            put("version", "1.0")
            put("businessType", businessType)
            put("layout", if (progress == null) "event_normal" else "event_progress")
            progress?.let { put("progress", it) }
            // The client schema uses objects here; cloud payloads use JSON strings.
            put(
                "cardText",
                JSONObject().apply {
                    put("iconUrl", iconUrl)
                    put("title", title)
                    put("content", content)
                    put("buttonText", buttonText)
                },
            )
            put(
                "notificationText",
                JSONObject().apply {
                    put("title", title)
                    put("content", content)
                    put("buttonText", buttonText)
                },
            )
            put(
                "capsuleText",
                JSONObject().apply {
                    put("businessName", capsuleBusinessName)
                    put("content", capsuleContent)
                    put("capsuleIconUrl", capsuleIconUrl)
                },
            )
            // Follow the OrderDetail field table and SDK example, not the conflicting object example.
            put("color", JSONObject().put("templateColor", templateColor).toString())
            put(
                "homePage",
                JSONObject().apply {
                    put("packageName", packageName)
                    put("url", deepLink)
                },
            )
        }
}
