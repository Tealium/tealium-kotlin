package com.tealium.core.collection

import com.tealium.core.*
import com.tealium.core.persistence.getTimestampMilliseconds
import com.tealium.dispatcher.Dispatch
import com.tealium.test.OpenForTesting
import java.text.SimpleDateFormat
import java.util.*

interface TimeData {
    val timestamp: String
    val timestampLocal: String
    val timestampOffset: String
    val timestampUnix: Long
    val timestampUnixMilliseconds: Long
    val timestampEpoch: Long
}

@OpenForTesting
class TimeCollector : Collector, TimeData {

    override val name: String = "TimeCollector"
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
        get() = String.format(
                Locale.ROOT,
                "%.0f",
                (TimeZone.getDefault().getOffset(timestampUnixMilliseconds)).toFloat().div(hourInMs)
        )

    override val timestampUnix: Long
        get() = timestampUnixMilliseconds.div(1000)

    override val timestampUnixMilliseconds: Long
        get() = getTimestampMilliseconds()

    override val timestampEpoch: Long
        get() = getTimestampMilliseconds().div(1000)

    override suspend fun collect(): Map<String, Any> {
        return mapOf(
                Dispatch.Keys.TIMESTAMP to timestamp,
                Dispatch.Keys.TIMESTAMP_LOCAL to timestampLocal,
                Dispatch.Keys.TIMESTAMP_OFFSET to timestampOffset,
                Dispatch.Keys.TIMESTAMP_UNIX to timestampUnix,
                Dispatch.Keys.TIMESTAMP_UNIX_MILLISECONDS to timestampUnixMilliseconds,
                Dispatch.Keys.TIMESTAMP_EPOCH to timestampEpoch
        )
    }

    companion object : CollectorFactory {
        @Volatile
        private var instance: Collector? = null

        override fun create(context: TealiumContext): Collector = instance ?: synchronized(this) {
            instance ?: TimeCollector().also { instance = it }
        }
    }
}

val Collectors.Time: CollectorFactory
    get() = TimeCollector