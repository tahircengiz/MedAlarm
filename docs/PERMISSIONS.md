# Permissions & System Health

Medication reminders are a critical use case. Android's battery
management and OEM "optimizations" actively work against background
alarms. We treat permissions and system settings as a **first-class UX
concern**: the user sees their status at all times and can fix issues
in one tap.

---

## The 5 system checks

| # | Check | Required? | Where it lives |
|---|---|---|---|
| 1 | `POST_NOTIFICATIONS` permission | **Critical** | Runtime permission (Android 13+) |
| 2 | `SCHEDULE_EXACT_ALARM` permission | **Critical** (12+) | Special access (Android 12+) |
| 3 | Battery optimization exemption | **Recommended** | Per-app battery setting |
| 4 | Notifications enabled for our app | **Critical** | App notification settings |
| 5 | OEM autostart / background activity | **Recommended** | OEM-specific app settings |

"Critical" = without this, alarms will not function. The app shows a
red banner on the home screen and blocks alarm creation with an
inline call-to-action.

"Recommended" = alarms will *probably* work but may be killed under
sustained low-battery conditions. Shown as yellow warning.

---

## Onboarding flow (first launch)

```
┌─────────────────────────────────────────┐
│ 1. Hoş geldin / Welcome                 │
│    - app vision, social-responsibility  │
│    - "Devam et" / "Continue"            │
└────────────────┬────────────────────────┘
                 ▼
┌─────────────────────────────────────────┐
│ 2. Sorumluluk metni / Disclaimer        │
│    - full text (DISCLAIMER.md)          │
│    - checkbox: "Okudum ve kabul ediyorum"│
│    - "Devam et" disabled until checked  │
│    - persists disclaimerAccepted=true   │
└────────────────┬────────────────────────┘
                 ▼
┌─────────────────────────────────────────┐
│ 3. Dil seçimi / Language                │
│    - System / Türkçe / English          │
└────────────────┬────────────────────────┘
                 ▼
┌─────────────────────────────────────────┐
│ 4. Tema seçimi / Theme                  │
│    - System / Light / Dark              │
│    - (visual preview)                   │
└────────────────┬────────────────────────┘
                 ▼
┌─────────────────────────────────────────┐
│ 5. İzin sihirbazı / Permissions wizard  │
│    - One screen per check               │
│    - Plain explanation + one CTA button │
│      that opens the right system screen │
│    - "Atla" / "Skip" for non-critical   │
│    - "Devam et" disabled if critical    │
│      check still failing                │
└────────────────┬────────────────────────┘
                 ▼
┌─────────────────────────────────────────┐
│ 6. İlk ilacını ekle / Add first med     │
│    - or "Daha sonra ekle" / "Add later" │
└─────────────────────────────────────────┘
```

Each permission step shows:

