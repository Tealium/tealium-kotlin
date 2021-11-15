package com.tealium.core.persistence

import org.json.JSONArray
import org.json.JSONObject

interface Serde<T> {
    val serializer: Serializer<T>
    val deserializer: Deserializer<T>
}

interface Serializer<T> {
    fun serialize(value: T): String
}

interface Deserializer<T> {
    fun deserialize(value: String): T
}

object Serdes {
    private val serdeMap: MutableMap<Class<*>, Serde<*>> = mutableMapOf()

    private var stringSerde: Serde<String>? = null
    private var intSerde: Serde<Int>? = null
    private var longSerde: Serde<Long>? = null
    private var doubleSerde: Serde<Double>? = null
    private var booleanSerde: Serde<Boolean>? = null
    private var stringArraySerde: Serde<Array<String>>? = null
    private var intArraySerde: Serde<Array<Int>>? = null
    private var longArraySerde: Serde<Array<Long>>? = null
    private var doubleArraySerde: Serde<Array<Double>>? = null
    private var booleanArraySerde: Serde<Array<Boolean>>? = null
    private var jsonObjectSerde: Serde<JSONObject>? = null
    private var jsonArraySerde: Serde<JSONArray>? = null

    fun stringSerde(): Serde<String> {
        return (stringSerde
            ?: StringSerde()).also {
            stringSerde = it
            serdeMap.put(String::class.java, it)
        }
    }
    fun intSerde(): Serde<Int> {
        return (intSerde
            ?: IntSerde()).also {
            intSerde = it
            serdeMap.put(Int::class.java, it)
        }
    }
    fun longSerde(): Serde<Long> {
        return (longSerde
            ?: LongSerde()).also {
            longSerde = it
            serdeMap.put(Long::class.java, it)
        }
    }
    fun doubleSerde(): Serde<Double> {
        return (doubleSerde
            ?: DoubleSerde()).also {
            doubleSerde = it
            serdeMap.put(Double::class.java, it)
        }
    }
    fun booleanSerde(): Serde<Boolean> {
        return (booleanSerde
            ?: BooleanSerde()).also {
            booleanSerde = it
            serdeMap.put(Boolean::class.java, it)
        }
    }
    fun stringArraySerde(): Serde<Array<String>> {
        return (stringArraySerde
            ?: StringArraySerde()).also {
            stringArraySerde = it
            serdeMap.put(Array<String>::class.java, it)
        }
    }
    fun intArraySerde(): Serde<Array<Int>> {
        return (intArraySerde
            ?: IntArraySerde()).also {
            intArraySerde = it
            serdeMap.put(IntArray::class.java, it)
        }
    }
    fun longArraySerde(): Serde<Array<Long>> {
        return (longArraySerde
            ?: LongArraySerde()).also {
            longArraySerde = it
            serdeMap.put(LongArray::class.java, it)
        }
    }
    fun doubleArraySerde(): Serde<Array<Double>> {
        return (doubleArraySerde
            ?: DoubleArraySerde()).also {
            doubleArraySerde = it
            serdeMap.put(DoubleArray::class.java, it)
        }
    }
    fun booleanArraySerde(): Serde<Array<Boolean>> {
        return (booleanArraySerde
            ?: BooleanArraySerde()).also {
            booleanArraySerde = it
            serdeMap.put(BooleanArray::class.java, it)
        }
    }
    fun jsonObjectSerde(): Serde<JSONObject> {
        return (jsonObjectSerde
            ?: JsonObjectSerde()).also {
            jsonObjectSerde = it
            serdeMap.put(JSONObject::class.java, it)
        }
    }
    fun jsonArraySerde(): Serde<JSONArray> {
        return (jsonArraySerde
            ?: JsonArraySerde()).also {
            jsonArraySerde = it
            serdeMap.put(JSONArray::class.java, it)
        }
    }

    @Suppress("UNCHECKED_CAST")
    fun <T> serdeFor(clazz: Class<T>) : Serde<T>? {
        val mapped = serdeMap[clazz]
        if (mapped != null) {
            return mapped as Serde<T>
        }

        return when (clazz) {
            String::class.java -> stringSerde() as Serde<T>
            Int::class.java -> intSerde() as Serde<T>
            Double::class.java -> doubleSerde() as Serde<T>
            Long::class.java -> longSerde() as Serde<T>
            Boolean::class.java -> booleanSerde() as Serde<T>
            Array<String>::class.java -> stringArraySerde() as Serde<T>
            IntArray::class.java -> intArraySerde() as Serde<T>
            DoubleArray::class.java -> doubleArraySerde() as Serde<T>
            LongArray::class.java -> longArraySerde() as Serde<T>
            BooleanArray::class.java -> booleanArraySerde() as Serde<T>
            JSONObject::class.java -> jsonObjectSerde() as Serde<T>
            JSONArray::class.java -> jsonArraySerde() as Serde<T>
            else -> null
        }
    }

    fun <T> serializerFor(clazz: Class<T>) : Serializer<T>? {
        return serdeFor(clazz)?.serializer
    }

    fun <T> deserializerFor(clazz: Class<T>) : Deserializer<T>? {
        return serdeFor(clazz)?.deserializer
    }
}

private open class BaseSerde<T>(
    override val serializer: Serializer<T>,
    override val deserializer: Deserializer<T>
) : Serde<T>

