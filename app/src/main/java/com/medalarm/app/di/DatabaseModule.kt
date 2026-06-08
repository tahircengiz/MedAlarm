package com.medalarm.app.di

import android.content.Context
import androidx.room.Room
import com.medalarm.app.data.local.MedAlarmDatabase
import com.medalarm.app.data.local.dao.DoseLogDao
import com.medalarm.app.data.local.dao.MedicationDao
import com.medalarm.app.data.local.dao.ScheduleDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context
    ): MedAlarmDatabase = Room.databaseBuilder(
        context,
        MedAlarmDatabase::class.java,
        MedAlarmDatabase.DB_NAME
    )
        // Pre-1.0: destructive migration is fine. Removed before first public release.
        .fallbackToDestructiveMigration()
        .build()

    @Provides fun provideMedicationDao(db: MedAlarmDatabase): MedicationDao = db.medicationDao()
    @Provides fun provideScheduleDao(db: MedAlarmDatabase): ScheduleDao = db.scheduleDao()
    @Provides fun provideDoseLogDao(db: MedAlarmDatabase): DoseLogDao = db.doseLogDao()
}
