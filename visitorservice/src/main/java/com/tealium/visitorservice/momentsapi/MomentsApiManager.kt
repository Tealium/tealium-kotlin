package com.tealium.visitorservice.momentsapi

import com.tealium.core.Logger
import com.tealium.core.TealiumContext
import com.tealium.core.network.ResourceRetriever
import com.tealium.visitorservice.BuildConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONException
import org.json.JSONObject
import java.net.URL

interface MomentsApiManager {
    fun fetchEngineResponse(engineId: String, handler: ResponseListener<EngineResponse>)
}

class MomentsManager(
    private val context: TealiumContext,
    private val networkClient: NetworkClient = HttpClient(context.config.application),
    private val backgroundScope: CoroutineScope = CoroutineScope(Dispatchers.IO)
) : MomentsApiManager {

    override fun fetchEngineResponse(engineId: String, handler: ResponseListener<EngineResponse>) {
        backgroundScope.launch {
            val urlString = generateMomentsApiUrl(engineId)
            networkClient.get(URL(urlString), object : ResponseListener<String> {
                override fun success(data: String) {
                    try {
                        val json = JSONObject(data)
                        val vp = EngineResponse.fromJson(json)
                        handler.success(vp)
                    } catch (ex: Exception) {
                        handler.failure(ErrorCode.INVALID_JSON, "Invalid JSON VisitorProfile")
                    }
                }

                override fun failure(errorCode: ErrorCode, message: String) {
                    handler.failure(errorCode, message)
                }
            })
        }
    }

    private suspend fun fetch(engineId: String): EngineResponse? {
        val retriever = ResourceRetriever(
            context.config,
            generateMomentsApiUrl(engineId),
            context.httpClient
        ).apply {
            maxRetries = 3
            useIfModifed = false
        }

        return retriever.fetch()?.let {
            try {
                val json = JSONObject(it)
                EngineResponse.fromJson(json)
            } catch (ex: JSONException) {
                Logger.qa(BuildConfig.TAG, "Exception parsing retrieved JSON.")
                null
            }
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

    companion object {
        const val PLACEHOLDER_REGION = "{{region}}"
        const val PLACEHOLDER_ACCOUNT = "{{account}}"
        const val PLACEHOLDER_PROFILE = "{{profile}}"
        const val PLACEHOLDER_ENGINE_ID = "{{engineId}}"
        const val PLACEHOLDER_VISITOR_ID = "{{visitorId}}"

        const val DEFAULT_VISITOR_SERVICE_TEMPLATE =
            "https://personalization-api.$PLACEHOLDER_REGION.prod.tealiumapis.com/personalization/accounts/$PLACEHOLDER_ACCOUNT/profiles/$PLACEHOLDER_PROFILE/engines/$PLACEHOLDER_ENGINE_ID/visitors/$PLACEHOLDER_VISITOR_ID?ignoreTapid=true"
    }
}