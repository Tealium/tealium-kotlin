package com.tealium.remotecommanddispatcher

import android.os.Build
import androidx.annotation.RequiresApi
import com.tealium.core.Logger
import com.tealium.dispatcher.Dispatch

class RemoteCommandParser {
    companion object {

        fun mapDispatch(dispatch: Dispatch, lookup: Map<String, String>): MutableMap<String, Any> {
            val mappedDispatch = mapPayload(dispatch.payload(), lookup)
            mappedDispatch.forEach { (key, value) ->
                if (value is Map<*, *>) {
                    (value as? Map<String, Any>)?.let { nestedMap ->
                        if (mapPayload(nestedMap, lookup).count() == 0) {
                            mappedDispatch[key] = nestedMap
                        } else {
                            mappedDispatch.putAll(mapPayload(nestedMap, lookup))
                        }
                    }
                }
            }
            return mappedDispatch
        }

        /**
         * Creates new command dictionary from the dispatch payload and lookup table.
         * Only maps keys that are in the lookup, everything else in the payload is not mapped.
         *
         */
        private fun mapPayload(payload: Map<String, Any>, lookup: Map<String, String>): MutableMap<String, Any> {
            var temp = mutableMapOf<String, Any>()
            lookup.forEach { (key, value) ->
                payload[key]?.let { payloadValue ->
                    lookup[key]?.let { lookupValue ->
                        temp.putAll(splitKeys(lookupValue, payloadValue, lookup))
//                     temp = mergeMaps(temp, splitKeys(lookupValue, payloadValue, lookup)).toMutableMap()
                    }
                }
            }

            return temp
        }

        /**
         * Splits config mappings keys when a "." is present and creates nested object.
         * If key in JSON was "event.parameter", method returns  {event = {parameter = value}}
         */
        private fun splitKeys(key: String, payloadValue: Any, lookup: Map<String, String>): Map<String, Any> {
            val result = mutableMapOf<String, Any>()
            if (key.contains(".")) {
                val keyValue = key.split(".")
                val temp = mutableMapOf<String, Any>()
                if (payloadValue is Map<*, *>) {
                    Logger.dev(BuildConfig.TAG, "payloadValue is a Map")
                    (payloadValue as? Map<String, Any>)?.let {
                        temp.putAll(mapPayload(it, lookup))
                    }
                } else {
                    temp[keyValue[1]] = payloadValue
                }
                result[keyValue[0]] = temp
            } else {
                result[key] = payloadValue
            }

            return result
        }


    }
}