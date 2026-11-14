package com.example.autoreply

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.telecom.TelecomManager
import android.telephony.PhoneStateListener
import android.telephony.TelephonyCallback
import android.telephony.TelephonyManager
import android.util.Log
import androidx.annotation.RequiresApi
import java.util.Locale

// Автоответ на звонки: через N секунд берём трубку, проигрываем приветствие
// (своя запись или голос синтезатора) в динамик и сбрасываем.
// Работает на Android 9+ (acceptRingingCall / endCall).
object CallHandler {
    private const val TAG = "AutoReplyCall"
    private const val MAX_CALL_MS = 60_000L
    private const val LINE_SETTLE_MS = 1_000L
    private const val HANGUP_PAUSE_MS = 1_500L

    private val main = Handler(Looper.getMainLooper())
    private lateinit var app: Context

    private var started = false
    private var lastState = TelephonyManager.CALL_STATE_IDLE
    private var answering = false   // отправили acceptRingingCall, ждём OFFHOOK
    private var handling = false    // трубку взяли мы
    private var finishing = false
    private var pendingAnswer: Runnable? = null
    private var tts: TextToSpeech? = null
    private var player: MediaPlayer? = null
    private var speakerWasOn = false

    fun supported() = Build.VERSION.SDK_INT >= 28

    fun isListening() = started

    // короткий журнал последних событий, виден в настройках
    private fun log(msg: String) {
        try {
            val p = Prefs(app)
            val line = "${Prefs.fmtTime(Prefs.nowMinutes())} $msg"
            p.callDebug = (listOf(line) + p.callDebug.split("\n").filter { it.isNotBlank() })
                .take(8).joinToString("\n")
        } catch (e: Exception) {
            Log.e(TAG, "log failed", e)
        }
    }

    fun hasPermissions(ctx: Context) =
        ctx.checkSelfPermission(Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED &&
            ctx.checkSelfPermission(Manifest.permission.ANSWER_PHONE_CALLS) == PackageManager.PERMISSION_GRANTED

    // безопасно вызывать много раз; вызывать из основного потока
    fun start(ctx: Context) {
        if (started || !supported() || !hasPermissions(ctx)) return
        app = ctx.applicationContext
        val tm = app.getSystemService(TelephonyManager::class.java) ?: return
        try {
            if (Build.VERSION.SDK_INT >= 31) {
                tm.registerTelephonyCallback(app.mainExecutor, Callback())
            } else {
                @Suppress("DEPRECATION")
                tm.listen(object : PhoneStateListener() {
                    @Deprecated("Deprecated in Java")
                    override fun onCallStateChanged(state: Int, phoneNumber: String?) {
                        onState(state)
                    }
                }, PhoneStateListener.LISTEN_CALL_STATE)
            }
            started = true
            log("слушаю состояние телефона")
        } catch (e: Exception) {
            Log.e(TAG, "listen failed", e)
            log("не удалось начать слушать: ${e.message}")
        }
    }

    @RequiresApi(31)
    private class Callback : TelephonyCallback(), TelephonyCallback.CallStateListener {
        override fun onCallStateChanged(state: Int) = onState(state)
    }

    private fun onState(state: Int) {
        val prev = lastState
        lastState = state
        log(
            when (state) {
                TelephonyManager.CALL_STATE_RINGING -> "телефон звонит"
                TelephonyManager.CALL_STATE_OFFHOOK -> "трубка снята"
                else -> "звонок завершён"
            }
        )
        when (state) {
            TelephonyManager.CALL_STATE_RINGING -> if (prev == TelephonyManager.CALL_STATE_IDLE) scheduleAnswer()
            TelephonyManager.CALL_STATE_OFFHOOK -> {
                if (answering) {
                    answering = false
                    handling = true
                    main.postDelayed({ play() }, LINE_SETTLE_MS)
                    main.postDelayed({ hangUp() }, MAX_CALL_MS)
                } else {
                    cancelPending() // сам взял трубку
                }
            }
            TelephonyManager.CALL_STATE_IDLE -> cleanup()
        }
    }

    private fun scheduleAnswer() {
        val prefs = Prefs(app)
        if (!prefs.enabled) {
            log("пропуск: AutoReply выключен")
            return
        }
        if (!prefs.callAnswer) {
            log("пропуск: «Отвечать на звонки» выключено")
            return
        }
        log("возьму трубку через ${prefs.callDelaySec} с")
        cancelPending()
        val r = Runnable {
            pendingAnswer = null
            answer()
        }
        pendingAnswer = r
        main.postDelayed(r, prefs.callDelaySec * 1000L)
    }

    private fun cancelPending() {
        pendingAnswer?.let { main.removeCallbacks(it) }
        pendingAnswer = null
    }

    private fun answer() {
        if (lastState != TelephonyManager.CALL_STATE_RINGING) {
            log("звонок уже не звонит, трубку не беру")
            return
        }
        if (!hasPermissions(app)) {
            log("нет разрешений на звонки")
            return
        }
        try {
            log("беру трубку")
            answering = true
            app.getSystemService(TelecomManager::class.java).acceptRingingCall()
        } catch (e: Exception) {
            answering = false
            Log.e(TAG, "answer failed", e)
            log("не удалось взять трубку: ${e.message}")
        }
    }

    private fun play() {
        if (!handling) return
        val prefs = Prefs(app)
        log("играю приветствие")
        prefs.count = prefs.count + 1
        prefs.addJournal("Звонок", "входящий")
        Notifier.update(app)
        AutoReplyWidget.refresh(app)

        val am = app.getSystemService(AudioManager::class.java)
        try {
            speakerWasOn = am.isSpeakerphoneOn
            @Suppress("DEPRECATION")
            am.isSpeakerphoneOn = true
            am.setStreamVolume(AudioManager.STREAM_VOICE_CALL, am.getStreamMaxVolume(AudioManager.STREAM_VOICE_CALL), 0)
        } catch (e: Exception) {
            Log.e(TAG, "speaker failed", e)
        }

        val uri = prefs.callAudioUri
        if (uri != null && playFile(uri)) return
        speak(prefs.callText)
    }

    private fun attrs() = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION)
        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
        .build()

