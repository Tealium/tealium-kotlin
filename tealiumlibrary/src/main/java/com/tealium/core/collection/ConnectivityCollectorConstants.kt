@file:JvmName("Constants")

@Deprecated(
    "Constants have been moved.",
    ReplaceWith("Dispatch.Keys", "com.tealium.dispatcher.Dispatch")
)
object ConnectivityCollectorConstants {
    @Deprecated(
        "Constants have been moved.",
        ReplaceWith("Dispatch.Keys.CONNECTION_TYPE", "com.tealium.dispatcher.Dispatch")
    )
    const val CONNECTION_TYPE = "connection_type"

    @Deprecated(
        "Constants have been moved.",
        ReplaceWith("Dispatch.Keys.IS_CONNECTED", "com.tealium.dispatcher.Dispatch")
    )
    const val IS_CONNECTED = "device_connected"

    @Deprecated(
        "Constants have been moved.",
        ReplaceWith("Dispatch.Keys.CARRIER", "com.tealium.dispatcher.Dispatch")
    )
    const val CARRIER = "carrier"

    @Deprecated(
        "Constants have been moved.",
        ReplaceWith("Dispatch.Keys.CARRIER_ISO", "com.tealium.dispatcher.Dispatch")
    )
    const val CARRIER_ISO = "carrier_iso"

    @Deprecated(
        "Constants have been moved.",
        ReplaceWith("Dispatch.Keys.CARRIER_MCC", "com.tealium.dispatcher.Dispatch")
    )
    const val CARRIER_MCC = "carrier_mcc"

    @Deprecated(
        "Constants have been moved.",
        ReplaceWith("Dispatch.Keys.CARRIER_MNC", "com.tealium.dispatcher.Dispatch")
    )
    const val CARRIER_MNC = "carrier_mnc"
}
