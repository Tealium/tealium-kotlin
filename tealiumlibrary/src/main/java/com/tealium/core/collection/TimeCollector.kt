package com.tealium.core.collection

import com.tealium.core.*
import com.tealium.core.persistence.getTimestampMilliseconds
import java.text.SimpleDateFormat
import java.util.*

interface TimeData {
    val timestamp : String
    val timestampLocal : String
    val timestampOffset : String
    val timestampUnix : Long
    val timestampUnixMilliseconds : Long
}

class TimeCollector : Collector, TimeData {

    override val name: String = "TIME_COLLECTOR"
    override var enabled: Boolean = true

    private val utcDateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.ROOT)
    internal val localDateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.ROOT)
    private val hourInMs: Long = 1000 * 60 * 60

    init {
        utcDateFormat.timeZone = TimeZone.getTimeZone("UTC")
    }

    override val timestamp: String
        get() = utcDateFormat.format(Date(timestampUnixMilliseconds))

    override val timestampLocal: String
        get() = localDateFormat.format(Date(timestampUnixMilliseconds))

    override val timestampOffset: String
        get() = String.format(Locale.ROOT, "%.0f", (TimeZone.getDefault().getOffset(timestampUnixMilliseconds)).toFloat().div(hourInMs))

    override val timestampUnix: Long
        get() = timestampUnixMilliseconds.div(1000)

    override val timestampUnixMilliseconds: Long
        get() = getTimestampMilliseconds()

    override suspend fun collect(): Map<String, Any> {
        return mapOf(
                TimeCollectorConstants.TIMESTAMP to timestamp,
                TimeCollectorConstants.TIMESTAMP_LOCAL to timestampLocal,
                TimeCollectorConstants.TIMESTAMP_OFFSET to timestampOffset,
                TimeCollectorConstants.TIMESTAMP_UNIX to timestampUnix,
                TimeCollectorConstants.TIMESTAMP_UNIX_MILLISECONDS to timestampUnixMilliseconds
        )
    }

    companion object : CollectorFactory {
        override fun create(context: TealiumContext): Collector {
            return TimeCollector()
        }
    }
}

val Collectors.Time : CollectorFactory
    get() = TimeCollector




