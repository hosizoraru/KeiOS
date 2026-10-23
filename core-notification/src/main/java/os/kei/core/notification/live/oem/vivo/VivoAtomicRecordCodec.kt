package os.kei.core.notification.live.oem.vivo

import org.json.JSONObject

/** Persistence schema, separate from notification extras and VPush JSON. */
object VivoAtomicRecordCodec {
    fun encode(record: VivoAtomicRecord): String = JSONObject().apply {
        put("version", 1)
        put("activity", record.activityId)
        put("notification", record.notificationId)
        put("business", record.business)
        put("boot", record.bootId)
        record.scene?.let { put("scene", it) }
        put("phase", record.phase.name)
        put("created", record.createdAtMs)
        put("published", record.lastPublishedAtMs)
        put("sequence", record.sequence)
        put("node", record.newNode)
    }.toString()

    fun decode(raw: String): VivoAtomicRecord? = runCatching {
        val json = JSONObject(raw)
        require(json.getInt("version") == 1)
        val phase = VivoAtomicPhase.valueOf(json.getString("phase"))
        val scene = json.optString("scene").takeIf(String::isNotBlank)
        val activity = json.getString("activity")
        val business = json.getString("business")
        val boot = json.getString("boot")
        val created = json.getLong("created")
        val published = json.getLong("published")
        val sequence = json.getInt("sequence")
        val node = json.getInt("node")
        require(activity.isNotBlank() && business.isNotBlank() && boot.isNotBlank())
        require(created >= 0 && published >= created && sequence >= 0 && node >= 0)
        require(phase != VivoAtomicPhase.ATOMIC || scene != null)
        VivoAtomicRecord(
            activity, json.getInt("notification"), business, boot, scene, phase,
            created, published, sequence, node,
        )
    }.getOrNull()
}
