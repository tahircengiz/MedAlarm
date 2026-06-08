package com.medalarm.app.permission

enum class CheckStatus {
    /** Everything is fine. */
    OK,

    /** Recommended but not critical (e.g. battery optimization). UI shows yellow. */
    NEEDS_ATTENTION,

    /** Critical for alarms to fire; UI shows red and blocks alarm creation. */
    BLOCKED,

    /** Cannot be queried (OEM autostart on some devices). UI shows gray. */
    UNKNOWN
}

data class SystemHealthReport(
    val notificationPermission: CheckStatus,
    val exactAlarmPermission: CheckStatus,
    val batteryOptimization: CheckStatus,
    val notificationsEnabled: CheckStatus,
    val alarmChannelImportance: CheckStatus,
    val oemAutostart: CheckStatus
) {
    /** True if any critical check is BLOCKED — UI should show the persistent banner. */
    val hasCriticalIssue: Boolean
        get() = notificationPermission == CheckStatus.BLOCKED ||
            exactAlarmPermission == CheckStatus.BLOCKED ||
            notificationsEnabled == CheckStatus.BLOCKED ||
            alarmChannelImportance == CheckStatus.BLOCKED
}
