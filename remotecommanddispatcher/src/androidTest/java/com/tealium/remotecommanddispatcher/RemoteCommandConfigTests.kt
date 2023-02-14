package com.tealium.remotecommanddispatcher

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Test

class RemoteCommandConfigTests {

    private val expectedConfig = mapOf<String, Any>("config_setting1" to "setting1", "config_setting2" to "setting2", "config_setting3" to "setting3", "config_setting4" to "setting4")
    private val expectedMappings = mapOf("mappings_test1" to "param_test1", "mappings_test2" to "param_test2", "mappings_test3" to "param_test3", "mappings_test4" to "param_test4", "mappings_test5" to "param_test5")
    private val expectedCommands = mapOf("launch" to "initialize", "screen_view" to "setscreenname", "click_thru" to "logevent", "social_media_activity" to "setuserid", "user_login" to "logevent")
    private val expectedCompoundCommands = mapOf("tealium_event:launch" to "initialize", "screen_view:home,page_content:homeView" to "setscreenname", "click_thru" to "logevent", "social_media_activity" to "setuserid", "user_login" to "logevent")
    private val expectedStatics = mapOf("launch" to mapOf("static_key1" to "launch_event", "static_key2" to "some_value"), "tealium_event:click,page_name:home" to mapOf("static_key" to "click_value"))

    @Test
    fun remoteCommandConfigValidParseFromJson() {
        val remoteCommandConfig = "{\n" +
                "  \"config\": {\n" +
                "  \"config_setting1\": \"setting1\",\n" +
                "  \"config_setting2\": \"setting2\",\n" +
                "  \"config_setting3\": \"setting3\",\n" +
                "  \"config_setting4\": \"setting4\"\n" +
                "  },\n" +
                "  \"mappings\": {\n" +
                "  \"mappings_test1\": \"param_test1\",\n" +
                "  \"mappings_test2\": \"param_test2\",\n" +
                "  \"mappings_test3\": \"param_test3\",\n" +
                "  \"mappings_test4\": \"param_test4\",\n" +
                "  \"mappings_test5\": \"param_test5\"\n" +
                "  },\n" +
                "  \"commands\": {\n" +
                "  \"launch\": \"initialize\",\n" +
                "  \"screen_view\": \"setscreenname\",\n" +
                "  \"click_thru\": \"logevent\",\n" +
                "  \"social_media_activity\": \"setuserid\",\n" +
                "  \"user_login\": \"logevent\"\n" +
                "  }\n" +
                "}"
        val json = JSONObject(remoteCommandConfig)
        val config = RemoteCommandConfig.fromJson(json)

        assertEquals(expectedConfig, config.apiConfig)
        assertEquals(expectedMappings, config.mappings)
        assertEquals(expectedCommands, config.apiCommands)
    }

    @Test
    fun remoteCommandConfigWithCompoundCommandsValidParseFromJson() {
        val remoteCommandConfig = "{\n" +
                "  \"config\": {\n" +
                "  \"config_setting1\": \"setting1\",\n" +
                "  \"config_setting2\": \"setting2\",\n" +
                "  \"config_setting3\": \"setting3\",\n" +
                "  \"config_setting4\": \"setting4\"\n" +
                "  },\n" +
                "  \"mappings\": {\n" +
                "  \"mappings_test1\": \"param_test1\",\n" +
                "  \"mappings_test2\": \"param_test2\",\n" +
                "  \"mappings_test3\": \"param_test3\",\n" +
                "  \"mappings_test4\": \"param_test4\",\n" +
                "  \"mappings_test5\": \"param_test5\"\n" +
                "  },\n" +
                "  \"commands\": {\n" +
                "  \"tealium_event:launch\": \"initialize\",\n" +
                "  \"screen_view:home,page_content:homeView\": \"setscreenname\",\n" +
                "  \"click_thru\": \"logevent\",\n" +
                "  \"social_media_activity\": \"setuserid\",\n" +
                "  \"user_login\": \"logevent\"\n" +
                "  }\n" +
                "}"
        val json = JSONObject(remoteCommandConfig)
        val config = RemoteCommandConfig.fromJson(json)

        assertEquals(expectedConfig, config.apiConfig)
        assertEquals(expectedMappings, config.mappings)
        assertEquals(expectedCompoundCommands, config.apiCommands)
    }

