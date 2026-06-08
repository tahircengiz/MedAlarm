# Architecture

## Stack

| Layer | Choice | Reason |
|---|---|---|
| Language | **Kotlin** | Native Android, modern, concise |
| UI | **Jetpack Compose + Material 3** | Modern declarative UI, dynamic color, dark/light themes built-in |
| Min SDK | **26 (Android 8.0)** | Required APIs for exact alarms + notification channels; covers ~97% of active devices |
| Target SDK | latest stable (35 at time of writing) | Required for Play Store; ensures latest behavior compliance |
| Database | **Room** | Type-safe SQLite, Compose-friendly with Flow |
| Settings | **DataStore (Preferences)** | Modern, async, replaces SharedPreferences |
| DI | **Hilt** | Standard, simple, official |
| Scheduling | **AlarmManager** + **WorkManager** | AlarmManager for exact-time triggers; WorkManager for periodic health checks |
| Notifications | **NotificationCompat + Channels** | Required since Android 8; supports action buttons |
| TTS | **android.speech.tts.TextToSpeech** | Built-in, offline (most languages cached on device) |
| PDF | **android.graphics.pdf.PdfDocument** | Built-in, no third-party deps |
| JSON | **kotlinx.serialization** | Type-safe, no reflection |
| Logging | **Timber** (debug only) — no cloud sink | Local-only |
| Testing | JUnit5, Turbine, Compose UI tests | Standard |

### Deliberately excluded

These libraries are **forbidden** by the bloatware-free manifesto and
must never be added to `build.gradle`:

- Firebase (any module)
- Google Analytics / GA4
- AdMob / any ads SDK
- Crashlytics
- Mixpanel / Amplitude / Segment / any analytics SDK
- OkHttp / Retrofit / any HTTP client (we don't need internet)
- ML Kit cloud APIs

`INTERNET` permission must remain **out of the manifest**. Build a lint
rule or CI check to enforce this.

---

## Module / Package Structure

Single-module app (no need for multi-module overhead at this scale).
Packages organized by feature × layer:

```
com.medalarm.app
├── MedAlarmApplication.kt           // @HiltAndroidApp
├── MainActivity.kt                  // single-activity Compose host
│
├── data/
│   ├── local/
│   │   ├── MedAlarmDatabase.kt
│   │   ├── entity/
│   │   │   ├── MedicationEntity.kt
│   │   │   ├── ScheduleEntity.kt
│   │   │   └── DoseLogEntity.kt
│   │   ├── dao/
│   │   │   ├── MedicationDao.kt
│   │   │   ├── ScheduleDao.kt
│   │   │   └── DoseLogDao.kt
│   │   └── converter/               // Room TypeConverters (enums, lists)
│   ├── repository/
│   │   ├── MedicationRepository.kt
│   │   ├── DoseLogRepository.kt
│   │   └── SettingsRepository.kt    // DataStore-backed
│   └── alarm/
│       ├── AlarmScheduler.kt        // wraps AlarmManager
│       ├── MedicationAlarmReceiver.kt
│       ├── NotificationActionReceiver.kt
│       ├── BootCompletedReceiver.kt
│       └── TimeChangedReceiver.kt
│
├── domain/
│   ├── model/                       // pure Kotlin, no Room/Android deps
│   │   ├── Medication.kt
│   │   ├── Schedule.kt
│   │   ├── DoseLog.kt
│   │   └── Unit.kt                  // enum
│   ├── usecase/
│   │   ├── ScheduleNextDoseUseCase.kt
│   │   ├── MarkDoseTakenUseCase.kt
│   │   ├── SnoozeDoseUseCase.kt
│   │   ├── SkipDoseUseCase.kt
│   │   ├── CheckStockUseCase.kt
│   │   ├── GenerateDailyDoseLogsUseCase.kt   // creates PENDING rows for T+1
│   │   ├── ExportBackupUseCase.kt
│   │   └── ImportBackupUseCase.kt
│   └── repository/                  // interfaces
│
├── ui/
│   ├── theme/
│   │   ├── Color.kt
│   │   ├── Theme.kt                 // dynamic color + dark/light
│   │   └── Type.kt
│   ├── navigation/
│   │   └── MedAlarmNavHost.kt
│   ├── onboarding/                  // first-launch wizard + disclaimer
│   ├── home/                        // today's doses
│   ├── medication/                  // list, add, edit
│   ├── history/                     // log + PDF export trigger
│   ├── settings/
│   │   ├── SettingsScreen.kt
│   │   ├── SystemStatusScreen.kt    // permission status panel
│   │   └── BackupScreen.kt
│   └── common/                      // shared composables
│
├── notification/
│   ├── NotificationHelper.kt
│   └── NotificationChannels.kt
│
├── tts/
│   └── TtsHelper.kt
│
├── backup/
│   ├── JsonExporter.kt
│   └── JsonImporter.kt
│
├── pdf/
│   └── PdfReportBuilder.kt
│
├── permission/
│   ├── PermissionState.kt           // sealed class: Granted/Denied/NotRequested
│   ├── SystemHealthChecker.kt       // queries all 5 checks
│   └── OemAutostartHelper.kt        // OEM-specific intents
│
└── di/
    ├── DatabaseModule.kt
    ├── RepositoryModule.kt
    └── AlarmModule.kt
```

Tests mirror this structure under `src/test/` (unit) and
`src/androidTest/` (instrumented).

---

## Alarm & Notification Flow

This is the highest-risk subsystem. Android aggressively kills
background work to save battery. We design defensively.

### Trigger chain

```
┌─────────────────────────────────────────────────────────┐
│ 1. SCHEDULE                                             │
│    AlarmScheduler.scheduleNext(medicationId)            │
│    - reads Schedule, computes next dose time            │
│    - writes DoseLog row with status=PENDING             │
│    - registers AlarmManager.setExactAndAllowWhileIdle   │
│      with PendingIntent → MedicationAlarmReceiver       │
└──────────────────────┬──────────────────────────────────┘
                       │ (alarm fires, possibly during Doze)
                       ▼
┌─────────────────────────────────────────────────────────┐
│ 2. FIRE                                                 │
│    MedicationAlarmReceiver.onReceive                    │
│    - loads DoseLog + Medication                         │
│    - posts notification:                                │
│      ┌──────────────────────────────────────────┐      │
│      │ 💊 Aspirin                               │      │
│      │ 1 tablet — yemekten sonra                │      │
│      │ [ Aldım ] [ Ertele ] [ Atla ]            │      │
│      └──────────────────────────────────────────┘      │
│    - if TTS enabled: speaks medication name              │
│    - calls AlarmScheduler.scheduleNext for the          │
│      following dose IMMEDIATELY (don't wait for action) │
└──────────────────────┬──────────────────────────────────┘
                       │ (user taps an action)
                       ▼
┌─────────────────────────────────────────────────────────┐
│ 3. ACTION                                               │
│    NotificationActionReceiver                           │
│    - "Aldım"   → DoseLog.status = TAKEN                 │
│                  → Medication.stockAmount -= dosageAmount│
│                  → if stockAmount ≤ threshold:           │
│                       post low-stock notification        │
│    - "Ertele"  → if snoozeCount < maxSnoozeCount:        │
│                       DoseLog.status = SNOOZED           │
│                       snoozeCount++                      │
│                       AlarmManager set for +Nmin         │
│                  else:                                    │
│                       notification re-posted as ongoing  │
│                       (cannot dismiss until acted on)    │
│    - "Atla"    → DoseLog.status = SKIPPED               │
└─────────────────────────────────────────────────────────┘
```

### Recovery (the things that go wrong)

Android can lose scheduled alarms in these cases. Each has a defense:

| Event | Risk | Defense |
|---|---|---|
| Device reboot | All AlarmManager registrations lost | `BootCompletedReceiver` reschedules all active medications |
| Timezone change | Alarms fire at wrong time | `TimeChangedReceiver` reschedules |
| Date set manually | Same | Same |
| App killed by OEM | Future alarms gone | Periodic `WorkManager` (1×/day) re-validates and reschedules |
| App updated | AlarmManager preserves on `replaced`, but verify | Listen `MY_PACKAGE_REPLACED` |
| Doze mode | Inexact alarms get batched | Use `setExactAndAllowWhileIdle` (requires permission) |
| Battery optimization | Alarms still fire on modern Android with `setExactAndAllowWhileIdle`, but app can't post foreground work | Request `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` |
| OEM autostart restriction | App can't start in background → BroadcastReceiver may not run | Detect OEM, deep-link to autostart settings, educate user |
| User denies POST_NOTIFICATIONS | No notification visible | Onboarding requests; Settings shows red status |
| User revokes SCHEDULE_EXACT_ALARM | New alarms refuse to register | Settings shows red status + button to system settings |

### Daily PENDING dose generation

A `WorkManager` periodic job runs every night at 00:05 local time:

- For each active medication × schedule, compute all dose times for
  the next 24 hours
- Insert `DoseLog` rows with status=PENDING
- Schedule AlarmManager triggers for each

This gives us a query-able "today's plan" without recomputing schedules
on every UI load, and survives short outages.

---

## Theme & UI principles

- **Material 3** with dynamic color on Android 12+ (opt-out in settings)
- Static fallback palette: calm green/blue accent, high contrast text
- **Dark / Light / System** — user choice in Settings, persisted in DataStore
- **Minimalist**: large touch targets, generous spacing, one primary
  action per screen
- Compose-only navigation (Navigation Compose)
- Accessibility: TalkBack labels, min 14sp text, contrast ratio ≥ 4.5:1

---

## Localization (i18n)

- All user-facing strings in `res/values/strings.xml` (default = EN)
  and `res/values-tr/strings.xml`
- No hardcoded strings in code or composables
- Date/time formatting via `java.time` with the user's locale
- Language override in Settings (uses `AppCompatDelegate.setApplicationLocales`)

---

## Backup format (JSON)

Manual export → `medalarm-backup-YYYYMMDD.json` saved via
`ACTION_CREATE_DOCUMENT` (user picks location). Schema versioned.

```json
{
  "version": 1,
  "exportedAt": "2025-01-15T08:00:00Z",
  "medications": [...],
  "schedules": [...],
  "doseLogs": [...],
  "settings": {...}
}
```

Import via `ACTION_OPEN_DOCUMENT`, validates schema version, merges or
replaces (user choice).

---

## PDF report

User selects date range → `PdfReportBuilder` produces a single-file
PDF with:
- Header (date range, generated timestamp)
- Per-medication adherence summary (X taken / Y planned = Z%)
- Day-by-day log table
- Footer disclaimer

Saved via `ACTION_CREATE_DOCUMENT`.

---

## Testing strategy

- **Unit tests** for use cases (no Android deps in `domain/`)
- **Room tests** with in-memory database
- **Alarm scheduling tests** using `Robolectric` (verify PendingIntent
  registered with correct trigger time)
- **Compose UI tests** for critical flows (add medication, mark taken)
- Manual matrix on real devices: stock Android, Samsung One UI, Xiaomi
  MIUI/HyperOS — these have the most aggressive battery management

---

## Open questions (defer to later)

- License choice: GPL-3.0 vs Apache-2.0 vs MPL-2.0
- App icon design
- Play Store listing copy
- F-Droid compatibility (likely yes — fully FOSS, no proprietary deps)
