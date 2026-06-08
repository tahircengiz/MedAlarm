package com.medalarm.app.permission

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import javax.inject.Inject
import javax.inject.Singleton

enum class Oem {
    XIAOMI, HUAWEI, OPPO, VIVO, SAMSUNG, ONEPLUS, ASUS, NOKIA, OTHER
}

/**
 * OEM-specific deep links for the autostart / background-activity setting.
 * No standard API exists, so this is a known-good matrix per docs/PERMISSIONS.md.
 * The intent components below were verified at the time of writing; treat them as
 * advisory and degrade gracefully when an OEM has changed them.
 */
@Singleton
class OemAutostartHelper @Inject constructor() {

    fun detect(): Oem = when (Build.MANUFACTURER.lowercase()) {
        "xiaomi", "redmi" -> Oem.XIAOMI
        "huawei", "honor" -> Oem.HUAWEI
        "oppo", "realme" -> Oem.OPPO
        "vivo" -> Oem.VIVO
        "samsung" -> Oem.SAMSUNG
        "oneplus" -> Oem.ONEPLUS
        "asus" -> Oem.ASUS
        "nokia", "hmd global" -> Oem.NOKIA
        else -> Oem.OTHER
    }

    /** Whether this OEM is known to aggressively kill background apps. */
    fun isAggressive(oem: Oem = detect()): Boolean = when (oem) {
        Oem.XIAOMI, Oem.HUAWEI, Oem.OPPO, Oem.VIVO,
        Oem.SAMSUNG, Oem.ONEPLUS, Oem.ASUS, Oem.NOKIA -> true
        Oem.OTHER -> false
    }

    /**
     * Returns a launchable Intent for the OEM's autostart/background screen, or
     * null if no known intent for this OEM (caller falls back to a help dialog).
     */
    fun autostartIntent(context: Context, oem: Oem = detect()): Intent? {
        val candidates: List<ComponentName> = when (oem) {
            Oem.XIAOMI -> listOf(
                ComponentName("com.miui.securitycenter", "com.miui.permcenter.autostart.AutoStartManagementActivity")
            )
            Oem.HUAWEI -> listOf(
                ComponentName("com.huawei.systemmanager", "com.huawei.systemmanager.startupmgr.ui.StartupNormalAppListActivity"),
                ComponentName("com.huawei.systemmanager", "com.huawei.systemmanager.optimize.process.ProtectActivity")
            )
            Oem.OPPO -> listOf(
                ComponentName("com.coloros.safecenter", "com.coloros.safecenter.permission.startup.StartupAppListActivity"),
                ComponentName("com.oppo.safe", "com.oppo.safe.permission.startup.StartupAppListActivity")
            )
            Oem.VIVO -> listOf(
                ComponentName("com.iqoo.secure", "com.iqoo.secure.ui.phoneoptimize.BgStartUpManager"),
                ComponentName("com.vivo.permissionmanager", "com.vivo.permissionmanager.activity.BgStartUpManagerActivity")
            )
            Oem.SAMSUNG -> listOf(
                ComponentName("com.samsung.android.lool", "com.samsung.android.sm.battery.ui.BatteryActivity")
            )
            Oem.ONEPLUS -> listOf(
                ComponentName("com.oneplus.security", "com.oneplus.security.chainlaunch.view.ChainLaunchAppListActivity")
            )
            Oem.ASUS -> listOf(
                ComponentName("com.asus.mobilemanager", "com.asus.mobilemanager.powersaver.PowerSaverSettings")
            )
            Oem.NOKIA -> listOf(
                ComponentName("com.evenwell.powersaving.g3", "com.evenwell.powersaving.g3.exception.PowerSaverExceptionActivity")
            )
            Oem.OTHER -> emptyList()
        }

        val pm = context.packageManager
        return candidates.firstNotNullOfOrNull { component ->
            val intent = Intent().apply {
                this.component = component
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            if (intent.resolveActivity(pm) != null) intent else null
        }
    }
}
