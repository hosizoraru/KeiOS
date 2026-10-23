package os.kei.core.notification.live.oem.oppo

import java.net.URI
import org.json.JSONArray
import org.json.JSONObject

/**
 * Caller-supplied protocol values, not admission credentials. No production IDs or
 * domain milestones can be derived from a notification ID.
 */
data class OppoProgressBinding(
    val intentName: String,
    val serviceIds: Map<String, String>,
    val entityName: String,
)

data class OppoProgressContent(
    val entityId: String,
    val milestoneCode: Int,
    val milestoneText: String,
    val capsuleLeftText: String,
    val capsuleRightText: String,
    val title: String,
    val content: String,
    val clickAction: String,
    val progress: OppoProgressBar? = null,
    val rightImageUrl: String? = null,
)

data class OppoProgressBar(
    val percent: Int,
    val indicatorImageUrl: String,
    val nodeLabels: List<String>,
)

enum class OppoCardAction(val wireValue: Int) {
    CREATE(0),
    UPDATE(1),
    END(2),
}

sealed interface OppoPayloadMapping {
    data class Valid(val json: String) : OppoPayloadMapping
    data class Invalid(val reason: String) : OppoPayloadMapping
}

/**
 * Official documents 13565/13567. Uses the SDK-free intent-sharing schema,
 * not SeedlingCardOptions or Xiaomi notification extras.
 */
object OppoProgressPayloadMapper {
    fun map(
        binding: OppoProgressBinding,
        state: OppoProgressContent,
        action: OppoCardAction,
        identifier: String,
        timestampMillis: Long,
    ): OppoPayloadMapping {
        val error = validate(binding, state, identifier, timestampMillis)
        if (error != null) return OppoPayloadMapping.Invalid(error)
        val entity = JSONObject()
            .put("entityName", binding.entityName)
            .put("entityId", state.entityId)
            .put("milestone", JSONObject()
                .put("code", state.milestoneCode)
                .put("text", state.milestoneText))
            .put("capsule", JSONObject()
                .put("leftText", state.capsuleLeftText)
                .put("rightText", state.capsuleRightText))
            .put("primary", JSONObject()
                .put("title", state.title)
                .put("content", state.content)
                .put("clickAction", state.clickAction)
                .apply { state.rightImageUrl?.let { put("rightImg", it) } })
        state.progress?.let { bar ->
            entity.put("secondaryData", JSONObject()
                .put("type", "PROGRESS")
                .put("progress", bar.percent)
                .put("indicatorImg", bar.indicatorImageUrl)
                .put("style", "inside")
                .put("nodeLabels", JSONArray(bar.nodeLabels)))
        }
        return OppoPayloadMapping.Valid(JSONObject()
            .put("intentName", binding.intentName)
            .put("identifier", identifier)
            .put("timestamp", timestampMillis)
            .put("serviceId", JSONObject(binding.serviceIds))
            .put("intentAction", JSONObject().put("actionStatus", action.wireValue))
            .put("intentEntity", entity)
            .toString())
    }

    private fun validate(
        binding: OppoProgressBinding,
        state: OppoProgressContent,
        identifier: String,
        timestampMillis: Long,
    ): String? = when {
        binding.intentName.isBlank() || binding.entityName.isBlank() -> "missing_binding"
        binding.serviceIds["fluidCloud"].isNullOrBlank() -> "missing_fluid_cloud_service_id"
        binding.serviceIds.any { it.key.isBlank() || it.value.isBlank() } -> "invalid_service_id"
        state.entityId.isBlank() || identifier.isBlank() || timestampMillis <= 0 -> "invalid_identity"
        state.milestoneText.isBlank() -> "missing_milestone"
        state.capsuleLeftText.isBlank() || state.capsuleRightText.isBlank() -> "missing_capsule_text"
        state.title.isBlank() -> "missing_title"
        !isAbsoluteLink(state.clickAction) -> "invalid_click_action"
        state.rightImageUrl != null && !isHttpsImage(state.rightImageUrl) -> "invalid_image"
        state.progress?.let { it.percent !in 0..100 } == true -> "invalid_progress"
        state.progress?.let { it.nodeLabels.size !in 2..5 || it.nodeLabels.any(String::isBlank) } == true ->
            "invalid_progress_nodes"
        state.progress?.let { !isHttpsImage(it.indicatorImageUrl) } == true -> "invalid_indicator_image"
        else -> null
    }

    private fun isAbsoluteLink(value: String): Boolean =
        runCatching {
            val uri = URI(value)
            uri.isAbsolute && !uri.isOpaque && uri.scheme.lowercase() !in setOf("file", "content", "javascript", "data")
        }.getOrDefault(false)

    private fun isHttpsImage(value: String): Boolean =
        runCatching {
            val uri = URI(value)
            uri.scheme.equals("https", ignoreCase = true) &&
                !uri.host.isNullOrBlank() && uri.userInfo == null
        }.getOrDefault(false)
}