private class GenericSerializer<T> : Serializer<T> {
    override fun serialize(value: T): String {
        return value.toString()
    }
}

private class ArraySerializer<Array> : Serializer<Array> {
    override fun serialize(value: Array): String {
        return JSONArray(value).toString()
    }
}

private class StringSerde : BaseSerde<String>(GenericSerializer(), StringDeserializer())
private class StringDeserializer : Deserializer<String> {
    override fun deserialize(value: String): String {
        return value
    }
}

private class IntSerde : BaseSerde<Int>(GenericSerializer(), IntDeserializer())
private class IntDeserializer : Deserializer<Int> {
    override fun deserialize(value: String): Int {
        return value.toInt()
    }
}

private class LongSerde : BaseSerde<Long>(GenericSerializer(), LongDeserializer())
private class LongDeserializer : Deserializer<Long> {
    override fun deserialize(value: String): Long {
        return value.toLong()
    }
}

private class DoubleSerde : BaseSerde<Double>(GenericSerializer(), DoubleDeserializer())
private class DoubleDeserializer : Deserializer<Double> {
    override fun deserialize(value: String): Double {
        return value.toDouble()
    }
}
private class BooleanSerde : BaseSerde<Boolean>(BooleanSerializer(), BooleanDeserializer())
private class BooleanSerializer : Serializer<Boolean> {
    override fun serialize(value: Boolean): String {
        return (if (value) 1 else 0).toString()
    }
}
private class BooleanDeserializer : Deserializer<Boolean> {
    override fun deserialize(value: String): Boolean {
        return value.toInt() > 0
    }
}

private class JsonObjectSerde : BaseSerde<JSONObject>(GenericSerializer(), JsonObjectDeserializer())
private class JsonObjectDeserializer : Deserializer<JSONObject> {
    override fun deserialize(value: String): JSONObject {
        return JSONObject(value)
    }
}

private class JsonArraySerde : BaseSerde<JSONArray>(GenericSerializer(), JsonArrayDeserializer())
private class JsonArrayDeserializer : Deserializer<JSONArray> {
    override fun deserialize(value: String): JSONArray {
        return JSONArray(value)
    }
}

private class StringArraySerde : BaseSerde<Array<String>>(ArraySerializer(), StringArrayDeserializer())
private class StringArrayDeserializer : Deserializer<Array<String>> {
    override fun deserialize(value: String): Array<String> {
        val jsonArray = JSONArray(value)
        val values = mutableListOf<String>()
        for (i in 0 until jsonArray.length()) {
            values.add(jsonArray[i].toString())
        }
        return values.toTypedArray()
    }
}

private class IntArraySerde : BaseSerde<Array<Int>>(ArraySerializer(), IntArrayDeserializer())
private class IntArrayDeserializer : Deserializer<Array<Int>> {
    override fun deserialize(value: String): Array<Int> {
        val jsonArray = JSONArray(value)
        val values = mutableListOf<Int>()
        for (i in 0 until jsonArray.length()) {
            when (jsonArray[i]) {
                is Int -> values.add(jsonArray[i] as Int)
                is String -> values.add(jsonArray[i].toString().toInt())
            }
        }
        return values.toTypedArray()
    }
}

private class LongArraySerde : BaseSerde<Array<Long>>(ArraySerializer(), LongArrayDeserializer())
private class LongArrayDeserializer : Deserializer<Array<Long>> {
    override fun deserialize(value: String): Array<Long> {
        val jsonArray = JSONArray(value)
        val values = mutableListOf<Long>()
        for (i in 0 until jsonArray.length()) {
            when (jsonArray[i]) {
                is Long -> values.add(jsonArray[i] as Long)
                is Int -> values.add((jsonArray[i] as Int).toLong())
                is String -> values.add(jsonArray[i].toString().toLong())
            }
        }
        return values.toTypedArray()
    }
}

private class DoubleArraySerde : BaseSerde<Array<Double>>(ArraySerializer(), DoubleArrayDeserializer())
private class DoubleArrayDeserializer : Deserializer<Array<Double>> {
    override fun deserialize(value: String): Array<Double> {
        val jsonArray = JSONArray(value)
        val values = mutableListOf<Double>()
        for (i in 0 until jsonArray.length()) {
            when (jsonArray[i]) {
                is Double -> values.add(jsonArray[i] as Double)
                is Long -> values.add((jsonArray[i] as Long).toDouble())
                is Int -> values.add((jsonArray[i] as Int).toDouble())
                is String -> values.add(jsonArray[i].toString().toDouble())
            }
        }
        return values.toTypedArray()
    }
}

private class BooleanArraySerde : BaseSerde<Array<Boolean>>(ArraySerializer(), BooleanArrayDeserializer())
private class BooleanArrayDeserializer : Deserializer<Array<Boolean>> {
    override fun deserialize(value: String): Array<Boolean> {
        val jsonArray = JSONArray(value)
        val values = mutableListOf<Boolean>()
        for (i in 0 until jsonArray.length()) {
            when (jsonArray[i]) {
                is Boolean -> values.add(jsonArray[i] as Boolean)
                is Int -> values.add((jsonArray[i] as Int) > 0)
                is String -> values.add(jsonArray[i].toString().toInt() > 0)
            }
        }
        return values.toTypedArray()
    }
}