    @Test
    fun remoteCommandConfigValidToJson() {
        val remoteCommandConfig = "{\n" +
                "  \"config\": {\n" +
                "  \"config_setting1\": \"setting1\",\n" +
                "  \"config_setting2\": \"setting2\",\n" +
                "  \"config_setting3\": \"setting3\",\n" +
                "  \"config_setting4\": \"setting4\"\n" +
                "  },\n" +
                "  \"mappings\": {\n" +
                "  \"mappings_test1\": \"param_test1\",\n" +
                "  \"mappings_test2\": \"param_test2\",\n" +
                "  \"mappings_test3\": \"param_test3\",\n" +
                "  \"mappings_test4\": \"param_test4\",\n" +
                "  \"mappings_test5\": \"param_test5\"\n" +
                "  },\n" +
                "  \"commands\": {\n" +
                "  \"launch\": \"initialize\",\n" +
                "  \"screen_view\": \"setscreenname\",\n" +
                "  \"click_thru\": \"logevent\",\n" +
                "  \"social_media_activity\": \"setuserid\",\n" +
                "  \"user_login\": \"logevent\"\n" +
                "  }\n" +
                "}"
        val json = JSONObject(remoteCommandConfig)
        val config = RemoteCommandConfig(apiConfig = expectedConfig, mappings = expectedMappings, apiCommands = expectedCommands)
        val configJson = RemoteCommandConfig.toJson(config)

        assertEquals(json.toString(), configJson.toString())
    }

    @Test
    fun remoteCommandConfigWithCompoundCommandsValidToJson() {
        val remoteCommandConfig = "{\n" +
                "  \"config\": {\n" +
                "  \"config_setting1\": \"setting1\",\n" +
                "  \"config_setting2\": \"setting2\",\n" +
                "  \"config_setting3\": \"setting3\",\n" +
                "  \"config_setting4\": \"setting4\"\n" +
                "  },\n" +
                "  \"mappings\": {\n" +
                "  \"mappings_test1\": \"param_test1\",\n" +
                "  \"mappings_test2\": \"param_test2\",\n" +
                "  \"mappings_test3\": \"param_test3\",\n" +
                "  \"mappings_test4\": \"param_test4\",\n" +
                "  \"mappings_test5\": \"param_test5\"\n" +
                "  },\n" +
                "  \"commands\": {\n" +
                "  \"tealium_event:launch\": \"initialize\",\n" +
                "  \"screen_view:home,page_content:homeView\": \"setscreenname\",\n" +
                "  \"click_thru\": \"logevent\",\n" +
                "  \"social_media_activity\": \"setuserid\",\n" +
                "  \"user_login\": \"logevent\"\n" +
                "  }\n" +
                "}"
        val json = JSONObject(remoteCommandConfig)
        val config = RemoteCommandConfig(apiConfig = expectedConfig, mappings = expectedMappings, apiCommands = expectedCompoundCommands)
        val configJson = RemoteCommandConfig.toJson(config)

        assertEquals(json.toString(), configJson.toString())
    }

    @Test
    fun remoteCommandConfigWithDefaultDelimitersValidToJson() {
        val remoteCommandConfig = "{\n" +
                "  \"config\": {\n" +
                "  \"config_setting1\": \"setting1\",\n" +
                "  \"config_setting2\": \"setting2\",\n" +
                "  \"config_setting3\": \"setting3\",\n" +
                "  \"config_setting4\": \"setting4\"\n" +
                "  },\n" +
                "  \"mappings\": {\n" +
                "  \"mappings_test1\": \"param_test1\",\n" +
                "  \"mappings_test2\": \"param_test2\",\n" +
                "  \"mappings_test3\": \"param_test3\",\n" +
                "  \"mappings_test4\": \"param_test4\",\n" +
                "  \"mappings_test5\": \"param_test5\"\n" +
                "  },\n" +
                "  \"commands\": {\n" +
                "  \"launch\": \"initialize\",\n" +
                "  \"screen_view\": \"setscreenname\",\n" +
                "  \"click_thru\": \"logevent\",\n" +
                "  \"social_media_activity\": \"setuserid\",\n" +
                "  \"user_login\": \"logevent\"\n" +
                "  }\n" +
                "}"
        val json = JSONObject(remoteCommandConfig)
        val config = RemoteCommandConfig.fromJson(json)

        assertEquals(Settings.DEFAULT_EQUALITY_DELIMITER, config.delimiters.keysEqualityDelimiter)
        assertEquals(Settings.DEFAULT_SEPARATION_DELIMITER, config.delimiters.keysSeparationDelimiter)
    }

