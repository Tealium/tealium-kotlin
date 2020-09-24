package com.tealium.remotecommanddispatcher.remotecommands

import com.tealium.remotecommanddispatcher.RemoteCommandConfigRetriever

//import com.tealium.remotecommands.RemoteCommand

class JsonRemoteCommand(val id: String,
                        val detail: String?,
                        val filename: String? = null,
                        val remoteUrl: String? = null,
                        var remoteCommandConfigRetriever: RemoteCommandConfigRetriever? = null) {
}