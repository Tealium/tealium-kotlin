package com.tealium.transformations.internal.impl

import com.tealium.transformations.internal.Util
import java.security.MessageDigest

object UtilImpl : Util {

    override fun hash(algorithm: String, input: String): String {
        val bytes = MessageDigest.getInstance(algorithm)
            .digest(input.toByteArray())
        return printHexBinary(bytes).uppercase()
    }

    private val HEX_CHARS = "0123456789ABCDEF".toCharArray()

    fun printHexBinary(data: ByteArray): String {
        val r = StringBuilder(data.size * 2)
        data.forEach { b ->
            val i = b.toInt()
            r.append(HEX_CHARS[i shr 4 and 0xF])
            r.append(HEX_CHARS[i and 0xF])
        }
        return r.toString()
    }

}