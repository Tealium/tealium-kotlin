package com.tealium.remotecommanddispatcher

class ConfigRemoteCommand : RemoteCommand(NAME, DESCRIPTION) {
    override fun onInvoke(response: Response) {
        val payload = response.requestPayload
        val traceId = payload?.optString("trace_id", null)
        // send traceId

        response.send()
    }

    companion object {
        val NAME = "_config"
        val DESCRIPTION = "Java callback for mobile.html information."
    }
}