package com.tealium.core

import android.content.SharedPreferences

data class Session(val id: Long,
                   var lastEventTime: Long = 0,
                   var eventCount: Int = 0,
                   var sessionStarted: Boolean = false) {

    companion object {
        const val INVALID_SESSION_ID = 0L
        const val KEY_SESSION_ID = "tealium_session_id";
        const val KEY_SESSION_LAST_EVENT_TIME = "tealium_session_last_event_time";
        const val KEY_SESSION_EVENT_COUNT = "tealium_session_event_count";
        const val KEY_SESSION_STARTED = "tealium_session_started";

        fun writeToSharedPreferences(sharedPreferences: SharedPreferences, session: Session) {
            sharedPreferences.edit().putLong(KEY_SESSION_ID, session.id)
                    .putLong(KEY_SESSION_LAST_EVENT_TIME, session.lastEventTime)
                    .putInt(KEY_SESSION_EVENT_COUNT, session.eventCount)
                    .putBoolean(KEY_SESSION_STARTED, session.sessionStarted)
                    .apply()
        }

        fun readFromSharedPreferences(sharedPreferences: SharedPreferences): Session {
            return Session(sharedPreferences.getLong(KEY_SESSION_ID, INVALID_SESSION_ID),
                    sharedPreferences.getLong(KEY_SESSION_LAST_EVENT_TIME, 0L),
                    sharedPreferences.getInt(KEY_SESSION_EVENT_COUNT, 0),
                    sharedPreferences.getBoolean(KEY_SESSION_STARTED, false))
        }
    }
}