package com.tealium.visitorservice.momentsapi

import com.tealium.core.LogLevel
import java.util.Locale

enum class ErrorCode(val value: Int) {
    BAD_REQUEST(400 ),
    ENGINE_NOT_ENABLED(403),
    VISITOR_NOT_FOUND(404),
    NOT_CONNECTED (0),
    INVALID_JSON (1),
    UNKNOWN_ERROR(2);

    companion object {
        fun fromInt(int: Int) : ErrorCode {
            return when(int) {
                400 -> BAD_REQUEST
                403 -> ENGINE_NOT_ENABLED
                404 -> VISITOR_NOT_FOUND
                else -> UNKNOWN_ERROR
            }
        }
    }
}