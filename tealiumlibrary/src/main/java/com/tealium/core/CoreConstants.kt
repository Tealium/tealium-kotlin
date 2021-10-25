@file:JvmName("Constants")

@Deprecated(
    "CoreConstants have been moved."
)
object CoreConstant {
    @Deprecated(
        "Constants have been moved.",
        ReplaceWith("Dispatch.Keys.TEALIUM_EVENT_TYPE", "com.tealium.dispatcher.Dispatch")
    )
    const val TEALIUM_EVENT_TYPE = "tealium_event_type"

    @Deprecated(
        "Constants have been moved.",
        ReplaceWith("Dispatch.Keys.TEALIUM_EVENT", "com.tealium.dispatcher.Dispatch")
    )
    const val TEALIUM_EVENT = "tealium_event"

    @Deprecated(
        "Constants have been moved.",
        ReplaceWith("Dispatch.Keys.LIBRARY_VERSION", "com.tealium.dispatcher.Dispatch")
    )
    const val LIBRARY_VERSION = "library_version"

    @Deprecated(
        "Constants have been moved.",
        ReplaceWith("Dispatch.Keys.TRACE_ID", "com.tealium.dispatcher.Dispatch")
    )
    const val TRACE_ID = "cp.trace_id"

    @Deprecated(
        "Constants have been moved.",
        ReplaceWith("Dispatch.Keys.REQUEST_UUID", "com.tealium.dispatcher.Dispatch")
    )
    const val REQUEST_UUID = "request_uuid"

    @Deprecated(
        "Constants have been moved.",
        ReplaceWith("Dispatch.Keys.EVENT", "com.tealium.dispatcher.Dispatch")
    )
    const val KILL_VISITOR_SESSION_EVENT_KEY = "event"

    @Deprecated(
        "Constants have been moved.",
        ReplaceWith("Dispatch.Keys.DEEP_LINK_URL", "com.tealium.dispatcher.Dispatch")
    )
    const val DEEP_LINK_URL = "deep_link_url"

    @Deprecated(
        "Constants have been moved.",
        ReplaceWith("Dispatch.Keys.DEEP_LINK_QUERY_PREFIX", "com.tealium.dispatcher.Dispatch")
    )
    const val DEEP_LINK_QUERY_PREFIX = "deep_link_param"

    @Deprecated(
        "Constants have been moved.",
        ReplaceWith("DeepLinkHandler.TRACE_ID_QUERY_PARAM", "com.tealium.core.DeepLinkHandler")
    )
    const val TRACE_ID_QUERY_PARAM = "tealium_trace_id"

    @Deprecated(
        "Constants have been moved.",
        ReplaceWith("DeepLinkHandler.LEAVE_TRACE_QUERY_PARAM", "com.tealium.core.DeepLinkHandler")
    )
    const val LEAVE_TRACE_QUERY_PARAM = "leave_trace"

    @Deprecated(
        "Constants have been moved.",
        ReplaceWith("DeepLinkHandler.KILL_VISITOR_SESSION", "com.tealium.core.DeepLinkHandler")
    )
    const val KILL_VISITOR_SESSION = "kill_visitor_session"
}

@Deprecated(
    "DispatchType has been moved.",
    ReplaceWith("DispatchType", "com.tealium.core.DispatchType")
)
object DispatchType {
    @Deprecated(
        "DispatchType has been moved.",
        ReplaceWith("com.tealium.core.DispatchType.VIEW", "com.tealium.core.DispatchType")
    )
    const val VIEW = "view"

    @Deprecated(
        "DispatchType has been moved.",
        ReplaceWith("com.tealium.core.DispatchType.EVENT", "com.tealium.core.DispatchType")
    )
    const val EVENT = "event"
    @Deprecated(
        "DispatchType has been moved.",
        ReplaceWith("com.tealium.core.DispatchType.REMOTE_API", "com.tealium.core.DispatchType")
    )
    const val REMOTE_API = "remote_api"
}