package com.medalarm.app.di

import com.medalarm.app.data.local.MedAlarmDatabase
import com.medalarm.app.data.local.dao.DoseLogDao
import com.medalarm.app.data.local.dao.MedicationDao
import com.medalarm.app.data.local.dao.ScheduleDao
import com.medalarm.app.data.repository.DoseLogRepositoryImpl
import com.medalarm.app.data.repository.MedicationRepositoryImpl
import com.medalarm.app.domain.repository.DoseLogRepository
import com.medalarm.app.domain.repository.MedicationRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {

    @Provides
    @Singleton
    fun provideMedicationRepository(
        db: MedAlarmDatabase,
        medicationDao: MedicationDao,
        scheduleDao: ScheduleDao
    ): MedicationRepository = MedicationRepositoryImpl(db, medicationDao, scheduleDao)

    @Provides
    @Singleton
    fun provideDoseLogRepository(
        doseLogDao: DoseLogDao
    ): DoseLogRepository = DoseLogRepositoryImpl(doseLogDao)
}
