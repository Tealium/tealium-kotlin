package com.tealium.core.messaging

import com.tealium.remotecommands.RemoteCommandRequest

interface AfterDispatchSendCallbacks {
    fun sendRemoteCommand(request: RemoteCommandRequest)
    fun onEvaluateJavascript(js: String)
}

class DispatchSendCallbacks(private val eventRouter: EventRouter) : AfterDispatchSendCallbacks {

    override fun sendRemoteCommand(request: RemoteCommandRequest) {
        eventRouter.onRemoteCommandSend(request)
    }

    override fun onEvaluateJavascript(js: String) {
        eventRouter.onEvaluateJavascript(js)
    }
}