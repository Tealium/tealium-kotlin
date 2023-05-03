package com.tealium.core.settings

import com.tealium.core.network.ResourceEntity
import org.json.JSONObject
import java.util.*
import java.util.regex.Pattern

class LibrarySettingsExtractor private constructor() {

    companion object {

        private const val REGEX_TIME_AMOUNT = "\\d+"
        private const val REGEX_TIME_UNIT = "[hmds]$"

        private const val REGEX_SCRIPT = "<script(.*?)>(.*?)</script>"
        private const val REGEX_VARS = ";? *var +[\\w]+ *= *"
        private const val SUB_STRING_SRC = "src"
        private const val SUB_STRING_MPS = "mps"
        private const val ETAG_KEY = "etag"

        // Returns the time in seconds
        fun timeConverter(time: String): Int {
            val amount = Regex(REGEX_TIME_AMOUNT).find(time)?.let { amount ->
                val unit = Regex(REGEX_TIME_UNIT)
                unit.find(time)?.let {
                    amount.value.toInt() * toSeconds(it.value)
                }
            }
            return amount ?: -1
        }

        private fun toSeconds(unit: String): Int {
            return when (unit) {
                "d" -> 86400
                "h" -> 3600
                "m" -> 60
                "s" -> 1
                else -> 60
            }
        }

        fun extractHtmlLibrarySettings(response: ResourceEntity): JSONObject? {
            return response.response?.let {
                extract(it)?.let { entity ->
                    // only interested in v5 properties
                    val json = JSONObject(entity).getJSONObject("5")
                    response.etag?.let { tag ->
                        json.put(ETAG_KEY, tag)
                    }
                    json
                }
            }
        }

        private fun extract(html: String): String? {
            val scriptPattern = Pattern.compile(REGEX_SCRIPT)
            val varsPattern = Pattern.compile(REGEX_VARS)
            val scriptMatches = scriptPattern.matcher(html)
            var mps: String? = null
            while (scriptMatches.find() && mps == null) {
                scriptMatches.group(1)?.let { matchGroup1 ->
                    if (!matchGroup1.toLowerCase(Locale.ROOT).contains(SUB_STRING_SRC)) {
                        scriptMatches.group(2)?.let { matchGroup2 ->
                            mps = findMps(varsPattern, matchGroup2)
                        }
                    }
                }
            }
            return mps
        }

        private fun findMps(varsPattern: Pattern, js: String): String? {
            val varMatches = varsPattern.matcher(js)
            var mpsStart = -1
            var mpsEnd = -1
            while (varMatches.find()) { // "var utag_cfg_ovrd=" or "; var nativeAppLiveHandlerData ="
                varMatches.group(0)?.let { varMatch ->
                    if (varMatch.toLowerCase(Locale.ROOT).contains(SUB_STRING_MPS)) { // Start at the stuff after the "var mps = "
                        mpsStart = js.indexOf(varMatch) + varMatch.length
                    } else if (mpsStart != -1 && mpsEnd == -1) { // The beginning has been found, set the ending
                        mpsEnd = js.indexOf(varMatch)
                    }
                }
            }
            if (mpsStart == -1) { // No MPS
                return null
            }
            if (mpsEnd == -1) { // Nothing after MPS
                mpsEnd = js.length
            }
            return js.substring(mpsStart, mpsEnd)
        }
    }
}