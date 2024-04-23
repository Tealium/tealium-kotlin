package com.tealium.visitorservice.momentsapi

enum class ErrorCode(val value: Int) {
    BAD_REQUEST(400 ),
    ENGINE_NOT_ENABLED(403),
    VISITOR_NOT_FOUND(404),
    NOT_CONNECTED (0),
    INVALID_JSON (1),
    UNKNOWN_ERROR(2)
}