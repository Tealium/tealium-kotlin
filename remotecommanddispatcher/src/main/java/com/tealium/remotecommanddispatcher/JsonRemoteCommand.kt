package com.tealium.remotecommanddispatcher

abstract class JsonRemoteCommand(var id: String,
                        descriptor: String? = null,
                        val filename: String? = null,
                        val remoteUrl: String? = null,
                        var remoteCommandConfigRetriever: RemoteCommandConfigRetriever? = null) : RemoteCommand(id, descriptor) {
}