package com.tealium.remotecommanddispatcher

class RemoteCommandParser {
    companion object {

        /**
         * Creates new command dictionary from the dispatch payload and lookup table.
         * Only maps keys that are in the lookup, everything else in the payload is not mapped.
         *
         */
        fun mapPayload(payload: Map<String, Any>, lookup: Map<String, String>): MutableMap<String, Any> {
            val mappedPayload = mutableMapOf<String, Any>()
            lookup.forEach { (lookupKey, lookupDestination) ->
                payload[lookupKey]?.let { payloadValue ->
                    checkAndSplitDestinationList(lookupDestination).forEach { destinations ->
                        val objectRow = splitKeys(destinations, payloadValue)
                        objectRow.second?.let { objectKey ->
                            if (mappedPayload.containsKey(objectKey)) {
                                // object key is already in the map, append to the same key
                                (mappedPayload[objectKey] as? MutableMap<*, *>)?.let { objectMap ->
                                    // create a map with a String key. This will throw an exception if the JSON mapping file does not use a String as a key.
                                    val oMap = objectMap.entries.associate { entry -> entry.key.toString() to entry.value }.toMutableMap()
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
        private fun splitKeys(key: String, payloadValue: Any): Pair<MutableMap<String, Any>, String?> {
            val result = mutableMapOf<String, Any>()
            var objectKey: String? = null
            if (key.contains(".")) {
                val keyValue = key.split(".")
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
        private fun checkAndSplitDestinationList(lookupValue: String): List<String> {
            return lookupValue.split(",").map { it.trim() }
        }
    }
}