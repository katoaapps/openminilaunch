package com.katoaapps.openminilaunch.features.minkspace.media

import android.content.Context
import android.content.Intent

internal object MediaActions {
    fun open(context: Context, item: MediaItem): Boolean = runCatching {
        context.startActivity(
            Intent(Intent.ACTION_VIEW)
                .setDataAndType(item.uri, item.mimeType)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION),
        )
        true
    }.getOrDefault(false)
}