    private fun playFile(uri: String): Boolean {
        return try {
            val mp = MediaPlayer()
            player = mp
            mp.setAudioAttributes(attrs())
            mp.setDataSource(app, Uri.parse(uri))
            mp.setOnCompletionListener { done() }
            mp.setOnErrorListener { _, _, _ ->
                speak(Prefs(app).callText)
                true
            }
            mp.prepare()
            mp.start()
            true
        } catch (e: Exception) {
            Log.e(TAG, "file play failed", e)
            player?.release()
            player = null
            false
        }
    }

    private fun speak(text: String) {
        tts = TextToSpeech(app) { status ->
            val engine = tts
            if (status != TextToSpeech.SUCCESS || engine == null) {
                log("синтезатор речи не запустился, сбрасываю")
                done()
                return@TextToSpeech
            }
            engine.language = Locale("ru")
            engine.setAudioAttributes(attrs())
            engine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {}
                override fun onDone(utteranceId: String?) { main.post { done() } }
                @Deprecated("Deprecated in Java")
                override fun onError(utteranceId: String?) { main.post { done() } }
            })
            engine.speak(text, TextToSpeech.QUEUE_FLUSH, null, "greeting")
        }
    }

    private fun done() {
        if (finishing) return
        finishing = true
        main.postDelayed({ hangUp() }, HANGUP_PAUSE_MS)
    }

    private fun hangUp() {
        if (!handling) return
        try {
            app.getSystemService(TelecomManager::class.java).endCall()
        } catch (e: Exception) {
            Log.e(TAG, "hangup failed", e)
        }
        cleanup()
    }

    private fun cleanup() {
        main.removeCallbacksAndMessages(null)
        pendingAnswer = null
        player?.release()
        player = null
        tts?.shutdown()
        tts = null
        if (handling) {
            try {
                val am = app.getSystemService(AudioManager::class.java)
                @Suppress("DEPRECATION")
                am.isSpeakerphoneOn = speakerWasOn
            } catch (e: Exception) {
                Log.e(TAG, "speaker restore failed", e)
            }
        }
        answering = false
        handling = false
        finishing = false
    }
}
