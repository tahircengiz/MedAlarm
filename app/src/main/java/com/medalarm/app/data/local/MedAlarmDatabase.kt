package com.medalarm.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
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
    version = 2,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class MedAlarmDatabase : RoomDatabase() {
    abstract fun medicationDao(): MedicationDao
    abstract fun scheduleDao(): ScheduleDao
    abstract fun doseLogDao(): DoseLogDao

    companion object {
        const val DB_NAME = "medalarm.db"

        /** v2: box photo support — medications.photoPath (nullable, app-internal file path). */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE medications ADD COLUMN photoPath TEXT")
            }
        }
    }
}
