package com.tealium.core.persistence

interface StringSerializer<T> {
    fun serialize(): String
}

interface StringDeserializer<T> {
    fun deserialize(string: String): T
}