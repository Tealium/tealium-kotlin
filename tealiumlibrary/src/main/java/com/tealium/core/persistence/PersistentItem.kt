package com.tealium.core.persistence

import android.content.ContentValues

data class PersistentItem(
    val key: String,
    val value: String,
    var expiry: Expiry? = null,
    var timestamp: Long? = null,
    val type: Serialization
) {
    fun toContentValues(): ContentValues {
        val contentValues = ContentValues()

        contentValues.put(SqlDataLayer.Columns.COLUMN_KEY, key)
        contentValues.put(SqlDataLayer.Columns.COLUMN_VALUE, value)
        contentValues.put(SqlDataLayer.Columns.COLUMN_TYPE, type.code)
        expiry?.let {
            contentValues.put(SqlDataLayer.Columns.COLUMN_EXPIRY, it.expiryTime())
        }
        timestamp?.let {
            contentValues.put(SqlDataLayer.Columns.COLUMN_TIMESTAMP, it)
        }
        return contentValues
    }
}