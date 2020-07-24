package com.tealium.remotecommanddispatcher

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
            val temp = mutableMapOf<String, Any>()
            lookup.forEach { (key, value) ->
                payload[key]?.let { payloadValue ->
                    lookup[key]?.let { lookupValue ->
                        val objectRow = splitKeys(lookupValue, payloadValue, lookup)
                        val objectKey = objectRow.second
                        objectKey?.let {
                            if (temp.containsKey(it)) {
                                // object key is already in the map, append to the same key
                                (temp[it] as? MutableMap<*, *>)?.let { objectMap ->
                                    // create a map with a String key. This will throw an exception if the JSON mapping file does not use a String as a key.
                                    val oMap = objectMap.entries.associate { entry -> entry.key as String to entry.value }.toMutableMap()
                                    (objectRow.first[objectKey] as? Map<*, *>)?.let {
                                        it.entries.associate { entry -> entry.key as String to entry.value as String }
                                    }?.forEach { (kk, vv) ->
                                        // add mapped values from splitKeys to the temporary oMap
                                        oMap[kk] = vv
                                        // append to the same key (e.g. "event")
                                        temp[objectKey] = oMap
                                    }
                                }
                            } else {
                                temp.putAll(objectRow.first)
                            }
                        } ?: run {
                            temp.putAll(objectRow.first)
                        }
                    }
                }
            }

            return temp
        }

        /**
         * Splits config mappings keys when a "." is present and creates nested object.
         * If key in JSON was "event.parameter", method returns  {event = {parameter = value}}
         */
        private fun splitKeys(key: String, payloadValue: Any, lookup: Map<String, String>): Pair<MutableMap<String, Any>, String?> {
            val result = mutableMapOf<String, Any>()
            var objectKey: String? = null
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
                objectKey = keyValue[0]
            } else {
                result[key] = payloadValue
            }

            return Pair(result, objectKey)
        }


    }
}