    @Test
    fun remoteCommandConfigWithCustomDelimitersValidToJson() {
        val remoteCommandConfig = "{\n" +
                "  \"config\": {\n" +
                "  \"config_setting1\": \"setting1\",\n" +
                "  \"config_setting2\": \"setting2\",\n" +
                "  \"keys_separation_delimiter\": \"&&\",\n" +
                "  \"keys_equality_delimiter\": \"==\"\n" +
                "  },\n" +
                "  \"mappings\": {\n" +
                "  \"mappings_test1\": \"param_test1\",\n" +
                "  \"mappings_test2\": \"param_test2\",\n" +
                "  \"mappings_test3\": \"param_test3\",\n" +
                "  \"mappings_test4\": \"param_test4\",\n" +
                "  \"mappings_test5\": \"param_test5\"\n" +
                "  },\n" +
                "  \"commands\": {\n" +
                "  \"launch\": \"initialize\",\n" +
                "  \"screen_view\": \"setscreenname\",\n" +
                "  \"click_thru\": \"logevent\",\n" +
                "  \"social_media_activity\": \"setuserid\",\n" +
                "  \"user_login\": \"logevent\"\n" +
                "  }\n" +
                "}"
        val json = JSONObject(remoteCommandConfig)
        val config = RemoteCommandConfig.fromJson(json)

        assertEquals("==", config.delimiters.keysEqualityDelimiter)
        assertEquals("&&", config.delimiters.keysSeparationDelimiter)
    }

    @Test
    fun remoteCommandConfigWithStaticsValidToJson() {
        val remoteCommandConfig = "{\n" +
                "  \"config\": {\n" +
                "  \"config_setting1\": \"setting1\",\n" +
                "  \"config_setting2\": \"setting2\",\n" +
                "  \"config_setting3\": \"setting3\",\n" +
                "  \"config_setting4\": \"setting4\"\n" +
                "  },\n" +
                "  \"mappings\": {\n" +
                "  \"mappings_test1\": \"param_test1\",\n" +
                "  \"mappings_test2\": \"param_test2\",\n" +
                "  \"mappings_test3\": \"param_test3\",\n" +
                "  \"mappings_test4\": \"param_test4\",\n" +
                "  \"mappings_test5\": \"param_test5\"\n" +
                "  },\n" +
                "  \"commands\": {\n" +
                "  \"launch\": \"initialize\",\n" +
                "  \"screen_view\": \"setscreenname\",\n" +
                "  \"click_thru\": \"logevent\",\n" +
                "  \"social_media_activity\": \"setuserid\",\n" +
                "  \"user_login\": \"logevent\"\n" +
                "  },\n" +
                "  \"statics\": {\n" +
                "  \"launch\": {\n" +
                "  \"static_key1\": launch_event,\n" +
                "  \"static_key2\": \"some_value\"\n" +
                "  },\n" +
                "  \"tealium_event:click,page_name:home\": {\n" +
                "  \"static_key\": \"click_value\"\n" +
                "  }\n" +
                "  }\n" +
                "}"
        val json = JSONObject(remoteCommandConfig)
        val config = RemoteCommandConfig(apiConfig = expectedConfig, mappings = expectedMappings, apiCommands = expectedCommands, statics = expectedStatics)
        val configJson = RemoteCommandConfig.toJson(config)

        assertEquals(json.toString(), configJson.toString())

    }

    @Test
    fun remoteCommandConfigValidToFromJson() {
        val config = RemoteCommandConfig(apiConfig = expectedConfig, mappings = expectedMappings, apiCommands = expectedCommands)
        val configJson = RemoteCommandConfig.toJson(config)
        val configFromJson = RemoteCommandConfig.fromJson(configJson)

        assertEquals(config, configFromJson)
    }

    @Test
    fun remoteCommandConfigWithStaticsValidToFromJson() {
        val config = RemoteCommandConfig(apiConfig = expectedConfig, mappings = expectedMappings, apiCommands = expectedCommands, statics = expectedStatics)
        val configJson = RemoteCommandConfig.toJson(config)
        val configFromJson = RemoteCommandConfig.fromJson(configJson)

        assertEquals(config, configFromJson)
    }

    @Test
    fun remoteCommandConfigWithStaticsAndCustomDelimitersValidToFromJson() {
        val config = RemoteCommandConfig(apiConfig = expectedConfig, mappings = expectedMappings, apiCommands = expectedCommands, statics = expectedStatics, delimiters = Delimiters("==", "&&"))
        val configJson = RemoteCommandConfig.toJson(config)
        val configFromJson = RemoteCommandConfig.fromJson(configJson)

        assertEquals(config, configFromJson)
    }
}