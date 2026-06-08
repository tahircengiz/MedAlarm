# ============================================================================
# MedAlarm ProGuard / R8 rules
# Conservative — R8 full mode is aggressive; these protect reflective access.
# ============================================================================

# --- Source attributes for crash diagnostics (local logs only) ---
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# --- Kotlin metadata ---
-keep class kotlin.Metadata { *; }

# --- kotlinx.serialization ---
# Keep @Serializable classes' companion objects + the serializer() methods used reflectively.
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.SerializationKt
-keep,includedescriptorclasses class com.medalarm.app.**$$serializer { *; }
-keepclassmembers class com.medalarm.app.** {
    *** Companion;
}
-keepclasseswithmembers class com.medalarm.app.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# --- Room ---
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-dontwarn androidx.room.paging.**

# --- Hilt / Dagger ---
-keep class dagger.hilt.** { *; }
-keep class * extends dagger.hilt.android.internal.managers.ViewComponentManager$FragmentContextWrapper { *; }
-keep,allowobfuscation,allowshrinking class dagger.hilt.android.internal.lifecycle.HiltViewModelFactory$ViewModelFactoriesEntryPoint
-keep,allowobfuscation,allowshrinking @dagger.hilt.android.HiltAndroidApp class *
-keep,allowobfuscation,allowshrinking @dagger.hilt.android.AndroidEntryPoint class *

# --- WorkManager ---
-keep class * extends androidx.work.Worker
-keep class * extends androidx.work.CoroutineWorker
-keep class * extends androidx.work.ListenableWorker

# --- AlarmManager / BroadcastReceivers (referenced via manifest, must survive shrinking) ---
-keep class com.medalarm.app.data.alarm.** extends android.content.BroadcastReceiver { *; }

# --- Compose: don't strip annotations used by tooling ---
-keep class androidx.compose.runtime.** { *; }

# --- Java NIO / java.time (desugared on min SDK 26 — kept defensively) ---
-dontwarn java.lang.invoke.StringConcatFactory
