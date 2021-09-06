package com.tealium.dispatcher

import com.tealium.core.JsonUtils
import com.tealium.core.Session
import org.json.JSONStringer

interface Dispatch {

    val id: String
    var timestamp: Long?

    fun payload(): Map<String, Any>

    fun addAll(data: Map<String, Any>)

    operator fun get(key: String): Any? {
        return payload()[key]
    }

    fun toJsonString(): String {
        return JsonUtils.jsonFor(payload()).toString()
    }

    @Deprecated("This feature will no longer be supported - prefer to use JsonUtils")
    fun encode(jsonStringer: JSONStringer, key: String, value: Any) {
        (value as? Any)?.let {
            jsonStringer.key(key)
            jsonStringer.value(it)
        }
    }

    @Deprecated("This feature will no longer be supported - prefer to use JsonUtils")
    fun encodeCollection(jsonStringer: JSONStringer, key: String, value: Any) {
        jsonStringer.key(key)
        jsonStringer.array()
        (value as? Array<*>)?.forEach {
            jsonStringer.value(it)
        }
        (value as? List<*>)?.forEach {
            jsonStringer.value(it)
        }
        jsonStringer.endArray()
    }

    @Deprecated("This feature will no longer be supported - prefer to use JsonUtils")
    fun encodeString(jsonStringer: JSONStringer, key: String, value: Any) {
        jsonStringer.key(key)
        jsonStringer.value(value.toString())
    }

    object Keys {
        const val TEALIUM_EVENT_TYPE = "tealium_event_type"
        const val TEALIUM_EVENT = "tealium_event"
        const val LIBRARY_VERSION = "library_version"
        const val TRACE_ID = "cp.trace_id"
        const val TEALIUM_TRACE_ID = "tealium_trace_id"
        const val EVENT = "event"
        const val DEEP_LINK_URL = "deep_link_url"
        const val DEEP_LINK_QUERY_PREFIX = "deep_link_param"
        const val REQUEST_UUID = "request_uuid"

        // AppCollector
        const val APP_UUID = "app_uuid"
        const val APP_RDNS = "app_rdns"
        const val APP_NAME = "app_name"
        const val APP_BUILD = "app_build"
        const val APP_VERSION = "app_version"
        const val APP_MEMORY_USAGE = "app_memory_usage"

        // ConnectivityCollector
        const val CONNECTION_TYPE = "connection_type"
        const val IS_CONNECTED = "device_connected"
        const val CARRIER = "carrier"
        const val CARRIER_ISO = "carrier_iso"
        const val CARRIER_MCC = "carrier_mcc"
        const val CARRIER_MNC = "carrier_mnc"

        // DeviceCollector
        const val DEVICE = "device"
        const val DEVICE_MODEL = "device_model"
        const val DEVICE_MANUFACTURER = "device_manufacturer"
        const val DEVICE_ARCHITECTURE = "device_architecture"
        const val DEVICE_CPU_TYPE = "device_cputype"
        const val DEVICE_RESOLUTION = "device_resolution"
        const val DEVICE_LOGICAL_RESOLUTION = "device_logical_resolution"
        const val DEVICE_RUNTIME = "device_android_runtime"
        const val DEVICE_ORIGIN = "origin"
        const val DEVICE_PLATFORM = "platform"
        const val DEVICE_OS_NAME = "os_name"
        const val DEVICE_OS_BUILD = "device_os_build"
        const val DEVICE_OS_VERSION = "device_os_version"
        const val DEVICE_AVAILABLE_SYSTEM_STORAGE = "device_free_system_storage"
        const val DEVICE_AVAILABLE_EXTERNAL_STORAGE = "device_free_external_storage"
        const val DEVICE_ORIENTATION = "device_orientation"
        const val DEVICE_LANGUAGE = "device_language"

        // ModuleCollector
        const val ENABLED_MODULES = "enabled_modules"
        const val ENABLED_MODULES_VERSIONS = "enabled_modules_versions"

        // SessionCollector
        const val TEALIUM_SESSION_ID = Session.KEY_SESSION_ID

        // TealiumCollector
        const val TEALIUM_ACCOUNT = "tealium_account"
        const val TEALIUM_PROFILE = "tealium_profile"
        const val TEALIUM_ENVIRONMENT = "tealium_environment"
        const val TEALIUM_DATASOURCE_ID = "tealium_datasource"
        const val TEALIUM_VISITOR_ID = "tealium_visitor_id"
        const val TEALIUM_LIBRARY_NAME = "tealium_library_name"
        const val TEALIUM_LIBRARY_VERSION = "tealium_library_version"

        // TimeCollector
        const val TIMESTAMP = "timestamp"
        const val TIMESTAMP_LOCAL = "timestamp_local"
        const val TIMESTAMP_OFFSET = "timestamp_offset"
        const val TIMESTAMP_UNIX = "timestamp_unix"
        const val TIMESTAMP_UNIX_MILLISECONDS = "timestamp_unix_milliseconds"

        // ConsentManager
        const val CONSENT_STATUS = "status"
        const val CONSENT_CATEGORIES = "categories"
        const val CONSENT_LAST_STATUS_UPDATE = "last_updated"

        // TimedEvents
        const val TIMED_EVENT_NAME = "timed_event_name"
        const val TIMED_EVENT_START = "timed_event_start"
        const val TIMED_EVENT_END = "timed_event_end"
        const val TIMED_EVENT_DURATION = "timed_event_duration"
    }
}