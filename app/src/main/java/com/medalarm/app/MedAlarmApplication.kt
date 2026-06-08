package com.medalarm.app

import android.app.Application
import android.os.StrictMode
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.medalarm.app.data.alarm.ReliabilityWorker
import com.medalarm.app.domain.model.AppLanguage
import com.medalarm.app.domain.repository.SettingsRepository
import com.medalarm.app.notification.NotificationChannels
import dagger.hilt.android.HiltAndroidApp
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject

@HiltAndroidApp
class MedAlarmApplication : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var settingsRepository: SettingsRepository

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()

        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
            enableStrictMode()
        }

        // Channels are safe to (re)create at every boot — Android merges by ID.
        NotificationChannels.createAll(this)

        // Apply the persisted language preference whenever it changes. AppCompatDelegate
        // handles configuration changes and Activity recreation for us.
        settingsRepository.settings
            .map { it.language }
            .distinctUntilChanged()
            .onEach { applyLanguage(it) }
            .launchIn(appScope)

        scheduleReliabilityWorker()
    }

    /**
     * Periodic safety net (every 6h) that re-arms dropped alarms and sweeps overdue
     * doses to MISSED. KEEP policy so re-launches don't reset the schedule. Runs on
     * every process start (including when a receiver wakes us), which is a good
     * moment to re-validate.
     */
    private fun scheduleReliabilityWorker() {
        val request = PeriodicWorkRequestBuilder<ReliabilityWorker>(
            ReliabilityWorker.PERIOD_HOURS, TimeUnit.HOURS
        ).build()
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            ReliabilityWorker.UNIQUE_PERIODIC_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }

    private suspend fun applyLanguage(language: AppLanguage) {
        val tag = when (language) {
            AppLanguage.SYSTEM -> ""           // empty list = follow system
            AppLanguage.TR -> "tr"
            AppLanguage.EN -> "en"
        }
        // setApplicationLocales must be called on the main thread.
        withContext(Dispatchers.Main) {
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(tag))
        }
    }

    /**
     * Catches accidental main-thread I/O, leaked closeables, and other reliability hazards
     * during development. Disabled in release per StrictMode best practices.
     */
    private fun enableStrictMode() {
        StrictMode.setThreadPolicy(
            StrictMode.ThreadPolicy.Builder()
                .detectDiskReads()
                .detectDiskWrites()
                .detectNetwork()
                .detectCustomSlowCalls()
                .penaltyLog()
                .build()
        )
        StrictMode.setVmPolicy(
            StrictMode.VmPolicy.Builder()
                .detectLeakedSqlLiteObjects()
                .detectLeakedClosableObjects()
                .detectActivityLeaks()
                .penaltyLog()
                .build()
        )
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .setMinimumLoggingLevel(if (BuildConfig.DEBUG) android.util.Log.DEBUG else android.util.Log.INFO)
            .build()
}
