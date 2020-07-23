package com.tealium.remotecommanddispatcher

class ConfigRemoteCommand : RemoteCommand(NAME, DESCRIPTION) {
    override fun onInvoke(response: Response) {
//         TODO: send traceId? V2?
//        val payload = response.requestPayload
//        val traceId = payload?.optString("trace_id", null)

        response.send()
    }

    companion object {
        val NAME = "_config"
        val DESCRIPTION = "Java callback for mobile.html information."
    }
}