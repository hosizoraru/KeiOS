package os.kei.core.notification.live.oem.vivo

import android.os.Bundle

/** Local Notification extras from doc 896 sections 2/3. This is NOT VPush liveMessage JSON. */
object VivoAtomicPayloadMapper {
    const val CANCEL_TAG = "VIVO_SUPERX_TAG"
    const val PREFIX = "notification.superx."

    fun extras(
        scene: String,
        operation: VivoAtomicOperation,
        content: VivoAtomicContent,
        sequence: Int,
    ): Bundle {
        require(scene.isNotBlank() && sequence >= 0)
        val result = Bundle().apply { putInt("${PREFIX}operation", operation.wireValue) }
        // Doc 896 section 5.4: end without an archive needs only the operation field.
        if (operation == VivoAtomicOperation.END) return result
        val base = Bundle().apply {
            putParcelable("${PREFIX}baseInfos.icon", content.icon)
            putCharSequence("${PREFIX}baseInfos.title", content.title)
            putCharSequence("${PREFIX}baseInfos.content", content.content)
            if (content.progress == null) {
                putInt("${PREFIX}baseInfos.subInfo", 1)
                putString("${PREFIX}baseInfos.subText", content.shortCoreInfo)
            } else {
                putInt("${PREFIX}baseInfos.subInfo", 5)
                putInt("${PREFIX}baseInfos.subProgress", content.progress)
                putInt("${PREFIX}baseInfos.progressState", content.progressState)
            }
        }
        val short = Bundle().apply {
            putParcelable("${PREFIX}shortInfos.icon", content.icon)
            putParcelable("${PREFIX}shortInfos.image", content.icon)
            putParcelable("${PREFIX}shortInfos.imageClickResp", content.openIntent)
            putString("${PREFIX}shortInfos.describeShort", content.shortDescription)
            putString("${PREFIX}shortInfos.coreInfoShort", content.shortCoreInfo)
        }
        val capsule = Bundle().apply {
            putInt("${PREFIX}capsule.state", if (content.landingPageVisible) 0 else 1)
            putParcelable("${PREFIX}capsule.icon", content.icon)
            putString("${PREFIX}capsule.content", content.capsuleText)
            putParcelable("${PREFIX}capsule.clickResp", content.openIntent)
        }
        val left = Bundle().apply {
            putParcelable("island.superx.leftInfo.icon", content.icon)
            putCharSequence("island.superx.leftInfo.content", content.capsuleText)
        }
        val right = Bundle().apply {
            if (content.progress == null) {
                putCharSequence("island.superx.rightInfo.content", content.shortCoreInfo)
            } else {
                putInt("island.superx.rightInfo.progressValue", content.progress)
                putInt("island.superx.rightInfo.progressState", content.progressState)
            }
        }
        val island = Bundle().apply {
            putInt("island.superx.leftTemplate", 1)
            putBundle("island.superx.leftInfo", left)
            putInt("island.superx.rightTemplate", if (content.progress == null) 4 else 2)
            putBundle("island.superx.rightInfo", right)
            putInt("island.superx.template", 4)
            putParcelable("island.superx.clickResp", content.openIntent)
        }
        return result.apply {
            putBoolean("${PREFIX}showNotify", true)
            putInt("${PREFIX}template", 4)
            putString("${PREFIX}scene", scene)
            putParcelable("${PREFIX}clickResp", content.openIntent)
            putBundle("${PREFIX}baseInfos", base)
            putBundle("${PREFIX}infos", Bundle())
            putBundle("${PREFIX}shortInfos", short)
            putBundle("${PREFIX}capsule", capsule)
            putBundle("${PREFIX}island", island)
            putInt("${PREFIX}changedRecord", sequence)
            putInt("${PREFIX}newNode", content.newNode)
            // Hexadecimal nibble masks from the local protocol, NOT 1 shl ordinal.
            putInt("${PREFIX}displays", if (content.landingPageVisible) 0x011 else 0x111)
            putBoolean("${PREFIX}sound", content.sound)
            putBoolean("${PREFIX}dismissWhenKill", content.dismissWhenKill)
        }
    }

    /** Strip only this OEM's protocol when returning to the existing ordinary builder. */
    fun ordinaryExtras(extras: Bundle): Bundle = Bundle(extras).apply {
        keySet().toList().filter { it.startsWith(PREFIX) || it.startsWith("island.superx.") }
            .forEach(::remove)
    }
}
