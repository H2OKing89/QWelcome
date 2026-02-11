package com.kingpaging.qwelcome.navigation

import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.kingpaging.qwelcome.data.SettingsStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

private const val TAG = "ShareTargetReceiver"

class ShareTargetChosenReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val componentName = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(Intent.EXTRA_CHOSEN_COMPONENT, ComponentName::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra(Intent.EXTRA_CHOSEN_COMPONENT)
        }

        val packageName = componentName?.packageName?.takeIf { it.isNotBlank() } ?: return

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                SettingsStore(context.applicationContext).recordRecentSharePackage(packageName)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to persist recent share package=$packageName", e)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
