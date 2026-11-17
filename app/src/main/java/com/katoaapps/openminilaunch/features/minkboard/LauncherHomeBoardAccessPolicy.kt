package com.katoaapps.openminilaunch.features.minkboard

/**
 * Keeps a locked board's card content off Home until the current process has an access grant.
 *
 * Board names remain available to the board picker so users can navigate to the board and unlock
 * it. The grant itself is process-memory-only and is still owned by the canonical MinkBoard access
 * session controller.
 */
internal fun canRevealLauncherHomeBoard(
    isLocked: Boolean,
    accessGranted: Boolean,
): Boolean = !isLocked || accessGranted
