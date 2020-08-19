package com.tealium.remotecommanddispatcher.remotecommands

import com.tealium.internal.tagbridge.RemoteCommand
import com.tealium.remotecommanddispatcher.RemoteCommandConfigRetriever

abstract class JsonRemoteCommand(var name: String,
                                 val detail: String?,
                                 val filename: String? = null,
                                 val remoteUrl: String? = null,
                                 var remoteCommandConfigRetriever: RemoteCommandConfigRetriever? = null) : RemoteCommand(name, detail) {
}