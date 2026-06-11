package com.medalarm.app.data.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.medalarm.app.domain.model.AppLanguage
import com.medalarm.app.domain.model.SwipeAction
import com.medalarm.app.domain.model.ThemeMode
import com.medalarm.app.domain.model.UserSettings
import com.medalarm.app.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.time.Instant

internal class SettingsRepositoryImpl(
    private val dataStore: DataStore<Preferences>
) : SettingsRepository {

    override val settings: Flow<UserSettings> = dataStore.data.map { it.toUserSettings() }

    override suspend fun get(): UserSettings = settings.first()

    override suspend fun setLanguage(language: AppLanguage) =
        dataStore.edit { it[Keys.language] = language.name }.unit()

    override suspend fun setThemeMode(mode: ThemeMode) =
        dataStore.edit { it[Keys.themeMode] = mode.name }.unit()

    override suspend fun setUseDynamicColor(value: Boolean) =
        dataStore.edit { it[Keys.useDynamicColor] = value }.unit()

    override suspend fun setTtsEnabled(value: Boolean) =
        dataStore.edit { it[Keys.ttsEnabled] = value }.unit()

    override suspend fun setDefaultSnoozeMinutes(minutes: Int) =
        dataStore.edit { it[Keys.defaultSnoozeMinutes] = minutes }.unit()

    override suspend fun setMaxSnoozeCount(count: Int) =
        dataStore.edit { it[Keys.maxSnoozeCount] = count }.unit()

    override suspend fun setVibrationEnabled(value: Boolean) =
        dataStore.edit { it[Keys.vibrationEnabled] = value }.unit()

    override suspend fun setNotificationSoundUri(uri: String?) =
        dataStore.edit {
            if (uri == null) it.remove(Keys.notificationSoundUri)
            else it[Keys.notificationSoundUri] = uri
        }.unit()

    override suspend fun setDefaultLowStockThreshold(value: Float) =
        dataStore.edit { it[Keys.defaultLowStockThreshold] = value }.unit()

    override suspend fun setSwipeRightAction(action: SwipeAction) =
        dataStore.edit { it[Keys.swipeRightAction] = action.name }.unit()

    override suspend fun setSwipeLeftAction(action: SwipeAction) =
        dataStore.edit { it[Keys.swipeLeftAction] = action.name }.unit()

    override suspend fun setLargeTextMode(value: Boolean) =
        dataStore.edit { it[Keys.largeTextMode] = value }.unit()

    override suspend fun setDisclaimerAccepted() {
        dataStore.edit {
            it[Keys.disclaimerAccepted] = true
            it[Keys.disclaimerAcceptedAt] = Instant.now().toEpochMilli()
        }
    }

    override suspend fun setOnboardingCompleted(value: Boolean) =
        dataStore.edit { it[Keys.onboardingCompleted] = value }.unit()

    override suspend fun setUserConfirmedOemAutostart(value: Boolean) =
        dataStore.edit { it[Keys.userConfirmedOemAutostart] = value }.unit()

    private fun Preferences.toUserSettings(): UserSettings {
        val defaults = UserSettings()
        return UserSettings(
            language = this[Keys.language]?.let { runCatching { AppLanguage.valueOf(it) }.getOrNull() }
                ?: defaults.language,
            themeMode = this[Keys.themeMode]?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() }
                ?: defaults.themeMode,
            useDynamicColor = this[Keys.useDynamicColor] ?: defaults.useDynamicColor,
            ttsEnabled = this[Keys.ttsEnabled] ?: defaults.ttsEnabled,
            defaultSnoozeMinutes = this[Keys.defaultSnoozeMinutes] ?: defaults.defaultSnoozeMinutes,
            maxSnoozeCount = this[Keys.maxSnoozeCount] ?: defaults.maxSnoozeCount,
            vibrationEnabled = this[Keys.vibrationEnabled] ?: defaults.vibrationEnabled,
            notificationSoundUri = this[Keys.notificationSoundUri],
            defaultLowStockThreshold = this[Keys.defaultLowStockThreshold] ?: defaults.defaultLowStockThreshold,
            swipeRightAction = this[Keys.swipeRightAction]?.let { runCatching { SwipeAction.valueOf(it) }.getOrNull() }
                ?: defaults.swipeRightAction,
            swipeLeftAction = this[Keys.swipeLeftAction]?.let { runCatching { SwipeAction.valueOf(it) }.getOrNull() }
                ?: defaults.swipeLeftAction,
            largeTextMode = this[Keys.largeTextMode] ?: defaults.largeTextMode,
            disclaimerAccepted = this[Keys.disclaimerAccepted] ?: defaults.disclaimerAccepted,
            disclaimerAcceptedAt = this[Keys.disclaimerAcceptedAt]?.let(Instant::ofEpochMilli),
            onboardingCompleted = this[Keys.onboardingCompleted] ?: defaults.onboardingCompleted,
            userConfirmedOemAutostart = this[Keys.userConfirmedOemAutostart] ?: defaults.userConfirmedOemAutostart
        )
    }

    private fun Preferences.unit() = Unit

    private object Keys {
        val language = stringPreferencesKey("language")
        val themeMode = stringPreferencesKey("theme_mode")
        val useDynamicColor = booleanPreferencesKey("use_dynamic_color")
        val ttsEnabled = booleanPreferencesKey("tts_enabled")
        val defaultSnoozeMinutes = intPreferencesKey("default_snooze_minutes")
        val maxSnoozeCount = intPreferencesKey("max_snooze_count")
        val vibrationEnabled = booleanPreferencesKey("vibration_enabled")
        val notificationSoundUri = stringPreferencesKey("notification_sound_uri")
        val defaultLowStockThreshold = floatPreferencesKey("default_low_stock_threshold")
        val swipeRightAction = stringPreferencesKey("swipe_right_action")
        val swipeLeftAction = stringPreferencesKey("swipe_left_action")
        val largeTextMode = booleanPreferencesKey("large_text_mode")
        val disclaimerAccepted = booleanPreferencesKey("disclaimer_accepted")
        val disclaimerAcceptedAt = longPreferencesKey("disclaimer_accepted_at")
        val onboardingCompleted = booleanPreferencesKey("onboarding_completed")
        val userConfirmedOemAutostart = booleanPreferencesKey("user_confirmed_oem_autostart")
    }
}
