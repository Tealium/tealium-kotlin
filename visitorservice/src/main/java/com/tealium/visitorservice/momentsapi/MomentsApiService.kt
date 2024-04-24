package com.tealium.visitorservice.momentsapi

import com.tealium.core.Module
import com.tealium.core.ModuleFactory
import com.tealium.core.Modules
import com.tealium.core.Tealium
import com.tealium.core.TealiumContext
import com.tealium.visitorservice.BuildConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.lang.Exception
import java.net.URL

class MomentsApiService @JvmOverloads constructor(
    private val context: TealiumContext,
    private val networkClient: NetworkClient = HttpClient(context.config.application),
    private val backgroundScope: CoroutineScope = CoroutineScope(Dispatchers.IO)
) : Module {

    override var enabled: Boolean = true // change
    override val name: String = MODULE_NAME

    fun fetchEngineResponse(engineId: String, responseListener: ResponseListener<EngineResponse>) {
        fetchResponse(engineId, responseListener)
    }

    private fun fetchResponse(engineId: String, handler: ResponseListener<EngineResponse>) {
        backgroundScope.launch {
            val urlString = generateMomentsApiUrl(engineId)
            networkClient.get(URL(urlString), object : ResponseListener<String> {
                override fun success(data: String) {
                    try {
                        val json = JSONObject(data)
                        val visitorData = EngineResponse.fromJson(json)
                        handler.success(visitorData)
                    } catch (ex: Exception) {
                        handler.failure(ErrorCode.INVALID_JSON, "Invalid JSON EngineResponse")
                    }
                }

                override fun failure(errorCode: ErrorCode, message: String) {
                    handler.failure(errorCode, message)
                }
            })
        }
    }

    internal fun generateMomentsApiUrl(engineId: String): String {
        return DEFAULT_VISITOR_SERVICE_TEMPLATE
            .replace(PLACEHOLDER_ACCOUNT, context.config.accountName)
            .replace(PLACEHOLDER_REGION, context.config.momentsApiRegion!!.value)
            .replace(PLACEHOLDER_ACCOUNT, context.config.accountName)
            .replace(PLACEHOLDER_PROFILE, context.config.profileName)
            .replace(PLACEHOLDER_ENGINE_ID, engineId)
            .replace(PLACEHOLDER_VISITOR_ID, context.visitorId)
    }

    companion object : ModuleFactory {
        const val MODULE_NAME = "MomentsApi"
        const val MODULE_VERSION = BuildConfig.LIBRARY_VERSION

        const val PLACEHOLDER_REGION = "{{region}}"
        const val PLACEHOLDER_ACCOUNT = "{{account}}"
        const val PLACEHOLDER_PROFILE = "{{profile}}"
        const val PLACEHOLDER_ENGINE_ID = "{{engineId}}"
        const val PLACEHOLDER_VISITOR_ID = "{{visitorId}}"

        const val DEFAULT_VISITOR_SERVICE_TEMPLATE =
            "https://personalization-api.$PLACEHOLDER_REGION.prod.tealiumapis.com/personalization/accounts/$PLACEHOLDER_ACCOUNT/profiles/$PLACEHOLDER_PROFILE/engines/$PLACEHOLDER_ENGINE_ID/visitors/$PLACEHOLDER_VISITOR_ID?ignoreTapid=true"

        override fun create(context: TealiumContext): Module {
            context.config.momentsApiRegion?.let {
                return MomentsApiService(context)
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