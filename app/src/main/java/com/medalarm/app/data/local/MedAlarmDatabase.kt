package com.medalarm.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.medalarm.app.data.local.converter.Converters
import com.medalarm.app.data.local.dao.DoseLogDao
import com.medalarm.app.data.local.dao.MedicationDao
import com.medalarm.app.data.local.dao.ScheduleDao
import com.medalarm.app.data.local.entity.DoseLogEntity
import com.medalarm.app.data.local.entity.MedicationEntity
import com.medalarm.app.data.local.entity.ScheduleEntity

@Database(
    entities = [
        MedicationEntity::class,
        ScheduleEntity::class,
        DoseLogEntity::class
    ],
    version = 1,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class MedAlarmDatabase : RoomDatabase() {
    abstract fun medicationDao(): MedicationDao
    abstract fun scheduleDao(): ScheduleDao
    abstract fun doseLogDao(): DoseLogDao

    companion object {
        const val DB_NAME = "medalarm.db"
    }
}
