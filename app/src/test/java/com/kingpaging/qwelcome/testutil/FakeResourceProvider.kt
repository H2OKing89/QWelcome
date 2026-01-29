package com.kingpaging.qwelcome.testutil

import com.kingpaging.qwelcome.util.ResourceProvider

class FakeResourceProvider : ResourceProvider {

    override fun getString(resId: Int): String {
        return "string_$resId"
    }

    override fun getString(resId: Int, vararg formatArgs: Any): String {
        val base = "string_$resId"
        return if (formatArgs.isNotEmpty()) {
            "$base[${formatArgs.joinToString(",")}]"
        } else {
            base
        }
    }
}
