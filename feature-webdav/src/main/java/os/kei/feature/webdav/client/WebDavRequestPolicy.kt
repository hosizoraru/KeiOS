package os.kei.feature.webdav.client

import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import os.kei.feature.webdav.model.WebDavConfig

/** Shared per-account pacing and cooldown, including across manual/background client instances. */
class WebDavRequestPolicy(
    private val intervalMs: Long = 0,
    private val nowMs: () -> Long = System::currentTimeMillis,
    private val wait: suspend (Long) -> Unit = { delay(it) },
) {
    private val mutex = Mutex()
    private var nextRequestMs = 0L
    private var cooldownUntilMs = 0L
    private val recentRequests = ArrayDeque<Long>()
    private val rejectedCredentials = mutableSetOf<String>()

    internal suspend fun acquire(credentialIdentity: String? = null) = mutex.withLock {
        if (credentialIdentity != null && credentialIdentity in rejectedCredentials) {
            throw WebDavRequestException(WebDavError.AuthFailed)
        }
        val now = nowMs()
        if (cooldownUntilMs > now) throw WebDavRequestException(
            WebDavError.Unknown(429, "WebDAV is cooling down; retry in ${(cooldownUntilMs - now + 999) / 1000} seconds"),
        )
        while (recentRequests.isNotEmpty() && now - recentRequests.first() >= WINDOW_MS) {
            recentRequests.removeFirst()
        }
        if (intervalMs > 0 && recentRequests.size >= REQUEST_BUDGET) {
            cooldownUntilMs = recentRequests.first() + WINDOW_MS
            throw WebDavRequestException(WebDavError.Unknown(429, "WebDAV request budget reached; retry later"))
        }
        (nextRequestMs - now).takeIf { it > 0 }?.let { wait(it) }
        val started = nowMs()
        nextRequestMs = started + intervalMs
        recentRequests.addLast(started)
    }

    internal suspend fun rejectCredentials(identity: String) = mutex.withLock {
        rejectedCredentials.add(identity)
    }

    internal suspend fun retryCredentials(identity: String) = mutex.withLock {
        rejectedCredentials.remove(identity)
    }

    internal suspend fun coolDown(retryAfter: String?) = mutex.withLock {
        val now = nowMs()
        val requested = retryAfter?.trim()?.toLongOrNull()?.coerceAtLeast(0)?.let {
            it.coerceAtMost(24 * 60 * 60) * 1000
        } ?: runCatching {
            ZonedDateTime.parse(retryAfter, DateTimeFormatter.RFC_1123_DATE_TIME)
                .toInstant().toEpochMilli() - now
        }.getOrDefault(DEFAULT_COOLDOWN_MS)
        cooldownUntilMs = maxOf(cooldownUntilMs, now + requested.coerceAtLeast(1_000))
    }

    companion object {
        private const val WINDOW_MS = 30 * 60 * 1000L
        private const val REQUEST_BUDGET = 120
        private const val DEFAULT_COOLDOWN_MS = 60_000L
        private val accounts = ConcurrentHashMap<String, WebDavRequestPolicy>()

        fun forConfig(config: WebDavConfig): WebDavRequestPolicy {
            val endpoint = WebDavEndpoint(config)
            val key = "${endpoint.base.protocol.name}://${endpoint.base.host}:${endpoint.base.port}/${config.username}"
            return accounts.computeIfAbsent(key) {
                WebDavRequestPolicy(
                    intervalMs = if (endpoint.base.host.equals("dav.jianguoyun.com", true)) 1_000 else 0,
                )
            }
        }
    }
}

internal class WebDavRequestException(val error: WebDavError) : Exception()
