package com.tealium.remotecommanddispatcher

import org.json.JSONObject

/**
 * The object used to request a remote command to the remote command dispatcher.
 * A response object must be set in order for the command to be invoked.
 */
class RemoteCommandRequest(val commandId: String) {

    constructor(remoteCommand: RemoteCommand) : this(remoteCommand.commandId)

    var payload: JSONObject? = null // comes from tiq or payload of track call when using json rc
    var response: Response? = null
}