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
                            mappedDispatch[key] = mapPayload(nestedMap, lookup)
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
        fun mapPayload(payload: Map<String, Any>, lookup: Map<String, String>): MutableMap<String, Any> {
            val temp = mutableMapOf<String, Any>()
            lookup.forEach { (key, value) ->
                payload[key]?.let { payloadValue ->
                    lookup[key]?.let { lookupValue ->
                        if (payloadValue is Map<*, *>) {
                            Logger.dev(BuildConfig.TAG, "payloadValue is a Map")
                            (payloadValue as? Map<String, Any>)?.let {
                                temp.putAll(mapPayload(it, lookup))
                            }
                        }
                        temp[lookupValue] = payloadValue
                    }
                }
            }

            return temp
        }
    }
}