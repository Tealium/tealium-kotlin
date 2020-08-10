package com.tealium.remotecommanddispatcher

import com.tealium.remotecommanddispatcher.remotecommands.RemoteCommand
import junit.framework.Assert
import org.json.JSONException
import org.json.JSONObject
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.lang.IllegalArgumentException
import java.util.*

@RunWith(RobolectricTestRunner::class)
class RemoteCommandRequestTest {

    @Test
    fun webViewRemoteCommandInvalidCorrupted() {
        try {
            val request = RemoteCommandRequest.tagManagementRequest("tealium://command?request=%7B%22foo%22%3A%22%7D")
            Assert.fail()
            Assert.assertNull(request)
        } catch (ex: JSONException) {
        }
    }

    @Test
    fun webViewRemoteCommandInvalidSymbol() {
        try {
            val request = RemoteCommandRequest.tagManagementRequest("tealium://command?request=%7B%22foo%22%3A%22bar%22%!!")
            Assert.fail()
            Assert.assertNull(request)
        } catch (ex: IllegalArgumentException) {
        }
    }

    @Test
    fun webViewRemoteCommandInvalidCommandName() {
        val request = RemoteCommandRequest.tagManagementRequest("tealium://commandpayload=%7B%7D")

        Assert.assertNull(request)
    }

    @Test
    fun webViewRemoteCommandValidRequest() {
        val request = RemoteCommandRequest.tagManagementRequest("tealium://command?request=%7B%22foo%22%3A%22bar%22%7D")

        Assert.assertEquals("command", request?.commandId)
        Assert.assertEquals(0, request?.response?.requestPayload?.length())
    }

    @Test
    fun webViewRemoteCommandValidRequestWithPayload() {
        val request = RemoteCommandRequest.tagManagementRequest("tealium://task-populated_arg?request=%7B%22payload%22%3A%7B%22foo%22%3A%22bar%22%7D%7D")

        Assert.assertEquals("task-populated_arg", request?.commandId)
        Assert.assertEquals(1, request?.response?.requestPayload?.length())
        Assert.assertNull(request?.response?.responseId)
    }

    @Test
    fun webViewRemoteCommandValidRequestWithId() {
        val request = RemoteCommandRequest.tagManagementRequest("tealium://command?request=%7B%22config%22%3A%7B%22response_id%22%3A%7B%7D%7D%7D")

        Assert.assertEquals("command", request?.commandId)
        Assert.assertEquals(0, request?.response?.requestPayload?.length())
        Assert.assertNotNull(request?.response?.responseId)
    }


    @Test
    fun webViewRemoteCommandValidRequestWithResponseJsonArrayIdAsString() {
        val request = RemoteCommandRequest.tagManagementRequest("tealium://command?request=%7B%22config%22%3A%7B%22response_id%22%3A%5B%5D%7D%7D")

        Assert.assertEquals("command", request?.commandId)
        Assert.assertEquals(0, request?.response?.requestPayload?.length())
        Assert.assertNotNull(request?.response?.responseId)
    }

    @Test
    fun webViewRemoteCommandValidRequestWithResponseNumberIdAsString() {
        val request = RemoteCommandRequest.tagManagementRequest("tealium://command?request=%7B%22config%22%3A%7B%22response_id%22%3A1234%7D%7D")

        Assert.assertEquals("command", request?.commandId)
        Assert.assertEquals(0, request?.response?.requestPayload?.length())
        Assert.assertNotNull(request?.response?.responseId)
    }

    @Test
    fun jsonRemoteCommandValidRequest() {
        val command = object : RemoteCommand("jsonTest", "Testing json requests", RemoteCommandType.JSON, filename = "abc1234.json") {
            override fun onInvoke(response: Response) { // do nothing
            }
        }
        val request = RemoteCommandRequest.jsonRequest(command, JSONObject())

        Assert.assertEquals("jsonTest".toLowerCase(Locale.ROOT), request.commandId)
        Assert.assertNotNull(request.payload)
        Assert.assertNotNull(request.response)
    }

    @Test
    fun jsonRemoteCommandInvalidName() {
        val command = object : RemoteCommand("testCommand", "Testing json requests", RemoteCommandType.JSON, filename = "abc1234.json") {
            override fun onInvoke(response: Response) { // do nothing
            }
        }
        val request = RemoteCommandRequest.jsonRequest(command, JSONObject())

        Assert.assertEquals("testCommand".toLowerCase(Locale.ROOT), request.commandId)
        Assert.assertNotNull(request.payload)
        Assert.assertNotNull(request.response)
    }

    @Test
    fun jsonRemoteCommand() {
        val command = object : RemoteCommand("testCommand", "Testing json requests", RemoteCommandType.JSON, filename = "abc1234.json") {
            override fun onInvoke(response: Response) { // do nothing
            }
        }
        val request = RemoteCommandRequest.jsonRequest(command, JSONObject())

        Assert.assertEquals("testCommand".toLowerCase(Locale.ROOT), request.commandId)
        Assert.assertNotNull(request.payload)
        Assert.assertNotNull(request.response)
    }
}