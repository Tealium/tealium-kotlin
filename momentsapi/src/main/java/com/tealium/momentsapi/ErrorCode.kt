package com.tealium.momentsapi

enum class ErrorCode(val value: Int, val message: String) {
    BAD_REQUEST(400, "Bad Request. Please check your configuration and verify request parameters."),
    ENGINE_NOT_ENABLED(403, "The Moments API engine is not enabled. Please check your configuration."),
    VISITOR_NOT_FOUND(404, "Visitor data not found. The Tealium visitor ID does not have moments stored in the database."),
    NOT_CONNECTED (0, "No connectivity established."),
    INVALID_JSON (1, "Invalid JSON or unsupported type for engine response."),
    UNKNOWN_ERROR(2, "Unknown error fetching engine response.");

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