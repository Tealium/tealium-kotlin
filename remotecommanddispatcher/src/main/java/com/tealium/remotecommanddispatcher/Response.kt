package com.tealium.remotecommanddispatcher

import org.json.JSONObject

open class Response(val commandId: String,
                    val responseId: String? = null,
                    val requestPayload: JSONObject? = null,
                    var status: Int = STATUS_OK,
                    var body: String? = null,
                    var evalJavascript: String? = null,
                    var sent: Boolean = false) {

    companion object {
        val STATUS_EXCEPTION_THROWN = 555
        val STATUS_BAD_REQUEST = 400
        val STATUS_NOT_FOUND = 404
        val STATUS_OK = 200
    }

    open fun send() {
        sent = true
    }
}