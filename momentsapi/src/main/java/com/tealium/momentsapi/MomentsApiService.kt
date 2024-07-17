package com.tealium.momentsapi

import com.tealium.core.Module
import com.tealium.core.ModuleFactory
import com.tealium.core.Modules
import com.tealium.core.Tealium
import com.tealium.core.TealiumContext
import com.tealium.momentsapi.network.HttpClient
import com.tealium.momentsapi.network.NetworkClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.lang.Exception
import java.net.URL

/**
 * Interface defining methods for fetching engine responses.
 */
interface MomentsApi {
    /**
     * Fetches the engine response for the given engine ID.
     * @param engineId The ID of the engine to fetch the response for.
     * @param responseListener Listener for handling the response.
     */
    fun fetchEngineResponse(engineId: String, responseListener: ResponseListener<EngineResponse>)
}

/**
 * Service class providing implementation for the Moments API module.
 * @param context The Tealium context.
 * @param region The region used for calls to Moments API endpoint (Required).
 * @param networkClient The network client used for making HTTP requests (default is HttpClient).
 * @param backgroundScope The CoroutineScope used for background tasks (default is Dispatchers.IO).
 */
class MomentsApiService @JvmOverloads constructor(
    private val context: TealiumContext,
    private val region: MomentsApiRegion,
    private val networkClient: NetworkClient = HttpClient(context),
    private val backgroundScope: CoroutineScope = CoroutineScope(Dispatchers.IO)
) : Module, MomentsApi {

    override var enabled: Boolean = true
    override val name: String = MODULE_NAME

    private val engineUrl: String =
        "https://personalization-api.${region.value}.prod.tealiumapis.com/personalization/accounts/${context.config.accountName}/profiles/${context.config.profileName}/"

    private val referer = context.config.momentsApiReferer
        ?: "https://tags.tiqcdn.com/utag/${context.config.accountName}/${context.config.profileName}/${context.config.environment.environment}/mobile.html"

    override fun fetchEngineResponse(engineId: String, responseListener: ResponseListener<EngineResponse>) {
        backgroundScope.launch {
            networkClient.get(
                URL("$engineUrl/engines/$engineId/visitors/${context.visitorId}?ignoreTapid=true"),
                referer,
                object : ResponseListener<String> {
                    override fun success(data: String) {
                        try {
                            val json = JSONObject(data)
                            val visitorData = EngineResponse.fromJson(json)
                            responseListener.success(visitorData)
                        } catch (ex: Exception) {
                            responseListener.failure(ErrorCode.INVALID_JSON, ErrorCode.INVALID_JSON.message)
                        }
                    }

                    override fun failure(errorCode: ErrorCode, message: String) {
                        responseListener.failure(errorCode, message)
                    }
                })
        }
    }

    companion object : ModuleFactory {
        const val MODULE_NAME = "MomentsApi"
        const val MODULE_VERSION = BuildConfig.LIBRARY_VERSION

        override fun create(context: TealiumContext): Module {
            context.config.momentsApiRegion?.let { region ->
                return MomentsApiService(
                    context,
                    region
                )
            }
            throw Exception("MomentsApi must have a region assigned. Ensure you have set one on TealiumConfig.")
        }
    }
}

val Modules.MomentsApi: ModuleFactory
    get() = MomentsApiService

/**
 * Returns the MomentsApi module for this Tealium instance.
 */
val Tealium.momentsApi: MomentsApiService?
    get() = modules.getModule(MomentsApiService.MODULE_NAME) as? MomentsApiService