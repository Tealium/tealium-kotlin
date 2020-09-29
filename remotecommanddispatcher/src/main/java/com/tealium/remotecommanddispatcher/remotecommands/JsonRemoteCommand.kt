package com.tealium.remotecommanddispatcher.remotecommands

import com.tealium.core.TealiumConfig
import com.tealium.remotecommanddispatcher.RemoteCommandConfigRetriever
import com.tealium.remotecommanddispatcher.RemoteCommandConfigRetrieverFactory

internal class JsonRemoteCommand(val config: TealiumConfig,
                                 val id: String,
                                 val filename: String? = null,
                                 val remoteUrl: String? = null,
                                 val remoteCommandConfigRetriever: RemoteCommandConfigRetriever? = null) {
}