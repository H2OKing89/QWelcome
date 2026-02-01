package com.kingpaging.qwelcome.testutil

import com.kingpaging.qwelcome.navigation.Navigator

class FakeNavigator : Navigator {

    data class SmsCall(val phoneNumber: String, val message: String)
    data class ShareCall(val message: String, val chooserTitle: String)
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

    override fun shareText(message: String, chooserTitle: String) {
        shareCalls.add(ShareCall(message, chooserTitle))
    }

    override fun copyToClipboard(label: String, text: String): Boolean {
        copyCalls.add(CopyCall(label, text))
        return clipboardSucceeds
    }
}
