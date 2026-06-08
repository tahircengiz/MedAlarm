package com.medalarm.app.data.local.converter

import androidx.room.TypeConverter
import com.medalarm.app.domain.model.DoseStatus
import com.medalarm.app.domain.model.MealRelation
import com.medalarm.app.domain.model.MedicationUnit
import com.medalarm.app.domain.model.ScheduleType
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import java.time.Instant
import java.time.LocalDate

/**
 * Room type converters. Enums stored by name (not ordinal) — robust against
 * reordering in source. Instants stored as epoch millis. Lists stored as JSON.
 */
class Converters {

    @TypeConverter fun fromInstant(value: Instant?): Long? = value?.toEpochMilli()
    @TypeConverter fun toInstant(value: Long?): Instant? = value?.let(Instant::ofEpochMilli)

    @TypeConverter fun fromLocalDate(value: LocalDate?): String? = value?.toString()
    @TypeConverter fun toLocalDate(value: String?): LocalDate? = value?.let(LocalDate::parse)

    @TypeConverter
    fun fromStringList(value: List<String>): String =
        Json.encodeToString(ListSerializer(String.serializer()), value)

    @TypeConverter
    fun toStringList(value: String): List<String> =
        if (value.isBlank()) emptyList()
        else Json.decodeFromString(ListSerializer(String.serializer()), value)

    @TypeConverter fun fromUnit(value: MedicationUnit): String = value.name
    @TypeConverter fun toUnit(value: String): MedicationUnit = MedicationUnit.valueOf(value)

    @TypeConverter fun fromScheduleType(value: ScheduleType): String = value.name
    @TypeConverter fun toScheduleType(value: String): ScheduleType = ScheduleType.valueOf(value)

    @TypeConverter fun fromMealRelation(value: MealRelation): String = value.name
    @TypeConverter fun toMealRelation(value: String): MealRelation = MealRelation.valueOf(value)

    @TypeConverter fun fromDoseStatus(value: DoseStatus): String = value.name
    @TypeConverter fun toDoseStatus(value: String): DoseStatus = DoseStatus.valueOf(value)
}
