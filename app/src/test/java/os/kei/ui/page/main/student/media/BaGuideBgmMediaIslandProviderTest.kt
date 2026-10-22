@file:androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)

package os.kei.ui.page.main.student

import android.app.Application
import android.app.Notification
import android.app.PendingIntent
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.os.Looper
import androidx.core.app.NotificationCompat
import androidx.core.graphics.drawable.IconCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.SimpleBasePlayer
import androidx.media3.session.CommandButton
import androidx.media3.session.MediaNotification
import androidx.media3.session.MediaSession
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.collect.ImmutableList
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.json.JSONObject
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

@RunWith(AndroidJUnit4::class)
@Config(application = Application::class, sdk = [35])
class BaGuideBgmMediaIslandProviderTest {
    private val context = ApplicationProvider.getApplicationContext<Application>()

    @Test fun `island decoration preserves the platform notification and original cover`() {
        val player = ArtworkPlayer(track("A"))
        val session = MediaSession.Builder(context, player).build()
        try {
            val cover = Bitmap.createBitmap(32, 32, Bitmap.Config.ARGB_8888)
            val intent = PendingIntent.getActivity(context, 1, Intent("open"), PendingIntent.FLAG_IMMUTABLE)
            val notification = Notification.Builder(context, "media")
                .setSmallIcon(android.R.drawable.ic_media_play)
                .setContentTitle("A")
                .setContentIntent(intent)
                .setLargeIcon(cover)
                .setStyle(Notification.MediaStyle().setMediaSession(session.platformToken))
                .addAction(Notification.Action.Builder(android.R.drawable.ic_media_pause, "Pause", intent).build())
                .build()
            val delegate = CapturingProvider(MediaNotification(19, notification))
            val result = provider(delegate).createNotification(session, ImmutableList.of(), NoActions, {})

            assertEquals(19, result.notificationId)
            assertEquals(notification.channelId, result.notification.channelId)
            assertEquals(notification.extras.getCharSequence(Notification.EXTRA_TITLE), result.notification.extras.getCharSequence(Notification.EXTRA_TITLE))
            assertEquals(session.platformToken, result.notification.extras.getParcelable(Notification.EXTRA_MEDIA_SESSION))
            assertEquals(intent, result.notification.contentIntent)
            assertSame(notification.getLargeIcon(), result.notification.getLargeIcon())
            assertEquals(notification.actions[0].actionIntent, result.notification.actions[0].actionIntent)
            assertEquals(notification.actions[0].title, result.notification.actions[0].title)
            assertEquals(notification.smallIcon.resId, result.notification.smallIcon.resId)
            assertEquals(notification.flags, result.notification.flags)
            assertNotNull(BaGuideBgmMediaIslandShareCompat.notificationShareParams(result.notification))
            assertFalse(notification.extras.containsKey(BA_GUIDE_BGM_MEDIA_ISLAND_PARAMS_KEY))
            assertSame(delegate.notificationChannelInfo, provider(delegate).notificationChannelInfo)
        } finally {
            session.release()
            player.release()
        }
    }

    @Test fun `late artwork callback retains its track snapshot after track change and session release`() {
        val player = ArtworkPlayer(track("A"))
        val session = MediaSession.Builder(context, player).build()
        val original = MediaNotification(19, Notification.Builder(context, "media").setSmallIcon(android.R.drawable.ic_media_play).build())
        val delegate = CapturingProvider(original)
        var delivered: MediaNotification? = null
        provider(delegate).createNotification(session, ImmutableList.of(), NoActions) { delivered = it }
        player.changeTrack(track("B"))
        session.release()
        player.release()
        delegate.callback!!.onNotificationChanged(original)

        val params = BaGuideBgmMediaIslandShareCompat.notificationShareParams(assertNotNull(delivered).notification).orEmpty()
        val content = JSONObject(params).getJSONObject("param_v2").getJSONObject("param_island")
            .getJSONObject("shareData").getString("shareContent")
        assertTrue(content.contains("https://example.test/A"))
        assertFalse(content.contains("https://example.test/B"))
        assertFalse(original.notification.extras.containsKey(BA_GUIDE_BGM_MEDIA_ISLAND_PARAMS_KEY))
    }

