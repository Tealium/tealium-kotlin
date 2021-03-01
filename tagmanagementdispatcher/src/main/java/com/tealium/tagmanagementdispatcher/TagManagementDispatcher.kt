package com.tealium.tagmanagementdispatcher

import android.os.Build
import android.webkit.*
import com.tealium.core.*
import com.tealium.core.consent.ConsentManagementPolicy
import com.tealium.core.consent.UserConsentPreferences
import com.tealium.core.messaging.AfterDispatchSendCallbacks
import com.tealium.core.messaging.DispatchReadyListener
import com.tealium.core.validation.DispatchValidator
import com.tealium.core.messaging.*
import com.tealium.core.network.Connectivity
import com.tealium.core.network.ConnectivityRetriever
import com.tealium.dispatcher.Dispatch
import com.tealium.dispatcher.Dispatcher
import com.tealium.dispatcher.TealiumEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * The TagManagementDispatcher allows Tealium IQ to be used within the native app.
 */
class TagManagementDispatcher(private val context: TealiumContext,
                              private val afterDispatchSendCallbacks: AfterDispatchSendCallbacks,
                              connectivity: Connectivity = ConnectivityRetriever.getInstance(context.config.application)) :
        Dispatcher,
        DispatchReadyListener,
        EvaluateJavascriptListener,
        DispatchValidator,
        UserConsentPreferencesUpdatedListener {

    val urlString: String
        get() = context.config.overrideTagManagementUrl
                ?: "https://tags.tiqcdn.com/utag/" +
                "${context.config.accountName}/" +
                "${context.config.profileName}/" +
                "${context.config.environment.environment}/mobile.html?" +
                "${DeviceCollectorConstants.DEVICE_PLATFORM}=android" +
                "&${DeviceCollectorConstants.DEVICE_OS_VERSION}=${Build.VERSION.RELEASE}" +
                "&${CoreConstant.LIBRARY_VERSION}=${BuildConfig.VERSION_NAME}" +
                "&sdk_session_count=true"

    private val scope = CoroutineScope(Dispatchers.Main)
    internal var webViewLoader = WebViewLoader(context, urlString, afterDispatchSendCallbacks, connectivityRetriever = connectivity)

    fun callRemoteCommandTags(dispatch: Dispatch) {
        val remoteCommandScript = "utag.track(\"remote_api\", ${dispatch.toJsonString()})"
        onEvaluateJavascript(remoteCommandScript)
    }

    override suspend fun onBatchDispatchSend(dispatches: List<Dispatch>) {
        dispatches.forEach {
            onDispatchSend(it)
        }
    }

    override suspend fun onDispatchSend(dispatch: Dispatch) {
        track(dispatch)
    }

    private fun track(dispatch: Dispatch) {
        val callType = dispatch.payload()[CoreConstant.TEALIUM_EVENT_TYPE]
        var javascriptCall = ""

        javascriptCall = callType?.let {
            when (it) {
                TagManagementConstants.EVENT -> "utag.track(\"link\", ${dispatch.toJsonString()})"
                else -> "utag.track(\"$it\", ${dispatch.toJsonString()})"
            }
        } ?: "utag.track(\"link\", ${dispatch.toJsonString()})"

        onEvaluateJavascript(javascriptCall)

        CookieManager.getInstance().apply {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                flush()
            }
        }
    }

    override fun onDispatchReady(dispatch: Dispatch) {
        when (webViewLoader.webViewStatus.get()) {
            PageStatus.LOADED_SUCCESS -> {
                callRemoteCommandTags(dispatch)
                return
            }
            PageStatus.LOADED_ERROR -> {
                if (webViewLoader.isTimedOut()) {
                    webViewLoader.loadUrlToWebView()
                }
            }
            PageStatus.INIT -> {
                Logger.qa(BuildConfig.TAG, "WebView not loaded yet.")
                webViewLoader.loadUrlToWebView()
            }
            PageStatus.LOADING -> {
                Logger.qa(BuildConfig.TAG, "WebView loading.")
            }
        }
    }

    override fun onEvaluateJavascript(js: String) {
        if (webViewLoader.webViewStatus.get() != PageStatus.LOADED_SUCCESS) {
            return
        }

        val script: String = if (js.startsWith("javascript:")) js.substring("javascript:".length) else js
        scope.launch {
            try {
                webViewLoader.webView.evaluateJavascript(script, null)
            } catch (t: Throwable) {
                Logger.prod(BuildConfig.TAG, t.localizedMessage)
            }
        }
    }

    override fun shouldQueue(dispatch: Dispatch?): Boolean {
        return webViewLoader.webViewStatus.get() != PageStatus.LOADED_SUCCESS
    }

    override fun shouldDrop(dispatch: Dispatch): Boolean {
        return false
    }

    override fun onUserConsentPreferencesUpdated(userConsentPreferences: UserConsentPreferences, policy: ConsentManagementPolicy) {
        if (!policy.cookieUpdateRequired) return

        val dispatch = TealiumEvent(policy.cookieUpdateEventName, policy.policyStatusInfo())
        dispatch.addAll(mapOf(CoreConstant.TEALIUM_EVENT_TYPE to policy.cookieUpdateEventName))

        track(dispatch)
    }

    companion object : DispatcherFactory {
        const val MODULE_NAME = "TAG_MANAGEMENT_DISPATCHER"

        override fun create(context: TealiumContext, callbacks: AfterDispatchSendCallbacks): Dispatcher {
            return TagManagementDispatcher(context, callbacks)
        }
    }

    override val name = MODULE_NAME
    override var enabled: Boolean = true
}

val com.tealium.core.Dispatchers.TagManagement: DispatcherFactory
    get() = TagManagementDispatcher