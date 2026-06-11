package com.medalarm.app.domain.model

import java.time.Instant

data class UserSettings(
    val language: AppLanguage = AppLanguage.SYSTEM,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val useDynamicColor: Boolean = true,
    val ttsEnabled: Boolean = false,

    // Snooze
    val defaultSnoozeMinutes: Int = 15,
    /** Maximum times a single dose can be snoozed. 0 = unlimited. */
    val maxSnoozeCount: Int = 3,

    // Notifications
    val vibrationEnabled: Boolean = true,
    /** Null = system default. */
    val notificationSoundUri: String? = null,

    // Stock
    val defaultLowStockThreshold: Float = 5f,

    // Home screen swipe gestures (elderly-friendly quick actions)
    val swipeRightAction: SwipeAction = SwipeAction.TAKEN,
    val swipeLeftAction: SwipeAction = SwipeAction.SNOOZE,

    /** Larger typography across the app for low-vision / elderly users. */
    val largeTextMode: Boolean = false,

    // Disclaimer / onboarding
    val disclaimerAccepted: Boolean = false,
    val disclaimerAcceptedAt: Instant? = null,
    val onboardingCompleted: Boolean = false,

    /** Set after the user confirms in-app that they enabled OEM autostart.
     *  We cannot read this state from Android, so we trust the user's confirmation. */
    val userConfirmedOemAutostart: Boolean = false
)

enum class AppLanguage { SYSTEM, TR, EN }
enum class ThemeMode { SYSTEM, LIGHT, DARK }

/** What a horizontal swipe on a Home dose card does. NONE disables that direction. */
enum class SwipeAction { TAKEN, SNOOZE, SKIP, NONE }
