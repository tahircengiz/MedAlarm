package com.medalarm.app.tts

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.suspendCancellableCoroutine
import timber.log.Timber
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

/**
 * Thin wrapper over Android's built-in [TextToSpeech]. Used by the alarm
 * receiver to optionally speak the medication name aloud.
 *
 * Lifecycle:
 * - Lazily initialized on first speak() call.
 * - Stays alive as a Singleton; shutdown() is exposed for tests, but the OS
 *   reclaims the engine when our process is killed.
 *
 * Offline: all major locales (TR + EN) are cached on-device by default; no
 * network is used. This is critical to honour the bloatware-free manifesto.
 */
@Singleton
class TtsHelper @Inject constructor(
    @ApplicationContext private val context: Context
) {

    @Volatile private var tts: TextToSpeech? = null
    @Volatile private var ready: Boolean = false

    private suspend fun ensureReady(): Boolean {
        if (ready && tts != null) return true
        return suspendCancellableCoroutine { cont ->
            tts = TextToSpeech(context) { status ->
                if (status == TextToSpeech.SUCCESS) {
                    val locale = Locale.getDefault()
                    val res = tts?.setLanguage(locale)
                    if (res == TextToSpeech.LANG_MISSING_DATA || res == TextToSpeech.LANG_NOT_SUPPORTED) {
                        tts?.setLanguage(Locale.ENGLISH)
                    }
                    ready = true
                    cont.resume(true)
                } else {
                    Timber.w("TextToSpeech init failed: status=$status")
                    cont.resume(false)
                }
            }
        }
    }

    /**
     * Speak [text] and suspend until playback finishes (or fails). Safe to call
     * from the alarm receiver's goAsync() block.
     */
    suspend fun speak(text: String, utteranceId: String) {
        if (text.isBlank()) return
        if (!ensureReady()) return

        suspendCancellableCoroutine<Unit> { cont ->
            val engine = tts ?: run {
                cont.resume(Unit); return@suspendCancellableCoroutine
            }
            engine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(id: String?) = Unit
                override fun onDone(id: String?) { if (cont.isActive) cont.resume(Unit) }
                @Deprecated("Deprecated in Java")
                override fun onError(id: String?) { if (cont.isActive) cont.resume(Unit) }
                override fun onError(id: String?, errorCode: Int) {
                    if (cont.isActive) cont.resume(Unit)
                }
            })
            val result = engine.speak(text, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
            if (result != TextToSpeech.SUCCESS && cont.isActive) cont.resume(Unit)
        }
    }

    fun shutdown() {
        runCatching {
            tts?.stop()
            tts?.shutdown()
        }
        tts = null
        ready = false
    }
}