    @Test fun `a track without a share URL clears only the stale media share extension`() {
        val player = ArtworkPlayer(MediaItem.Builder().setMediaId("local-track").build())
        val session = MediaSession.Builder(context, player).build()
        try {
            val notification = Notification.Builder(context, "media").setSmallIcon(android.R.drawable.ic_media_play).build()
            notification.extras.putString(BA_GUIDE_BGM_MEDIA_ISLAND_PARAMS_KEY, "stale")
            notification.extras.putString("other-owner", "untouched")
            val result = provider(CapturingProvider(MediaNotification(19, notification)))
                .createNotification(session, ImmutableList.of(), NoActions, {})
            assertFalse(result.notification.extras.containsKey(BA_GUIDE_BGM_MEDIA_ISLAND_PARAMS_KEY))
            assertEquals("untouched", result.notification.extras.getString("other-owner"))
            assertEquals("stale", notification.extras.getString(BA_GUIDE_BGM_MEDIA_ISLAND_PARAMS_KEY))
        } finally {
            session.release()
            player.release()
        }
    }

    @Test fun `non HyperOS delegate and asynchronous delivery remain untouched`() {
        val player = ArtworkPlayer(track("A"))
        val session = MediaSession.Builder(context, player).build()
        try {
            val original = MediaNotification(19, Notification.Builder(context, "media").setSmallIcon(android.R.drawable.ic_media_play).build())
            val delegate = CapturingProvider(original)
            var delivered: MediaNotification? = null
            val callback = MediaNotification.Provider.Callback { delivered = it }
            val result = BaGuideBgmMediaIslandShareNotificationProvider(context, delegate, false)
                .createNotification(session, ImmutableList.of(), NoActions, callback)
            assertSame(original, result)
            assertSame(callback, delegate.callback)
            delegate.callback!!.onNotificationChanged(original)
            assertSame(original, delivered)
        } finally {
            session.release()
            player.release()
        }
    }

    private fun provider(delegate: MediaNotification.Provider) =
        BaGuideBgmMediaIslandShareNotificationProvider(context, delegate, true)

    private fun track(id: String): MediaItem = MediaItem.Builder()
        .setMediaId("https://example.test/$id")
        .setMediaMetadata(MediaMetadata.Builder().setTitle(id).build())
        .build()

    private class ArtworkPlayer(private var item: MediaItem) : SimpleBasePlayer(Looper.getMainLooper()) {
        fun changeTrack(value: MediaItem) { item = value; invalidateState() }
        override fun getState(): State = State.Builder()
            .setAvailableCommands(Player.Commands.Builder().addAll(Player.COMMAND_GET_CURRENT_MEDIA_ITEM, Player.COMMAND_GET_TIMELINE, Player.COMMAND_GET_METADATA).build())
            .setPlaylist(listOf(MediaItemData.Builder(item.mediaId).setMediaItem(item).setMediaMetadata(item.mediaMetadata).setDurationUs(30_000_000).build()))
            .setPlaybackState(Player.STATE_READY)
            .build()
    }

    private class CapturingProvider(private val value: MediaNotification) : MediaNotification.Provider {
        var callback: MediaNotification.Provider.Callback? = null
        private val channel = MediaNotification.Provider.NotificationChannelInfo("media", "Media")
        override fun createNotification(session: MediaSession, preferences: ImmutableList<CommandButton>, actions: MediaNotification.ActionFactory, callback: MediaNotification.Provider.Callback): MediaNotification {
            this.callback = callback
            return value
        }
        override fun handleCustomCommand(session: MediaSession, action: String, extras: Bundle) = false
        override fun getNotificationChannelInfo() = channel
    }

    private object NoActions : MediaNotification.ActionFactory {
        override fun createMediaAction(session: MediaSession, icon: IconCompat, title: CharSequence, command: Int): NotificationCompat.Action = error("Unexpected action")
        override fun createCustomAction(session: MediaSession, icon: IconCompat, title: CharSequence, action: String, extras: Bundle): NotificationCompat.Action = error("Unexpected action")
        override fun createCustomActionFromCustomCommandButton(session: MediaSession, button: CommandButton): NotificationCompat.Action = error("Unexpected action")
        override fun createMediaActionPendingIntent(session: MediaSession, command: Int): PendingIntent = error("Unexpected action")
    }
}
