package com.tealium.core

import com.tealium.dispatcher.Dispatch
import java.net.URLEncoder

interface Encoder {
    fun encode(dispatch: Dispatch): String
    fun encode(config: TealiumConfig): String
}

class TealiumEncoder {

    companion object: Encoder {
        const val encoding = "UTF-8"

        override fun encode(dispatch: Dispatch): String {
            val payload = dispatch.payload()
            val keys = dispatch.payload().keys

            var encoded = ""
            keys.forEach {
                val value = payload[it]

                encoded += when (value) {
                    is String -> encode(it, value)
                    is Int -> encode(it, value)
                    is Float -> encode(it, value)
                    is Double -> encode(it, value)
                    is Array<*> -> encodeCollection(it, value)
                    else -> {
                        "${URLEncoder.encode(it, encoding)}=${URLEncoder.encode(value.toString(), encoding)}"
                    }
                }
                encoded += "&"
            }

            return encoded.removeSuffix("&")
        }

        override fun encode(config: TealiumConfig): String {
            return "${URLEncoder.encode(TEALIUM_ACCOUNT, encoding)}=${URLEncoder.encode(config.accountName, encoding)}&" +
                    "${URLEncoder.encode(TEALIUM_PROFILE, encoding)}=${URLEncoder.encode(config.profileName, encoding)}"
        }

        private fun <T> encodeCollection(key: String, value: Array<T>): String {
            var encoded = ""
            value.forEach {
                encoded += URLEncoder.encode(it.toString(), encoding) + ","
            }
            return "${URLEncoder.encode(key, encoding)}=${URLEncoder.encode(encoded.removeSuffix(","), encoding)}"
        }

        private fun <T> encode(key: String, value: T): String {
            return "${URLEncoder.encode(key, encoding)}=${URLEncoder.encode(value.toString(), encoding)}"
        }
    }
}