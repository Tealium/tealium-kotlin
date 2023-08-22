package com.tealium.core.persistence

import android.database.sqlite.SQLiteDatabase
import com.tealium.core.Logger
import com.tealium.tealiumlibrary.BuildConfig
import java.security.MessageDigest
import java.util.*

internal fun getTimestamp(): Long {
    return System.currentTimeMillis() / 1000
}

internal fun getTimestampMilliseconds(): Long {
    return System.currentTimeMillis()
}

interface HashingProvider {
    fun hash(algorithm: String, input: String): String
}

object DefaultHashingProvider : HashingProvider {

    override fun hash(algorithm: String, input: String): String {
        val bytes = MessageDigest.getInstance(algorithm)
            .digest(input.toByteArray())
        return printHexBinary(bytes).toUpperCase(Locale.ROOT)
    }

    private val HEX_CHARS = "0123456789ABCDEF".toCharArray()

    private fun printHexBinary(data: ByteArray): String {
        val r = StringBuilder(data.size * 2)
        data.forEach { b ->
            val i = b.toInt()
            r.append(HEX_CHARS[i shr 4 and 0xF])
            r.append(HEX_CHARS[i and 0xF])
        }
        return r.toString()
    }
}

fun String.sha256() : String {
    return DefaultHashingProvider.hash("SHA-256", this)
}

internal fun PersistentItem.deserialize() : Any? {
    return Serdes.serdeFor(this.type.clazz)?.deserializer?.deserialize(this.value)
}

/**
 * Convenience method to wrap database execution in the relevant transaction calls.
 *
 * @param errorMessage Log message to output if the [block] throws an exception.
 * @param block The block of code to execute the database operations
 */
internal fun DatabaseHelper.transaction(errorMessage: String, block: (SQLiteDatabase) -> Unit) =
    transaction({ Logger.dev(BuildConfig.TAG, errorMessage) }, block)

/**
 * Convenience method to wrap database execution in the relevant transaction calls.
 *
 * @param onException Listener for any exceptions thrown by the [block] provided
 * @param block The block of code to execute the database operations
 */
internal fun DatabaseHelper.transaction(
    onException: ((Exception) -> Unit)? = null,
    block: (SQLiteDatabase) -> Unit
) = onDbReady { database ->
    try {
        database.beginTransactionNonExclusive()

        try {
            block(database)

            database.setTransactionSuccessful()
        } catch (e: Exception) {
            onException?.invoke(e)
        } finally {
            database.endTransaction()
        }
    } catch (ex: Exception) {
        Logger.dev(BuildConfig.TAG, "Could not begin transaction: ${ex.message}")
    }
}