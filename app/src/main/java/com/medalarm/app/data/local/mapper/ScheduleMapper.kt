package com.medalarm.app.data.local.mapper

import com.medalarm.app.data.local.entity.ScheduleEntity
import com.medalarm.app.domain.model.Schedule
import java.time.LocalTime
import java.time.format.DateTimeFormatter

private val TIME_FMT: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

internal fun ScheduleEntity.toDomain(): Schedule = Schedule(
    id = id,
    medicationId = medicationId,
    type = type,
    times = times.map { LocalTime.parse(it, TIME_FMT) },
    intervalHours = intervalHours,
    intervalStartTime = intervalStartTime?.let { LocalTime.parse(it, TIME_FMT) },
    daysOfWeek = Schedule.bitmaskToDaysOfWeek(daysOfWeekBitmask),
    mealRelation = mealRelation
)

internal fun Schedule.toEntity(): ScheduleEntity = ScheduleEntity(
    id = id,
    medicationId = medicationId,
    type = type,
    times = times.map { it.format(TIME_FMT) },
    intervalHours = intervalHours,
    intervalStartTime = intervalStartTime?.format(TIME_FMT),
    daysOfWeekBitmask = Schedule.daysOfWeekToBitmask(daysOfWeek),
    mealRelation = mealRelation
)
