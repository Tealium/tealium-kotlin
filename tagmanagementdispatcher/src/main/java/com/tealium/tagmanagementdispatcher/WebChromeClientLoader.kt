package com.tealium.tagmanagementdispatcher

import android.webkit.ConsoleMessage
import android.webkit.WebChromeClient
import com.tealium.core.Logger

class WebChromeClientLoader : WebChromeClient() {
    override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
        consoleMessage?.let { message ->
            val details = "Src: ${message.sourceId()}; Line: ${message.lineNumber()}; ${message.message()}"

            when (message.messageLevel()) {
                ConsoleMessage.MessageLevel.DEBUG -> {
                    Logger.dev(BuildConfig.TAG, details)
                }
                ConsoleMessage.MessageLevel.ERROR -> {
                    Logger.dev(BuildConfig.TAG, details)
                }
                ConsoleMessage.MessageLevel.LOG -> {
                    Logger.qa(BuildConfig.TAG, details)
                }
                ConsoleMessage.MessageLevel.TIP -> {
                    Logger.qa(BuildConfig.TAG, details)
                }
                ConsoleMessage.MessageLevel.WARNING -> {
                    Logger.prod(BuildConfig.TAG, details)
                }
                else -> {
                    Logger.prod(BuildConfig.TAG, details)
                }
            }
        }
        return true
    }
}