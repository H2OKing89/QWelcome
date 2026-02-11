package com.kingpaging.qwelcome.testutil

import com.kingpaging.qwelcome.navigation.Navigator

class FakeNavigator : Navigator {

    data class SmsCall(val phoneNumber: String, val message: String)
    data class ShareCall(val packageName: String?, val message: String, val chooserTitle: String, val subject: String?)
    data class CopyCall(val label: String, val text: String)

    val smsCalls = mutableListOf<SmsCall>()
    val shareCalls = mutableListOf<ShareCall>()
    val copyCalls = mutableListOf<CopyCall>()

    /**
     * Controls whether copyToClipboard succeeds or fails.
     * Set to false to test clipboard failure handling.
     */
    var clipboardSucceeds: Boolean = true

    override fun openSms(phoneNumber: String, message: String) {
        smsCalls.add(SmsCall(phoneNumber, message))
    }

    override fun shareText(message: String, chooserTitle: String, subject: String?) {
        shareCalls.add(ShareCall(packageName = null, message = message, chooserTitle = chooserTitle, subject = subject))
    }

    override fun shareToApp(packageName: String, message: String, subject: String?, chooserTitle: String) {
        shareCalls.add(
            ShareCall(
                packageName = packageName,
                message = message,
                chooserTitle = chooserTitle,
                subject = subject
            )
        )
    }

    override fun copyToClipboard(label: String, text: String): Boolean {
        copyCalls.add(CopyCall(label, text))
        return clipboardSucceeds
    }
}
