package com.tealium.remotecommanddispatcher

import com.tealium.core.DispatchType
import com.tealium.dispatcher.Dispatch

class RemoteCommandParser {
    companion object {

        /**
         * Creates new command dictionary from the dispatch payload and lookup table.
         * Only maps keys that are in the lookup, everything else in the payload is not mapped.
         *
         */
        fun mapPayload(
            payload: Map<String, Any>,
            lookup: Map<String, String>
        ): MutableMap<String, Any> {
            val mappedPayload = mutableMapOf<String, Any>()
            lookup.forEach { (lookupKey, lookupDestination) ->
                payload[lookupKey]?.let { payloadValue ->
                    checkAndSplitDestinationList(lookupDestination, ",").forEach { destinations ->
                        val objectRow = splitKeys(destinations, payloadValue)
                        objectRow.second?.let { objectKey ->
                            if (mappedPayload.containsKey(objectKey)) {
                                // object key is already in the map, append to the same key
                                (mappedPayload[objectKey] as? MutableMap<*, *>)?.let { objectMap ->
                                    // create a map with a String key. This will throw an exception if the JSON mapping file does not use a String as a key.
                                    val oMap =
                                        objectMap.entries.associate { entry -> entry.key.toString() to entry.value }
                                            .toMutableMap()
                                    (objectRow.first[objectKey] as? Map<*, *>)?.let {
                                        it.entries.associate { entry -> entry.key.toString() to entry.value }
                                    }?.forEach { (kk, vv) ->
                                        // add mapped values from splitKeys to the temporary oMap
                                        oMap[kk] = vv
                                        // append to the same key (e.g. "event")
                                        mappedPayload[objectKey] = oMap
                                    }
                                }
                            } else {
                                mappedPayload.putAll(objectRow.first)
                            }
                        } ?: run {
                            mappedPayload.putAll(objectRow.first)
                        }
                    }
                }
            }

            return mappedPayload
        }

        /**
         * Splits config mappings keys when a "." is present and creates nested object.
         * If key in JSON was "event.parameter", method returns  {event = {parameter = value}}
         */
        private fun splitKeys(
            key: String,
            payloadValue: Any
        ): Pair<MutableMap<String, Any>, String?> {
            val result = mutableMapOf<String, Any>()
            var objectKey: String? = null
            if (key.contains(".")) {
                val keyValue = checkAndSplitDestinationList(key, ".")
                result[keyValue.first()] = mutableMapOf(keyValue.last() to payloadValue)
                objectKey = keyValue.first()
            } else {
                result[key] = payloadValue
            }

            return Pair(result, objectKey)
        }

        /**
         * Checks for multi-destination lookup values and returns a list of destinations to be mapped
         * If lookup value in JSON was "event.destination1, event.destination2",
         * method returns listOf("event.destination1", "event.destination2")
         */
        private fun checkAndSplitDestinationList(
            lookupValue: String,
            delimiter: String
        ): List<String> {
            return lookupValue.split(delimiter).map { it.trim() }
        }

        fun processStaticMappings(
            statics: Map<String, Any>?,
            payload: Map<String, Any>,
            delimiters: Delimiters
        ): Map<String, Any> {
            val processedPayload = payload.toMutableMap()
            statics?.let { staticMap ->
                staticMap.forEach { (key, value) ->
                    val splitKeys = splitCompoundKeys(key, delimiters)
                    if (matchKeys(splitKeys, payload)) {
                        val valueMap =
                            (value as Map<*, *>).entries.associate { entry -> entry.key.toString() to entry.value as Any }
                        processedPayload.putAll(valueMap)
                    }
                }
            }
            return processedPayload.toMap()
        }

        fun extractCommandNames(
            commands: Map<String, String>?,
            payload: Map<String, Any>,
            delimiters: Delimiters
        ): String {
            val eventType = payload[Dispatch.Keys.TEALIUM_EVENT_TYPE] as? String
            val commandsList = mutableListOf<String>()

            commands?.let { commandsMap ->
                commandsMap.forEach { (key, value) ->
                    val splitCommands = splitCompoundKeys(key, delimiters)
                    if (matchKeys(splitCommands, payload)) {
                        commandsList.addAll(checkAndSplitDestinationList(value, ","))
                    }
                }
            }

            commands?.get(Settings.ALL_EVENTS)?.let {
                if (eventType == DispatchType.EVENT) commandsList.add(it)
            }
            commands?.get(Settings.ALL_VIEWS)?.let {
                if (eventType == DispatchType.VIEW) commandsList.add(it)
            }

            return commandsList.joinToString(",")
        }

        private fun splitCompoundKeys(key: String, delimiters: Delimiters): Map<String, String> {
            val result = mutableMapOf<String, String>()
            val keys = checkAndSplitDestinationList(key, delimiters.keysSeparationDelimiter)
            keys.forEach { k ->
                val keyValue = checkAndSplitDestinationList(k, delimiters.keysEqualityDelimiter)
                if (keyValue.size == 1) {
                    result[Key.TEALIUM_EVENT] = keyValue.first()
                } else {
                    result[keyValue.first()] = keyValue.last()
                }
            }
            return result.toMap()
        }

        private fun matchKeys(
            keyValues: Map<String, String>,
            payload: Map<String, Any>
        ): Boolean {
            val result = keyValues.filter { (key, value) ->
                if (!payload.containsKey(key)) {
                    return@filter true
                } else {
                    return@filter payload[key] != value
                }
            }

            return result.isEmpty()
        }
    }
}