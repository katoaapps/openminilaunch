package com.katoaapps.openminilaunch.ui.minkboard

import android.content.Context
import com.katoaapps.openminilaunch.R
import com.katoaapps.openminilaunch.features.minkboard.OpenMinkCardUnavailableException

/** Maps launcher command failures to host-owned, localized presentation text. */
internal fun Context.openMinkBoardErrorMessage(failure: Throwable): String = when (failure) {
    is OpenMinkCardUnavailableException -> getString(R.string.minkboard_card_unavailable)
    else -> failure.message ?: getString(R.string.minkboard_change_failed)
}
