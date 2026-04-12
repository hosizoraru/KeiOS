package com.example.keios.ui.page.main.student

import android.content.Context
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory

private const val GAMEKEE_REFERER = "https://www.gamekee.com/"
private const val GAMEKEE_ORIGIN = "https://www.gamekee.com"
private const val GAMEKEE_DESKTOP_UA =
    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/146.0.0.0 Safari/537.36"

private val GAMEKEE_DEFAULT_HEADERS = mapOf(
    "Accept" to "*/*",
    "Accept-Language" to "zh-CN",
    "Referer" to GAMEKEE_REFERER,
    "Origin" to GAMEKEE_ORIGIN,
    "User-Agent" to GAMEKEE_DESKTOP_UA,
    "device-num" to "1",
    "game-alias" to "ba"
)

internal fun createGameKeeHttpDataSourceFactory(): DefaultHttpDataSource.Factory {
    return DefaultHttpDataSource.Factory()
        .setAllowCrossProtocolRedirects(true)
        .setConnectTimeoutMs(8_000)
        .setReadTimeoutMs(12_000)
        .setUserAgent(GAMEKEE_DESKTOP_UA)
        .setDefaultRequestProperties(GAMEKEE_DEFAULT_HEADERS)
}

internal fun createGameKeeMediaSourceFactory(context: Context): DefaultMediaSourceFactory {
    val httpFactory = createGameKeeHttpDataSourceFactory()
    return DefaultMediaSourceFactory(DefaultDataSource.Factory(context, httpFactory))
}
