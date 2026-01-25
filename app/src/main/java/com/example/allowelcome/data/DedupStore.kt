package com.example.allowelcome.data

import android.content.Context
import com.example.allowelcome.util.PhoneUtils

class DedupStore(context: Context) {
    private val prefs = context.getSharedPreferences("dedup", Context.MODE_PRIVATE)
    private val squelchMs = 5 * 60 * 1000L // 5 minutes

    fun isDuplicate(phone: String, accountNumber: String): Pair<Boolean, Long?> {
        val key = "${PhoneUtils.normalize(phone)}:$accountNumber"
        val lastSent = prefs.getLong(key, 0L)

        if (lastSent == 0L) return Pair(false, null)

        val elapsed = System.currentTimeMillis() - lastSent
        return if (elapsed < squelchMs) {
            Pair(true, elapsed)
        } else {
            Pair(false, null)
        }
    }

    fun markSent(phone: String, accountNumber: String) {
        val key = "${PhoneUtils.normalize(phone)}:$accountNumber"
        prefs.edit().putLong(key, System.currentTimeMillis()).apply()
    }
}
