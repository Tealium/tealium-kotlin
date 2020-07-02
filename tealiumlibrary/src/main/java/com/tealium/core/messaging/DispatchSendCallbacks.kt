package com.tealium.core.messaging

interface AfterDispatchSendCallbacks {
    fun sendRemoteCommand(url: String)
    fun onEvaluateJavascript(js: String)
}

class DispatchSendCallbacks(private val eventRouter: EventRouter) : AfterDispatchSendCallbacks {

    override fun sendRemoteCommand(url: String) {
        eventRouter.onRemoteCommandSend(url)
    }

    override fun onEvaluateJavascript(js: String) {
        eventRouter.onEvaluateJavascript(js)
    }
}