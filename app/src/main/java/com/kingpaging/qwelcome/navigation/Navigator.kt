package com.kingpaging.qwelcome.navigation

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.app.PendingIntent
import android.content.pm.PackageManager
import android.net.Uri
import android.util.Log
import android.widget.Toast
import com.kingpaging.qwelcome.R

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
     * @param subject Optional share subject for apps that support it
     */
    fun shareText(
        message: String,
        chooserTitle: String = "Share via...",
        subject: String? = null
    )

    /**
     * Shares text directly to a specific package if available.
     * Falls back to generic chooser when the package is unavailable.
     */
    fun shareToApp(
        packageName: String,
        message: String,
        subject: String? = null,
        chooserTitle: String = "Share via..."
    )
    
    /**
     * Copies text to the system clipboard.
     * @param label A user-visible label for the clipboard data
     * @param text The text content to copy
     * @return true if the operation succeeded, false if it failed
     */
    fun copyToClipboard(label: String, text: String): Boolean
}

private const val TAG = "AndroidNavigator"

/**
 * Production implementation of [Navigator] that uses Android APIs.
 *
 * @param context Application context for starting activities and accessing system services.
 *                Using application context avoids memory leaks.
 */
class AndroidNavigator(private val context: Context) : Navigator {

    override fun openSms(phoneNumber: String, message: String) {
        val smsUri = Uri.fromParts("smsto", phoneNumber, null)
        val intent = Intent(Intent.ACTION_SENDTO, smsUri).apply {
            putExtra("sms_body", message)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        try {
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
                context.startActivity(Intent.createChooser(intent, context.getString(R.string.chooser_send_message)).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                })
            }
        } catch (e: ActivityNotFoundException) {
            Log.e(TAG, "No SMS app available", e)
            Toast.makeText(context, R.string.toast_no_messaging_app, Toast.LENGTH_SHORT).show()
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException opening SMS app", e)
            Toast.makeText(context, R.string.toast_unable_open_messaging, Toast.LENGTH_SHORT).show()
        } catch (e: IllegalArgumentException) {
            Log.e(TAG, "IllegalArgumentException opening SMS app", e)
            Toast.makeText(context, R.string.toast_unable_open_messaging, Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to open SMS app: ${e::class.java.simpleName}", e)
            Toast.makeText(context, R.string.toast_unable_open_messaging, Toast.LENGTH_SHORT).show()
        }
    }

    override fun shareText(message: String, chooserTitle: String, subject: String?) {
        val chooserCallback = PendingIntent.getBroadcast(
            context,
            1001,
            Intent(context, ShareTargetChosenReceiver::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, message)
            if (!subject.isNullOrBlank()) {
                putExtra(Intent.EXTRA_SUBJECT, subject)
            }
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        val chooserIntent = Intent.createChooser(intent, chooserTitle, chooserCallback.intentSender).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        try {
            context.startActivity(chooserIntent)
        } catch (e: ActivityNotFoundException) {
            Log.e(TAG, "No share target available", e)
            Toast.makeText(context, R.string.toast_no_share_app, Toast.LENGTH_SHORT).show()
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException opening share sheet", e)
            Toast.makeText(context, R.string.toast_unable_share, Toast.LENGTH_SHORT).show()
        } catch (e: IllegalArgumentException) {
            Log.e(TAG, "IllegalArgumentException opening share sheet", e)
            Toast.makeText(context, R.string.toast_unable_share, Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to open share sheet: ${e::class.java.simpleName}", e)
            Toast.makeText(context, R.string.toast_unable_share, Toast.LENGTH_SHORT).show()
        }
    }

    override fun shareToApp(
        packageName: String,
        message: String,
        subject: String?,
        chooserTitle: String
    ) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, message)
            if (!subject.isNullOrBlank()) {
                putExtra(Intent.EXTRA_SUBJECT, subject)
            }
            setPackage(packageName)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            val canHandle = context.packageManager.resolveActivity(intent, 0) != null
            if (canHandle) {
                context.startActivity(intent)
            } else {
                shareText(message, chooserTitle, subject)
            }
        } catch (e: ActivityNotFoundException) {
            Log.e(TAG, "No share target available for package=$packageName", e)
            shareText(message, chooserTitle, subject)
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException sharing to package=$packageName", e)
            shareText(message, chooserTitle, subject)
        } catch (e: IllegalArgumentException) {
            Log.e(TAG, "IllegalArgumentException sharing to package=$packageName", e)
            shareText(message, chooserTitle, subject)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to share to package=$packageName: ${e::class.java.simpleName}", e)
            shareText(message, chooserTitle, subject)
        }
    }

    override fun copyToClipboard(label: String, text: String): Boolean {
        return try {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
            if (clipboard == null) {
                Log.e(TAG, "Failed to get ClipboardManager")
                return false
            }
            clipboard.setPrimaryClip(ClipData.newPlainText(label, text))
            true
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException accessing clipboard", e)
            false
        } catch (e: Exception) {
            Log.e(TAG, "Failed to copy to clipboard: ${e::class.java.simpleName}", e)
            false
        }
    }
}
