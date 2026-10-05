package com.example.autoreply

import android.app.Notification
import android.app.PendingIntent
import android.app.RemoteInput
import android.content.Intent
import android.os.Bundle
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log

class AutoReplyService : NotificationListenerService() {

    private val lastReply = HashMap<String, Long>()

    override fun onListenerConnected() {
        Notifier.update(this)
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val prefs = Prefs(this)
        if (!prefs.enabled) return
        val app = Apps.byPackage(sbn.packageName) ?: return
        if (!prefs.isAppOn(app.title)) return

        val n = sbn.notification
        if (n.flags and Notification.FLAG_GROUP_SUMMARY != 0) return

        val extras = n.extras
        // группы пропускаем
        if (extras.getBoolean(Notification.EXTRA_IS_GROUP_CONVERSATION, false)) return
        if (extras.getCharSequence(Notification.EXTRA_CONVERSATION_TITLE) != null) return

        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString() ?: return
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()
        // не отвечаем на собственный автоответ
        if (text != null && text.contains(prefs.replyText)) return

        val action = findReplyAction(n) ?: return

        val key = "${sbn.packageName}:$title"
        val now = System.currentTimeMillis()
        val last = lastReply[key] ?: 0L
        if (now - last < prefs.cooldownMin * 60_000L) return
        lastReply[key] = now

        if (sendReply(action, prefs.replyText)) {
            prefs.count = prefs.count + 1
            prefs.addJournal(app.title, title)
            Notifier.update(this)
        }
    }

    private fun findReplyAction(n: Notification): Notification.Action? {
        val actions = n.actions ?: return null
        val withInput = actions.filter { !it.remoteInputs.isNullOrEmpty() }
        return withInput.firstOrNull { it.semanticAction == Notification.Action.SEMANTIC_ACTION_REPLY }
            ?: withInput.firstOrNull()
    }

    private fun sendReply(action: Notification.Action, text: String): Boolean {
        return try {
            val inputs = action.remoteInputs
            val intent = Intent()
            val bundle = Bundle()
            for (input in inputs) {
                bundle.putCharSequence(input.resultKey, text)
            }
            RemoteInput.addResultsToIntent(inputs, intent, bundle)
            action.actionIntent.send(this, 0, intent)
            true
        } catch (e: PendingIntent.CanceledException) {
            Log.e(TAG, "reply failed", e)
            false
        }
    }

    companion object {
        private const val TAG = "AutoReply"
    }
}
