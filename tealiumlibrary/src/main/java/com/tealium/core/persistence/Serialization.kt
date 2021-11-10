package com.tealium.core.persistence

import org.json.JSONArray
import org.json.JSONObject

enum class Serialization(val code: Int, val clazz: Class<*>) {
    STRING(0, String::class.java),
    INT(1, Int::class.java),
    DOUBLE(2, Double::class.java),
    LONG(3, Long::class.java),
    BOOLEAN(4, Boolean::class.java),
    STRING_ARRAY(5, Array<String>::class.java),
    INT_ARRAY(6, IntArray::class.java),
    DOUBLE_ARRAY(7, DoubleArray::class.java),
    LONG_ARRAY(8, LongArray::class.java),
    BOOLEAN_ARRAY(9, BooleanArray::class.java),
    JSON_OBJECT(10, JSONObject::class.java),
    JSON_ARRAY(11, JSONArray::class.java);
}