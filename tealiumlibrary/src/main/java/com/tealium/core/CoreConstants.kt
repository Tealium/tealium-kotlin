@file:JvmName("Constants")

object CoreConstant {
    const val TEALIUM_EVENT_TYPE = "tealium_event_type"
    const val TEALIUM_EVENT = "tealium_event"
    const val TEALIUM_LIBRARY_NAME = "tealium_library_name"
    const val TEALIUM_LIBRARY_VERSION = "tealium_library_version"
    const val LIBRARY_VERSION = "library_version"
    const val TRACE_ID = "cp.trace_id"
    const val TRACE_ID_QUERY_PARAM = "tealium_trace_id"
    const val LEAVE_TRACE_QUERY_PARAM = "leave_trace"
    const val KILL_VISITOR_SESSION = "kill_visitor_session"
    const val KILL_VISITOR_SESSION_EVENT_KEY = "event"
    const val DEEP_LINK_URL = "deep_link_url"
    const val DEEP_LINK_QUERY_PREFIX = "deep_link_param"
}

object DispatchType {
    const val VIEW = "view"
    const val EVENT = "event"
}