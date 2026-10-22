@file:androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)

package os.kei.ui.page.main.student

import android.content.Context
import androidx.media3.common.util.BitmapLoader
import androidx.media3.datasource.DataSourceBitmapLoader
import androidx.media3.datasource.DefaultDataSource

internal fun createGameKeeMediaBitmapLoader(context: Context): BitmapLoader =
    DataSourceBitmapLoader.Builder(context)
        .setDataSourceFactory(DefaultDataSource.Factory(context, createGameKeeHttpDataSourceFactory()))
        .setMaximumOutputDimension(
            androidx.media3.session.MediaSession.getBitmapDimensionLimit(context) * 2 - 1
        )
        .build()
