# Data Model

All persistent state lives on-device. Two stores:

- **Room** (SQLite) for structured/relational data
- **DataStore (Preferences)** for user settings

Never any cloud, account, or remote store.

---

## Room entities

### `medications` table

```kotlin
@Entity(tableName = "medications")
data class MedicationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val unit: MedicationUnit,           // enum, see below
    val dosageAmount: Float,            // amount per dose (e.g. 1.0 tablet, 5.0 ml)
    val notes: String? = null,
    val colorHex: String? = null,       // UI accent — null = default
    val iconKey: String? = null,        // resource key from a fixed icon set

    // Treatment window
    val startDate: LocalDate,
    val endDate: LocalDate? = null,     // null = open-ended

    // Stock tracking — all nullable; null = "don't track stock for this med"
    val stockAmount: Float? = null,
    val stockThreshold: Float? = null,
    val lowStockNotified: Boolean = false,  // prevents repeated nag notifications

    val isActive: Boolean = true,       // soft-delete + pause
    val createdAt: Instant,
    val updatedAt: Instant
)
```

```kotlin
enum class MedicationUnit {
    TABLET, CAPSULE, ML, MG, DROP, PUFF, SACHET, OTHER
}
```

When `unit = OTHER`, the UI shows a free-text field that's stored as
part of `notes` (we don't add a column for a rare case).

### `schedules` table

A medication can have multiple schedules (e.g. "8am every day" + "every
6 hours when needed" — though we'll keep one schedule per med in MVP and
allow multiple in v1.1).

```kotlin
@Entity(
    tableName = "schedules",
    foreignKeys = [ForeignKey(
        entity = MedicationEntity::class,
        parentColumns = ["id"],
        childColumns = ["medicationId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("medicationId")]
)
data class ScheduleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val medicationId: Long,
    val type: ScheduleType,

    // For DAILY_TIMES: a list of HH:mm strings
    val times: List<String> = emptyList(),  // TypeConverter → JSON

    // For INTERVAL_HOURS
    val intervalHours: Int? = null,
    val intervalStartTime: String? = null,  // HH:mm, when the interval cycle starts each day

    // For WEEKLY_DAYS: bitmask, Mon=1, Tue=2, Wed=4, Thu=8, Fri=16, Sat=32, Sun=64
    val daysOfWeekBitmask: Int = 0,

    val mealRelation: MealRelation = MealRelation.NONE
)
```

```kotlin
enum class ScheduleType { DAILY_TIMES, INTERVAL_HOURS, WEEKLY_DAYS }
enum class MealRelation { NONE, BEFORE, AFTER, WITH }
```

### `dose_logs` table

This is the source of truth for "did the user take it". One row per
planned dose. PENDING rows are generated nightly for the next 24h.

```kotlin
@Entity(
    tableName = "dose_logs",
    foreignKeys = [
        ForeignKey(
            entity = MedicationEntity::class,
            parentColumns = ["id"],
            childColumns = ["medicationId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = ScheduleEntity::class,
            parentColumns = ["id"],
            childColumns = ["scheduleId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("medicationId"),
        Index("scheduleId"),
        Index("scheduledAt"),
        Index("status")
    ]
)
data class DoseLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val medicationId: Long,
    val scheduleId: Long,

    val scheduledAt: Instant,           // when it SHOULD be taken
    val status: DoseStatus,

    val actionAt: Instant? = null,      // when user tapped a button
    val snoozeCount: Int = 0,
    val snoozeUntil: Instant? = null,   // when snoozed alarm will re-fire

    // Snapshot of dosage at time of scheduling — so historic reports
    // reflect what was actually planned, not current medication state
    val dosageAmountSnapshot: Float,
    val unitSnapshot: MedicationUnit
)
```

```kotlin
enum class DoseStatus { PENDING, TAKEN, SKIPPED, SNOOZED, MISSED }
```

`MISSED` is set by the periodic WorkManager job: a PENDING dose whose
`scheduledAt` is more than 4 hours in the past with no action gets
flipped to MISSED, so the home screen doesn't accumulate stale items.

---

## DAOs (sketch)

```kotlin
@Dao
interface MedicationDao {
    @Insert suspend fun insert(med: MedicationEntity): Long
    @Update suspend fun update(med: MedicationEntity)
    @Query("UPDATE medications SET isActive = 0 WHERE id = :id")
    suspend fun softDelete(id: Long)

    @Query("SELECT * FROM medications WHERE isActive = 1 ORDER BY name")
    fun observeActive(): Flow<List<MedicationEntity>>

    @Query("SELECT * FROM medications WHERE id = :id")
    suspend fun getById(id: Long): MedicationEntity?

    @Query("UPDATE medications SET stockAmount = stockAmount - :amount, lowStockNotified = 0 WHERE id = :id")
    suspend fun decrementStock(id: Long, amount: Float)
}

@Dao
interface ScheduleDao {
    @Insert suspend fun insert(schedule: ScheduleEntity): Long
    @Update suspend fun update(schedule: ScheduleEntity)
    @Query("SELECT * FROM schedules WHERE medicationId = :medId")
    suspend fun getForMedication(medId: Long): List<ScheduleEntity>
}

@Dao
interface DoseLogDao {
    @Insert suspend fun insert(log: DoseLogEntity): Long
    @Update suspend fun update(log: DoseLogEntity)

    @Query("""
        SELECT * FROM dose_logs
        WHERE scheduledAt BETWEEN :start AND :end
        ORDER BY scheduledAt
    """)
    fun observeRange(start: Instant, end: Instant): Flow<List<DoseLogEntity>>

    @Query("""
        SELECT * FROM dose_logs
        WHERE status = 'PENDING' AND scheduledAt < :now
    """)
    suspend fun findOverduePending(now: Instant): List<DoseLogEntity>

    @Query("""
        UPDATE dose_logs SET status = 'MISSED'
        WHERE status = 'PENDING' AND scheduledAt < :threshold
    """)
    suspend fun markOverdueAsMissed(threshold: Instant)
}
```

---

## TypeConverters

```kotlin
class Converters {
    @TypeConverter fun fromInstant(v: Instant?): Long? = v?.toEpochMilli()
    @TypeConverter fun toInstant(v: Long?): Instant? = v?.let(Instant::ofEpochMilli)

    @TypeConverter fun fromLocalDate(v: LocalDate?): String? = v?.toString()
    @TypeConverter fun toLocalDate(v: String?): LocalDate? = v?.let(LocalDate::parse)

    @TypeConverter fun fromStringList(v: List<String>): String = Json.encodeToString(v)
    @TypeConverter fun toStringList(v: String): List<String> = Json.decodeFromString(v)

    // enums — store as strings (more robust than ordinals across versions)
    @TypeConverter fun fromUnit(v: MedicationUnit) = v.name
    @TypeConverter fun toUnit(v: String) = MedicationUnit.valueOf(v)
    // … same for ScheduleType, MealRelation, DoseStatus
}
```

---

## DataStore (Preferences) — user settings

```kotlin
data class UserSettings(
    val language: AppLanguage = AppLanguage.SYSTEM,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val useDynamicColor: Boolean = true,
    val ttsEnabled: Boolean = false,

    // Snooze
    val defaultSnoozeMinutes: Int = 15,
    val maxSnoozeCount: Int = 3,             // 0 = unlimited (UI says "sınırsız")

    // Notifications
    val vibrationEnabled: Boolean = true,
    val notificationSoundUri: String? = null,  // null = system default

    // Stock
    val defaultLowStockThreshold: Float = 5f,

    // Disclaimer
    val disclaimerAccepted: Boolean = false,
    val disclaimerAcceptedAt: Instant? = null,

    // Onboarding
    val onboardingCompleted: Boolean = false
)

enum class AppLanguage { SYSTEM, TR, EN }
enum class ThemeMode { SYSTEM, LIGHT, DARK }
```

Backed by Preferences DataStore; one `Flow<UserSettings>` consumed by
the `SettingsRepository`.

---

## Migration policy

- **Pre-1.0 (no users)**: destructive migrations allowed.
  `Room.databaseBuilder(...).fallbackToDestructiveMigration()` for now,
  removed before the first public release.
- **Post-1.0**: every schema change ships with a named `Migration`.
  Migrations are unit-tested with `MigrationTestHelper`.
- Schema dumps committed to `app/schemas/`.

---

## Backup JSON schema (v1)

```json
{
  "version": 1,
  "exportedAt": "2025-01-15T08:00:00Z",
  "appVersion": "1.0.0",
  "medications": [
    {
      "id": 1,
      "name": "Aspirin",
      "unit": "TABLET",
      "dosageAmount": 1.0,
      "notes": null,
      "colorHex": "#4CAF50",
      "iconKey": "pill",
      "startDate": "2025-01-10",
      "endDate": null,
      "stockAmount": 22.0,
      "stockThreshold": 5.0,
      "isActive": true,
      "createdAt": "2025-01-10T07:00:00Z",
      "updatedAt": "2025-01-15T07:55:00Z"
    }
  ],
  "schedules": [
    {
      "id": 1,
      "medicationId": 1,
      "type": "DAILY_TIMES",
      "times": ["08:00", "20:00"],
      "intervalHours": null,
      "intervalStartTime": null,
      "daysOfWeekBitmask": 0,
      "mealRelation": "AFTER"
    }
  ],
  "doseLogs": [
    {
      "id": 1,
      "medicationId": 1,
      "scheduleId": 1,
      "scheduledAt": "2025-01-15T08:00:00Z",
      "status": "TAKEN",
      "actionAt": "2025-01-15T08:03:21Z",
      "snoozeCount": 0,
      "snoozeUntil": null,
      "dosageAmountSnapshot": 1.0,
      "unitSnapshot": "TABLET"
    }
  ],
  "settings": { "...": "as in UserSettings above" }
}
```

Import strategies (user chooses):
- **Replace**: wipes current DB, restores from file
- **Merge**: keeps existing meds, adds non-conflicting rows from file
  (conflict = same name + same startDate)
