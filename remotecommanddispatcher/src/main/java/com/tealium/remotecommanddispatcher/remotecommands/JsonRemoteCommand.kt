package com.tealium.remotecommanddispatcher.remotecommands

import com.tealium.core.TealiumConfig
import com.tealium.remotecommanddispatcher.RemoteCommandConfigRetriever

internal class JsonRemoteCommand(val config: TealiumConfig,
                                 val id: String,
                                 val filename: String? = null,
                                 val remoteUrl: String? = null,
                                 val remoteCommandConfigRetriever: RemoteCommandConfigRetriever? = null) {
}