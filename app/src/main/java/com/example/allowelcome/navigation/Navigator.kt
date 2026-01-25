package com.example.allowelcome.navigation

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri

/**
 * Abstraction for navigation and intent-based actions.
 * 
 * This interface decouples the ViewModel from Android's Context-dependent
 * operations, improving testability and following clean architecture principles.
 * 
 * In production, use [AndroidNavigator] which delegates to the actual Android APIs.
 * In tests, provide a mock implementation to verify behavior without side effects.
 */
interface Navigator {
    /**
     * Opens the SMS app with a pre-filled message.
     * Uses the default SMS app if available, otherwise shows a chooser.
     * @param phoneNumber The recipient's phone number (E.164 format recommended)
     * @param message The message body to pre-fill
     */
    fun openSms(phoneNumber: String, message: String)
    
    /**
     * Opens the system share sheet with the given text content.
     * @param message The text content to share
     * @param chooserTitle The title shown on the share chooser dialog
     */
    fun shareText(message: String, chooserTitle: String = "Share via...")
    
    /**
     * Copies text to the system clipboard.
     * @param label A user-visible label for the clipboard data
     * @param text The text content to copy
     */
    fun copyToClipboard(label: String, text: String)
}

/**
 * Production implementation of [Navigator] that uses Android APIs.
 * 
 * @param context Application context for starting activities and accessing system services.
 *                Using application context avoids memory leaks.
 */
class AndroidNavigator(private val context: Context) : Navigator {
    
    override fun openSms(phoneNumber: String, message: String) {
        val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:$phoneNumber")).apply {
            putExtra("sms_body", message)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        
        // Check if there's a default handler for SMS intents
        val hasDefaultHandler = context.packageManager.resolveActivity(
            intent,
            PackageManager.MATCH_DEFAULT_ONLY
        ) != null
        
        if (hasDefaultHandler) {
            // Launch directly to the default SMS app
            context.startActivity(intent)
        } else {
            // No default handler, show chooser
            context.startActivity(Intent.createChooser(intent, "Send message via...").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            })
        }
    }
    
    override fun shareText(message: String, chooserTitle: String) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, message)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(Intent.createChooser(intent, chooserTitle).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        })
    }
    
    override fun copyToClipboard(label: String, text: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText(label, text))
    }
}
