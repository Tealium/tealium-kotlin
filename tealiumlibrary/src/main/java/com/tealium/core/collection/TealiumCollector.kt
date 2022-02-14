package com.tealium.core.collection

import com.tealium.core.*
import com.tealium.dispatcher.Dispatch
import com.tealium.tealiumlibrary.BuildConfig
import java.security.SecureRandom
import java.util.*
import kotlin.math.abs

interface TealiumData {
    val account: String
    val profile: String
    val environment: String
    val dataSource: String?
    val random: String
}

class TealiumCollector(private val context: TealiumContext) : Collector, TealiumData {

    override val name: String
        get() = "TealiumCollector"
    override var enabled: Boolean = true
    private val secureRandom = SecureRandom()

    private val config = context.config
    override val account: String = config.accountName
    override val profile: String = config.profileName
    override val environment: String = config.environment.environment
    override val dataSource: String? = config.dataSourceId
    override val random: String
        get() {
            val rand = secureRandom.nextLong() % 10000000000000000L
            return String.format(Locale.ROOT, "%016d", abs(rand));
        }

    override suspend fun collect(): Map<String, Any> {
        return mapOf(
                Dispatch.Keys.TEALIUM_ACCOUNT to account,
                Dispatch.Keys.TEALIUM_PROFILE to profile,
                Dispatch.Keys.TEALIUM_ENVIRONMENT to environment,
                Dispatch.Keys.TEALIUM_DATASOURCE_ID to (dataSource ?: ""),
                Dispatch.Keys.TEALIUM_VISITOR_ID to context.visitorId,
                Dispatch.Keys.TEALIUM_LIBRARY_NAME to BuildConfig.LIBRARY_NAME,
                Dispatch.Keys.TEALIUM_LIBRARY_VERSION to BuildConfig.LIBRARY_VERSION,
                Dispatch.Keys.TEALIUM_RANDOM to random
        )
    }

    companion object : CollectorFactory {
        override fun create(context: TealiumContext): Collector {
            return TealiumCollector(context)
        }
    }
}

val Collectors.Tealium: CollectorFactory
    get() = TealiumCollector