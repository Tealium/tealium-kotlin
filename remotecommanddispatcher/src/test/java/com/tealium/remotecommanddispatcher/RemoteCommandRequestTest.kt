package com.tealium.remotecommanddispatcher

import junit.framework.Assert
import org.junit.Test

class RemoteCommandRequestTest {

    @Test
    fun webViewRemoteCommandInvalidCorrupted() {
        val request = RemoteCommandRequest.tagManagementRequest("tealium://command?request=%7B%22foo%22%3A%22%7D")

        Assert.assertNull(request.response)
        Assert.assertNull(request.commandId)
        Assert.assertNull(request.payload)
    }

    @Test
    fun webViewRemoteCommandInvalidSymbol() {
        val request = RemoteCommandRequest.tagManagementRequest("tealium://command?request=%7B%22foo%22%3A%22bar%22%!!")

        Assert.assertNull(request.response)
        Assert.assertNull(request.commandId)
        Assert.assertNull(request.payload)
    }

    @Test
    fun webViewRemoteCommandInvalidCommandName() {
        val request = RemoteCommandRequest.tagManagementRequest("tealium://commandpayload=%7B%7D")

        Assert.assertNull(request.response)
        Assert.assertNull(request.commandId)
        Assert.assertNull(request.payload)
    }

    @Test
    fun webViewRemoteCommandValidRequest() {
        val request = RemoteCommandRequest.tagManagementRequest("tealium://command?request=%7B%22foo%22%3A%22bar%22%7D")
//        val request = RemoteCommandRequest.tagManagementRequest("tealium://task-populated_arg?request=%7B%22foo%22%3A%22bar%22%7D")

        Assert.assertEquals("command", request.commandId)
        Assert.assertNull(request.response?.requestPayload)
    }
}