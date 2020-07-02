package com.tealium.visitorservice

import com.tealium.core.*
import com.tealium.core.network.ResourceRetriever
import com.tealium.tealiumlibrary.BuildConfig
import kotlin.properties.Delegates

interface VisitorServiceDelegate {
    fun didUpdate(visitorProfile: VisitorProfile)
}

/**
 * The VisitorService module is responsible for fetching updates to a VisitorProfile, and notifying
 * delegates that there have been updates.
 */
class VisitorService(private val context: TealiumContext,
                     var delegate: VisitorServiceDelegate? = null) : Module {

    override val name: String
        get() = "VISITOR_SERVICE"

    override var enabled: Boolean = true

    private val visitorId: String = context.visitorId

    private val urlString: String
        get() = context.config.overrideVisitorServiceUrl
                ?: "https://visitor-service.tealiumiq.com/${context.config.accountName}/${context.config.profileName}/${visitorId}"

    private val resourceRetriever: ResourceRetriever

    init {
        resourceRetriever = ResourceRetriever(context.config, urlString, context.httpClient)
        resourceRetriever.useIfModifed = false
    }

    private var visitorProfile: VisitorProfile by Delegates.observable(
            initialValue = VisitorProfile(),
            onChange = { _, _, new ->
                delegate?.didUpdate(new)
            }
    )

    /**
     * Retrieves the latest VisitorProfile. If there are updates then the delegate will be informed.
     */
    suspend fun requestVisitorProfile() {
        Logger.dev(BuildConfig.TAG, "Fetching visitor profile for $visitorId.")
        val json = resourceRetriever.fetch()
        json?.let {
            Logger.dev(BuildConfig.TAG, "Fetched visitor profile: $it.")
            visitorProfile = VisitorProfile.fromJson(it)
        }
    }

    companion object : ModuleFactory {
        const val MODULE_NAME = "VISITOR_SERVICE"

        override fun create(context: TealiumContext): Module {
            return VisitorService(context)
        }
    }
}

val Modules.VisitorService: ModuleFactory
    get() = com.tealium.visitorservice.VisitorService

/**
 * Returns the VisitorService module for this Tealium instance.
 */
val Tealium.visitorService: VisitorService?
    get() = modules.getModule(VisitorService.MODULE_NAME) as? VisitorService