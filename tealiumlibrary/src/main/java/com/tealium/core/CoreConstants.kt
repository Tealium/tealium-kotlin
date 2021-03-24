@file:JvmName("Constants")

object CoreConstant {
    const val TEALIUM_EVENT_TYPE = "tealium_event_type"
    const val TEALIUM_EVENT = "tealium_event"
    const val LIBRARY_VERSION = "tealium_library_version"
    const val TRACE_ID = "cp.trace_id"
    const val TRACE_ID_QUERY_PARAM = "tealium_trace_id"
    const val LEAVE_TRACE_QUERY_PARAM = "leave_trace"
    const val KILL_VISITOR_SESSION = "kill_visitor_session"
    const val KILL_VISITOR_SESSION_EVENT_KEY = "event"
    const val DEEP_LINK_URL = "deep_link_url"
    const val DEEP_LINK_QUERY_PREFIX = "deep_link_param"
    const val REQUEST_UUID = "request_uuid"
}

object DispatchType {
    const val VIEW = "view"
    const val EVENT = "event"
    const val REMOTE_API = "remote_api"
}