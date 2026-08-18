package app.axis.coach.audio

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.speech.tts.TextToSpeech
import app.axis.coach.domain.analysis.Cues
import app.axis.coach.domain.model.CueSeverity
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CoachVoice @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private var tts: TextToSpeech? = null
    private var ready = false
    private var lastCue: String? = null
    private var lastSpokenAt = 0L

    private val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= 31) {
        context.getSystemService(VibratorManager::class.java)?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }

    init {
        tts = TextToSpeech(context) { status ->
            ready = status == TextToSpeech.SUCCESS
            if (ready) {
                tts?.language = Locale.US
                tts?.setSpeechRate(0.96f)
                tts?.setPitch(0.92f)
            }
        }
    }

    fun onCue(cue: String?, severity: CueSeverity, nowMs: Long) {
        if (cue.isNullOrBlank() || severity == CueSeverity.NONE) return
        if (cue == Cues.GOOD_REP) return
        if (cue == lastCue && nowMs - lastSpokenAt < 3_200L) return
        if (nowMs - lastSpokenAt < 2_400L) return
        lastCue = cue
        lastSpokenAt = nowMs
        if (ready) {
            tts?.speak(cue, TextToSpeech.QUEUE_FLUSH, null, cue)
        }
        if (severity == CueSeverity.WARN) buzz(48)
    }

    fun onRep() {
        pulse()
    }

    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
        tts = null
        ready = false
    }

    private fun pulse() {
        val vibe = vibrator ?: return
        if (Build.VERSION.SDK_INT >= 29) {
            vibe.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK))
        } else {
            @Suppress("DEPRECATION")
            vibe.vibrate(30)
        }
    }

    private fun buzz(ms: Long) {
        val vibe = vibrator ?: return
        if (Build.VERSION.SDK_INT >= 26) {
            vibe.vibrate(VibrationEffect.createOneShot(ms, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            vibe.vibrate(ms)
        }
    }
}
