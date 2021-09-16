@file:JvmName("Constants")

@Deprecated(
    "Constants have been moved.",
    ReplaceWith("Dispatch.Keys", "com.tealium.dispatcher.Dispatch")
)
object TimeCollectorConstants {
    @Deprecated(
        "Constants have been moved.",
        ReplaceWith("Dispatch.Keys.TIMESTAMP", "com.tealium.dispatcher.Dispatch")
    )
    const val TIMESTAMP = "timestamp"

    @Deprecated(
        "Constants have been moved.",
        ReplaceWith("Dispatch.Keys.TIMESTAMP_LOCAL", "com.tealium.dispatcher.Dispatch")
    )
    const val TIMESTAMP_LOCAL = "timestamp_local"

    @Deprecated(
        "Constants have been moved.",
        ReplaceWith("Dispatch.Keys.TIMESTAMP_OFFSET", "com.tealium.dispatcher.Dispatch")
    )
    const val TIMESTAMP_OFFSET = "timestamp_offset"

    @Deprecated(
        "Constants have been moved.",
        ReplaceWith("Dispatch.Keys.TIMESTAMP_UNIX", "com.tealium.dispatcher.Dispatch")
    )
    const val TIMESTAMP_UNIX = "timestamp_unix"

    @Deprecated(
        "Constants have been moved.",
        ReplaceWith("Dispatch.Keys.TIMESTAMP_UNIX_MILLISECONDS", "com.tealium.dispatcher.Dispatch")
    )
    const val TIMESTAMP_UNIX_MILLISECONDS = "timestamp_unix_milliseconds"
}
