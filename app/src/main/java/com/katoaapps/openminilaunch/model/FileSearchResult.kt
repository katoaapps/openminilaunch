package com.katoaapps.openminilaunch.model

import android.net.Uri

data class FileSearchResult(
    val name: String,
    val uri: Uri,
    val mimeType: String,
    val modifiedAt: Long,
)