- **What** the permission is, in plain language (no jargon)
- **Why** the app needs it (concrete example: "without this, your
  8 AM alarm may be silenced")
- **Status indicator** (green check, yellow warning, red X)
- A single primary button that deep-links to the exact system screen
- An "Already done" / "I'll fix this later" secondary option

---

## Settings → Sistem Durumu / System Status panel

Always-accessible mirror of the same checks, with live status:

```
┌─────────────────────────────────────────────────┐
│  Sistem Durumu                                  │
├─────────────────────────────────────────────────┤
│  ✅ Bildirim izni                Verildi        │
│  ✅ Tam zamanlı alarm izni       Verildi        │
│  ⚠️  Pil optimizasyonu           Kısıtlı  [Düzelt]│
│  ✅ Bildirimler açık             Açık           │
│  ⚠️  Xiaomi otomatik başlatma    Bilinmiyor [Aç] │
│                                                 │
│  Son kontrol: 2 dakika önce       [Yenile]      │
└─────────────────────────────────────────────────┘
```

`SystemHealthChecker` is queried on:
- Screen open
- App resume (after returning from a system settings screen)
- Tapping "Yenile" / "Refresh"

It returns a `SystemHealthReport`:

```kotlin
data class SystemHealthReport(
    val notificationPermission: CheckStatus,
    val exactAlarmPermission: CheckStatus,
    val batteryOptimization: CheckStatus,
    val notificationsEnabled: CheckStatus,
    val oemAutostart: CheckStatus
)

enum class CheckStatus { OK, NEEDS_ATTENTION, BLOCKED, UNKNOWN }
```

---

## Detailed check logic

### 1. POST_NOTIFICATIONS (Android 13+)

```kotlin
fun checkNotificationPermission(ctx: Context): CheckStatus =
    when {
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU -> CheckStatus.OK
        ContextCompat.checkSelfPermission(ctx, POST_NOTIFICATIONS) == PERMISSION_GRANTED -> CheckStatus.OK
        else -> CheckStatus.BLOCKED
    }
```

Fix CTA: `ActivityResultContracts.RequestPermission()` for
`POST_NOTIFICATIONS`. If permanently denied, deep-link to
app notification settings.

### 2. SCHEDULE_EXACT_ALARM (Android 12+)

Declared in manifest as `USE_EXACT_ALARM` (no user prompt, auto-granted
for alarm-clock-style apps — which we are). If we use
`SCHEDULE_EXACT_ALARM` instead, user must grant in Settings.

```kotlin
fun checkExactAlarm(ctx: Context): CheckStatus {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return CheckStatus.OK
    val am = ctx.getSystemService(AlarmManager::class.java)
    return if (am.canScheduleExactAlarms()) CheckStatus.OK else CheckStatus.BLOCKED
}
```

Fix CTA: `Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)`.

**Decision:** use `USE_EXACT_ALARM` since MedAlarm legitimately qualifies
as an alarm-clock-class app per Play policy. This avoids the user
having to grant the permission manually.

### 3. Battery optimization

```kotlin
fun checkBatteryOptimization(ctx: Context): CheckStatus {
    val pm = ctx.getSystemService(PowerManager::class.java)
    return if (pm.isIgnoringBatteryOptimizations(ctx.packageName))
        CheckStatus.OK else CheckStatus.NEEDS_ATTENTION
}
```

Fix CTA: `Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)`
with `data = Uri.parse("package:$packageName")`.

⚠️ **Play Policy nuance**: requesting whitelist requires
`REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` permission and policy
justification. Medication reminders qualify under "core functionality
that cannot be performed unless your app is exempt". We must declare
this use case in the Play Store listing.

### 4. Notifications enabled

```kotlin
fun checkNotificationsEnabled(ctx: Context): CheckStatus =
    if (NotificationManagerCompat.from(ctx).areNotificationsEnabled())
        CheckStatus.OK else CheckStatus.BLOCKED
```

Also check our specific channel:

```kotlin
fun checkAlarmChannel(ctx: Context): Boolean {
    val nm = ctx.getSystemService(NotificationManager::class.java)
    val channel = nm.getNotificationChannel(CHANNEL_MEDICATION_ALARM)
    return channel != null && channel.importance >= NotificationManager.IMPORTANCE_HIGH
}
```

### 5. OEM autostart

There is **no standard API**. We detect the OEM by `Build.MANUFACTURER`
and offer deep links to known settings screens. Status is always
`UNKNOWN` for fixed (granted) — we can't read it — but we mark `OK`
once the user confirms they've enabled it (stored in DataStore).

```kotlin
data class OemAutostartInfo(
    val oem: Oem,
    val intent: Intent?,        // null = no known intent for this OEM
    val instructionsResId: Int
)

enum class Oem { XIAOMI, HUAWEI, OPPO, VIVO, SAMSUNG, ONEPLUS, ASUS, NOKIA, OTHER }
```

OEM matrix (known good as of 2024 — verify before each release):

| OEM | Intent component | Notes |
|---|---|---|
| Xiaomi | `com.miui.securitycenter / .permission.AutoStartManagementActivity` | Required on MIUI/HyperOS |
| Huawei | `com.huawei.systemmanager / .startupmgr.ui.StartupNormalAppListActivity` | Differs by EMUI version |
| Oppo | `com.coloros.safecenter / .permission.startup.StartupAppListActivity` | ColorOS |
| Vivo | `com.iqoo.secure / .ui.phoneoptimize.BgStartUpManager` | Funtouch OS |
| Samsung | `com.samsung.android.lool / .battery.ui.BatteryActivity` | Aggressive on One UI 4+ |
| OnePlus | `com.oneplus.security / .chainlaunch.view.ChainLaunchAppListActivity` | OxygenOS |
| Asus | `com.asus.mobilemanager / .powersaver.PowerSaverSettings` | |
| Nokia | `com.evenwell.powersaving.g3 / .exception.PowerSaverExceptionActivity` | |
| Other / unknown | null — instruct user manually | Plain Android, Pixel: nothing to do |

For each OEM with a known intent, the Settings panel shows a "Aç" button.
For unknown OEMs, show a help dialog with generic instructions.

After the user returns from the OEM screen, ask them in-app: "Did you
enable autostart for MedAlarm?" → store result in DataStore as
`userConfirmedOemAutostart`.

---

## Manifest declarations

```xml
<!-- Notifications (Android 13+) -->
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />

<!-- Exact alarms — alarm-clock-style app -->
<uses-permission android:name="android.permission.USE_EXACT_ALARM" />
<uses-permission
    android:name="android.permission.SCHEDULE_EXACT_ALARM"
    android:maxSdkVersion="32" />

<!-- Battery optimization exemption (optional but recommended) -->
<uses-permission android:name="android.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS" />

<!-- Boot recovery -->
<uses-permission android:name="android.permission.RECEIVE_BOOT_COMPLETED" />

<!-- Vibration -->
<uses-permission android:name="android.permission.VIBRATE" />

<!-- Foreground service (in case TTS or notification post needs it) -->
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE_SPECIAL_USE" />

<!-- IMPORTANT: INTERNET is intentionally absent. CI must enforce this. -->
```

---

## Periodic re-check

`WorkManager` periodic job (`SystemHealthWorker`, every 12 h):

- Runs `SystemHealthChecker`
- If any **critical** check is BLOCKED → post a high-priority
  notification: "MedAlarm bildirim gönderemiyor — düzeltmek için tıkla"
- Tapping the notification opens the System Status screen

This is the safety net: even if a user grants permission then later
revokes it, they'll know within 12 h instead of finding out by
missing a dose.
