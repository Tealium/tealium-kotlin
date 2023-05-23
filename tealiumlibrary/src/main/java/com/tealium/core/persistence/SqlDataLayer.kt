package com.tealium.core.persistence

import android.provider.BaseColumns

object SqlDataLayer {

    object Columns : BaseColumns {
        const val COLUMN_KEY = "key"
        const val COLUMN_VALUE = "value"
        const val COLUMN_EXPIRY = "expiry"
        const val COLUMN_TIMESTAMP = "timestamp"
        const val COLUMN_TYPE = "type"
    }

    object Sql {
        fun getCreateTableSql(tableName: String): String {
            return "CREATE TABLE IF NOT EXISTS $tableName (" +
                    "${Columns.COLUMN_KEY} TEXT PRIMARY KEY," +
                    "${Columns.COLUMN_VALUE} TEXT," +
                    "${Columns.COLUMN_EXPIRY} LONG, " +
                    "${Columns.COLUMN_TIMESTAMP} LONG, " +
                    "${Columns.COLUMN_TYPE} SMALLINT)"
        }

        fun getDeleteTableSql(tableName: String): String {
            return "DROP TABLE $tableName"
        }
    }
}