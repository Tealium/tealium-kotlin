package com.tealium.tagmanagementdispatcher

import android.webkit.ConsoleMessage
import android.webkit.WebChromeClient
import com.tealium.core.Logger

class WebChromeClientLoader(
    private val webViewLogsEnabled: Boolean = false
) : WebChromeClient() {
    override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
        if (webViewLogsEnabled) {
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
        }
        // Always return true to prevent Android from logging I/chromium to logcat.
        return true
    }
}