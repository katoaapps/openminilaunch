package com.katoaapps.openminilaunch.model

enum class IconSource {
    MINK,
    SYSTEM,
    ICON_PACK,
}

data class IconAppearance(
    val source: IconSource = IconSource.MINK,
    val iconPackPackage: String? = null,
    val minkIconColorArgb: Int? = null,
)
