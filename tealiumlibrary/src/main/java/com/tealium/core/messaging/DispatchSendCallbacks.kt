package com.tealium.core.messaging

import com.tealium.remotecommands.RemoteCommand

interface AfterDispatchSendCallbacks {
    fun sendRemoteCommand(handler: RemoteCommand.ResponseHandler, url: String)
    fun onEvaluateJavascript(js: String)
}

class DispatchSendCallbacks(private val eventRouter: EventRouter) : AfterDispatchSendCallbacks {

    override fun sendRemoteCommand(handler: RemoteCommand.ResponseHandler, url: String) {
        eventRouter.onRemoteCommandSend(handler, url)
    }

    override fun onEvaluateJavascript(js: String) {
        eventRouter.onEvaluateJavascript(js)
    }
}