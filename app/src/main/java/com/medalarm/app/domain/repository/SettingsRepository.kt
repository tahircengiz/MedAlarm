package com.medalarm.app.domain.repository

import com.medalarm.app.domain.model.AppLanguage
import com.medalarm.app.domain.model.SwipeAction
import com.medalarm.app.domain.model.ThemeMode
import com.medalarm.app.domain.model.UserSettings
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {

    val settings: Flow<UserSettings>
    suspend fun get(): UserSettings

    suspend fun setLanguage(language: AppLanguage)
    suspend fun setThemeMode(mode: ThemeMode)
    suspend fun setUseDynamicColor(value: Boolean)
    suspend fun setTtsEnabled(value: Boolean)

    suspend fun setDefaultSnoozeMinutes(minutes: Int)
    suspend fun setMaxSnoozeCount(count: Int)

    suspend fun setVibrationEnabled(value: Boolean)
    suspend fun setNotificationSoundUri(uri: String?)
    suspend fun setDefaultLowStockThreshold(value: Float)

    suspend fun setSwipeRightAction(action: SwipeAction)
    suspend fun setSwipeLeftAction(action: SwipeAction)
    suspend fun setLargeTextMode(value: Boolean)

    suspend fun setDisclaimerAccepted()
    suspend fun setOnboardingCompleted(value: Boolean)
    suspend fun setUserConfirmedOemAutostart(value: Boolean)
}
