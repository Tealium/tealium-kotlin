package com.tealium.remotecommanddispatcher

import com.tealium.remotecommanddispatcher.remotecommands.HttpRemoteCommand.Formatter
import com.tealium.remotecommanddispatcher.remotecommands.HttpRemoteCommand
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.nio.charset.Charset

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [21, 35])
class FormatterTests {

    val utf8: Charset = Charset.forName("utf-8")
    val testJsonObject: JSONObject = JSONObject(mapOf(
            "string" to "value",
            "int" to 10,
            "double" to 10.5,
            "boolean" to true,
            "map" to mapOf("key" to "value"),
            "list" to listOf("value_1", "value_2"),
            "array" to arrayOf("value_1", "value_2")))
    val testString: String = "my_payload"
    val testObject: Any = object : Any() {
        override fun toString(): String {
            return testString
        }
    }

    @Test
    fun formatters_Default_FormatsJson() {
        val formatter: Formatter = HttpRemoteCommand.Formatters.formatterFor("application/json")
        val formatted = formatter.format(testJsonObject, utf8)!!

        val jsonString = formatted.toString(utf8)
        assertTrue(jsonString.startsWith("{"))
        assertTrue(jsonString.endsWith("}"))
        assertTrue(jsonString.contains("\"string\":\"value\""))
        assertTrue(jsonString.contains("\"int\":10"))
        assertTrue(jsonString.contains("\"double\":10.5"))
        assertTrue(jsonString.contains("\"boolean\":true"))
        assertTrue(jsonString.contains("\"map\":{\"key\":\"value\"}"))
        assertTrue(jsonString.contains("\"list\":[\"value_1\",\"value_2\"]"))
        assertTrue(jsonString.contains("\"array\":[\"value_1\",\"value_2\"]"))
        assertNotNull(JSONObject(jsonString))
    }

    @Test
    fun formatters_Default_FormatsString() {
        val formatter: Formatter = HttpRemoteCommand.Formatters.formatterFor("application/json")
        val formatted = formatter.format(testString, utf8)!!

        assertEquals(testString, formatted.toString(utf8))
    }

    @Test
    fun formatters_Default_FormatsUnknown() {
        val formatter: Formatter = HttpRemoteCommand.Formatters.formatterFor("application/json")
        val formatted = formatter.format(testObject, utf8)!!

        assertEquals(testString, formatted.toString(utf8))
    }

    @Test
    fun formatters_FormsFormatter_FormatsJson() {
        val formatter: Formatter = HttpRemoteCommand.Formatters.formatterFor("application/x-www-form-urlencoded")
        val formatted = formatter.format(testJsonObject, utf8)!!

        val jsonString = formatted.toString(utf8)
        assertTrue(jsonString.contains("string=value"))
        assertTrue(jsonString.contains("int=10"))
        assertTrue(jsonString.contains("double=10.5"))
        assertTrue(jsonString.contains("boolean=true"))
        assertTrue(jsonString.contains("map=%7B%22key%22%3A%22value%22%7D"))
        assertTrue(jsonString.contains("list=%5B%22value_1%22%2C%22value_2%22%5D"))
        assertTrue(jsonString.contains("array=%5B%22value_1%22%2C%22value_2%22%5D"))
    }

    @Test
    fun formatters_FormsFormatter_FormatsString() {
        val formatter: Formatter = HttpRemoteCommand.Formatters.formatterFor("application/x-www-form-urlencoded")
        val formatted = formatter.format(testString, utf8)!!

        assertEquals(testString, formatted.toString(utf8))
    }

    @Test
    fun formatters_FormsFormatter_FormatsUnknown() {
        val formatter: Formatter = HttpRemoteCommand.Formatters.formatterFor("application/x-www-form-urlencoded")
        val formatted = formatter.format(testObject, utf8)!!

        assertEquals(testString, formatted.toString(utf8))
    }
}