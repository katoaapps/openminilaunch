package com.katoaapps.minkpad.core

enum class MinkPadHostSwitchPlan {
    NO_OP,
    REPLACE_CONNECTING_HOST,
    QUEUE_AFTER_DISCONNECT,
    DISCONNECT_THEN_CONNECT,
    CONNECT_NOW,
}

fun minkPadHostSwitchPlan(
    status: MinkPadStatus,
    currentAddress: String?,
    targetAddress: String,
): MinkPadHostSwitchPlan = when {
    status == MinkPadStatus.CONNECTING && currentAddress == targetAddress -> MinkPadHostSwitchPlan.NO_OP
    status == MinkPadStatus.CONNECTING -> MinkPadHostSwitchPlan.REPLACE_CONNECTING_HOST
    status == MinkPadStatus.DISCONNECTING -> MinkPadHostSwitchPlan.QUEUE_AFTER_DISCONNECT
    status == MinkPadStatus.CONNECTED && currentAddress == targetAddress -> MinkPadHostSwitchPlan.NO_OP
    status == MinkPadStatus.CONNECTED -> MinkPadHostSwitchPlan.DISCONNECT_THEN_CONNECT
    else -> MinkPadHostSwitchPlan.CONNECT_NOW
}
