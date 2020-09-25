package com.tealium.remotecommanddispatcher.remotecommands

import com.tealium.remotecommanddispatcher.RemoteCommandConfigRetriever

internal class JsonRemoteCommand(val id: String,
                        val filename: String? = null,
                        val remoteUrl: String? = null,
                        var remoteCommandConfigRetriever: RemoteCommandConfigRetriever? = null) {
}