package com.tealium.core.collection

import com.tealium.core.*
import com.tealium.tealiumlibrary.BuildConfig

interface TealiumData {
    val account: String
    val profile: String
    val environment: String
    val dataSource: String?
}

class TealiumCollector(private val context: TealiumContext) : Collector, TealiumData {

    override val name: String
        get() = "TEALIUM_COLLECTOR"
    override var enabled: Boolean = true

    private val config = context.config
    override val account: String = config.accountName
    override val profile: String = config.profileName
    override val environment: String = config.environment.environment
    override val dataSource: String? = config.dataSourceId

    override suspend fun collect(): Map<String, Any> {
        return mapOf(
                TealiumCollectorConstants.TEALIUM_ACCOUNT to account,
                TealiumCollectorConstants.TEALIUM_PROFILE to profile,
                TealiumCollectorConstants.TEALIUM_ENVIRONMENT to environment,
                TealiumCollectorConstants.TEALIUM_DATASOURCE_ID to (dataSource ?: ""),
                TealiumCollectorConstants.TEALIUM_VISITOR_ID to context.visitorId,
                TealiumCollectorConstants.TEALIUM_LIBRARY_NAME_KEY_NAME to BuildConfig.LIBRARY_NAME,
                TealiumCollectorConstants.TEALIUM_LIBRARY_VERSION_KEY_NAME to BuildConfig.LIBRARY_VERSION
        )
    }

    companion object: CollectorFactory {
        override fun create(context: TealiumContext): Collector {
            return TealiumCollector(context)
        }
    }
}

val Collectors.Tealium: CollectorFactory
    get() = TealiumCollector