package com.tealium.tagmanagementdispatcher

import android.annotation.TargetApi
import android.net.Uri
import android.net.http.SslError
import android.os.Build
import android.os.SystemClock
import android.view.View
import android.webkit.*
import com.tealium.core.*
import com.tealium.core.messaging.*
import com.tealium.core.network.Connectivity
import com.tealium.core.network.ConnectivityRetriever
import com.tealium.core.settings.LibrarySettings
import com.tealium.remotecommands.RemoteCommand
import com.tealium.remotecommands.RemoteCommandRequest
import kotlinx.coroutines.*
import kotlinx.coroutines.Dispatchers
import org.json.JSONObject
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

class WebViewLoader(
    private val context: TealiumContext,
    private val urlString: String,
    private val afterDispatchSendCallbacks: AfterDispatchSendCallbacks,
    private val connectivityRetriever: Connectivity = ConnectivityRetriever.getInstance(context.config.application),
    private val webViewProvider: () -> WebView = { WebView(context.config.application) }
) : LibrarySettingsUpdatedListener,
    SessionStartedListener,
    QueryParametersUpdatedListener {

    val webViewStatus = AtomicReference<PageStatus>(PageStatus.INIT)
    var lastUrlLoadTimestamp = 0L
    private var isWifiOnlySending = false
    private var timeoutInterval = -1
    private val scope = CoroutineScope(Dispatchers.Main)
    private val backgroundScope = CoroutineScope(Dispatchers.IO)
    private var sessionId: Long = INVALID_SESSION_ID
    private val shouldRegisterSession = AtomicBoolean(false)
    private val webViewCreationRetries = 3
    private val sessionCountingEnabled: Boolean = context.config.sessionCountingEnabled ?: true
    private var queryParams: Map<String, List<String>> = fetchQueryParams()

    @Volatile
    private var webViewCreationErrorCount = 0

    @Volatile
    lateinit var webView: WebView

    internal val webViewClient = object : WebViewClient() {
        override fun onPageFinished(view: WebView?, url: String?) {
            super.onPageFinished(view, url)

            lastUrlLoadTimestamp = SystemClock.elapsedRealtime()

            if (PageStatus.LOADED_ERROR == webViewStatus.get()) {
                webViewStatus.set(PageStatus.LOADED_ERROR)
                Logger.dev(BuildConfig.TAG, "Error loading URL $url in WebView $view")
                return
            }
            webViewStatus.set(PageStatus.LOADED_SUCCESS)

            registerNewSessionIfNeeded(sessionId)
            context.events.send(ValidationChangedMessenger())

            // Run JS evaluation here
            view?.loadUrl(
                "javascript:(function(){\n" +
                        "    var payload = {};\n" +
                        "    try {\n" +
                        "        var ts = new RegExp(\"ut[0-9]+\\.[0-9]+\\.[0-9]{12}\").exec(document.childNodes[0].textContent)[0];\n" +
                        "        ts = ts.substring(ts.length - 12, ts.length);\n" +
                        "        var y = ts.substring(0, 4);\n" +
                        "        var mo = ts.substring(4, 6);\n" +
                        "        var d = ts.substring(6, 8);\n" +
                        "        var h = ts.substring(8, 10);\n" +
                        "        var mi = ts.substring(10, 12);\n" +
                        "        var t = Date.from(y+'/'+mo+'/'+d+' '+h+':'+mi+' UTC');\n" +
                        "        if(!isNaN(t)){\n" +
                        "            payload.published = t;\n" +
                        "        }\n" +
                        "    } catch(e) {    }\n" +
                        "    var f=document.cookie.indexOf('trace_id=');\n" +
                        "    if(f>=0){\n" +
                        "        payload.trace_id = document.cookie.substring(f+9).split(';')[0];\n" +
                        "    }\n" +
                        "})()"
            )
        }

        override fun onLoadResource(view: WebView?, url: String?) {
            super.onLoadResource(view, url)
            Logger.dev(BuildConfig.TAG, "Loaded Resource: $url")
        }

        @TargetApi(Build.VERSION_CODES.LOLLIPOP)
        override fun onReceivedHttpError(
            view: WebView?,
            request: WebResourceRequest?,
            errorResponse: WebResourceResponse?
        ) {
            super.onReceivedHttpError(view, request, errorResponse)

            // Main resource failed to load; set status to error.
            request?.let {
                if (it.url.toString().startsWith(urlString)) {
                    webViewStatus.set(PageStatus.LOADED_ERROR)
                }
            }

            errorResponse?.let { error ->
                request?.let { req ->
                    Logger.prod(
                        BuildConfig.TAG,
                        "Received http error with ${req.url}: ${error.statusCode}: ${error.reasonPhrase}"
                    )
                }
            }
        }

        @TargetApi(Build.VERSION_CODES.M)
        override fun onReceivedError(
            view: WebView?,
            request: WebResourceRequest?,
            error: WebResourceError?
        ) {
            error?.let {
                onReceivedError(
                    view,
                    it.errorCode,
                    it.description.toString(),
                    request?.url.toString()
                )
            }
            super.onReceivedError(view, request, error)
        }

        // deprecated in API 23, but still supporting API level
        override fun onReceivedError(
            view: WebView?,
            errorCode: Int,
            description: String?,
            failingUrl: String?
        ) {
            failingUrl?.let {
                if (isFavicon(it)) {
                    return
                }
                if (isAboutScheme(it)) {
                    return
                }
            }

            super.onReceivedError(view, errorCode, description, failingUrl)

            if (PageStatus.LOADED_ERROR == webViewStatus.getAndSet(PageStatus.LOADED_ERROR)) {
                // error already occurred
                return
            }

            lastUrlLoadTimestamp = SystemClock.uptimeMillis()
            Logger.prod(
                BuildConfig.TAG, "Received err: {\n" +
                        "\tcode: $errorCode,\n" +
                        "\tdesc:\"${description?.replace("\"", "\\\"")}\",\n" +
                        "\turl:\"$failingUrl\"\n" +
                        "}"
            )
        }

        override fun shouldInterceptRequest(view: WebView?, url: String?): WebResourceResponse? {
            url?.let {
                if (!isFavicon(it)) {
                    return null
                }
            }

            return WebResourceResponse("image/png", null, null)
        }

        @TargetApi(Build.VERSION_CODES.LOLLIPOP)
        override fun shouldInterceptRequest(
            view: WebView?,
            request: WebResourceRequest?
        ): WebResourceResponse? {
            return shouldInterceptRequest(view, request?.url.toString())
        }

        override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
            url?.let {
                if (url.startsWith(TagManagementRemoteCommand.PREFIX)) {
                    backgroundScope.launch {
                        afterDispatchSendCallbacks.sendRemoteCommand(
                            RemoteCommandRequest(
                                createResponseHandler(),
                                url
                            )
                        )
                    }
                }
            }
            return true
        }

        @TargetApi(Build.VERSION_CODES.LOLLIPOP)
        override fun shouldOverrideUrlLoading(
            view: WebView?,
            request: WebResourceRequest?
        ): Boolean {
            return shouldOverrideUrlLoading(view, request?.url.toString())
        }

        override fun onReceivedSslError(
            view: WebView?,
            handler: SslErrorHandler?,
            error: SslError?
        ) {
            webViewStatus.set(PageStatus.LOADED_ERROR)

            view?.let {
                Logger.dev(BuildConfig.TAG, "Received SSL Error in WebView $it (${it.url}): $error")
            }
            super.onReceivedSslError(view, handler, error)
        }

        override fun onRenderProcessGone(
            view: WebView?,
            detail: RenderProcessGoneDetail?
        ): Boolean {
            view?.destroy()
            webViewStatus.set(PageStatus.LOADED_ERROR)
            initializeWebView()
            return true
        }
    }

    init {
        context.events.subscribe(this)
        initializeWebView()

    }

    private fun decorateUrlParams(urlString: String): String {
        if (queryParams.isEmpty()) {
            return urlString
        }
        val uriBuilder = Uri.parse(urlString).buildUpon()
        queryParams.forEach { entry ->
            entry.value.forEach { value ->
                uriBuilder.appendQueryParameter(entry.key, value)
            }
        }

        return uriBuilder.build().toString()
    }

    private fun fetchQueryParams(): Map<String, List<String>> {
        val currentParams = mutableMapOf<String, List<String>>()
        context.tealium.modules.getModulesForType(Module::class.java)
            .filterIsInstance(QueryParameterProvider::class.java).forEach { provider ->
                provider.provideParameters().let { params ->
                    if (params.isNotEmpty()) {
                        currentParams.putAll(params)
                    }
                }
            }
        return currentParams.toMap()
    }

    private fun initializeWebView() {
        scope.launch {
            if (hasReachedMaxErrors()) return@launch

            try {
                webView = webViewProvider()
            } catch (ex: Exception) {
                webViewCreationErrorCount++
                Logger.qa(BuildConfig.TAG, "Exception whilst creating the WebView: ${ex.message}")
                Logger.qa(BuildConfig.TAG, ex.stackTraceToString())
                context.events.send(WebViewExceptionMessenger(ex))
                return@launch
            }
            webView.let { view ->
                view.settings.run {
                    databaseEnabled = true
                    javaScriptEnabled = true
                    domStorageEnabled = true
                    setAppCacheEnabled(true)
                    setAppCachePath(context.config.tealiumDirectory.absolutePath)
                }

                // unrendered webview, disable hardware acceleration
                view.setLayerType(View.LAYER_TYPE_SOFTWARE, null)
                view.webChromeClient = WebChromeClientLoader()
                view.webViewClient = webViewClient
            }

            loadUrlToWebView()
            enableCookieManager()
        }
    }

    private fun createResponseHandler(): RemoteCommand.ResponseHandler {
        return object : RemoteCommand.ResponseHandler {
            override fun onHandle(response: RemoteCommand.Response?) {
                var js = ""
                response?.id?.let {
                    response.body?.let {
                        js = "try {" +
                                "	utag.mobile.remote_api.response[\"${response.commandId}\"][\"${response.id}\"](${response.status}, ${
                                    JSONObject.quote(
                                        response.body
                                    )
                                });" +
                                "} catch(err) {" +
                                "	console.error(err);" +
                                "};"
                    } ?: run {
                        js = "try {" +
                                "	utag.mobile.remote_api.response[\"${response.commandId}\"][\"${response.id}\"](${response.status});" +
                                "} catch(err) {" +
                                "	console.error(err);" +
                                "};"
                    }
                }

                if (js.isNotEmpty()) {
                    afterDispatchSendCallbacks.onEvaluateJavascript(js)
                }
            }
        }
    }

    private fun enableCookieManager() {
        CookieManager.getInstance().apply {
            setAcceptCookie(true)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                setAcceptThirdPartyCookies(webView, true)
            }

            Logger.dev(BuildConfig.TAG, "WebView: $webView created and cookies enabled")
        }
    }

    fun loadUrlToWebView() {
        val cannotSendWifiUnavailable =
            isWifiOnlySending && !connectivityRetriever.isConnectedWifi()

        if (cannotSendWifiUnavailable || !connectivityRetriever.isConnected()) {
            return
        }

        if (!this::webView.isInitialized) {
            initializeWebView()
            return
        }

        val oldStatus = webViewStatus.getAndSet(PageStatus.LOADING)
        if (oldStatus != PageStatus.LOADING) {
            val decoratedUrl = decorateUrlParams(urlString)
            val cacheBuster = if (urlString.contains("?")) '&' else '?'
            val timestamp = "timestamp_unix=${(System.currentTimeMillis() / 1000)}"
            val url = "$decoratedUrl$cacheBuster$timestamp"

            try {
                scope.launch {
                    webView.loadUrl(url)
                }
            } catch (t: Throwable) {
                Logger.prod(BuildConfig.TAG, t.localizedMessage)
            }
        }
    }

    fun isTimedOut(): Boolean {
        return SystemClock.elapsedRealtime() - lastUrlLoadTimestamp >= timeoutInterval.coerceAtLeast(
            0
        ) * 1000;
    }

    fun hasReachedMaxErrors(): Boolean {
        return webViewCreationErrorCount >= webViewCreationRetries
    }

    override fun onLibrarySettingsUpdated(settings: LibrarySettings) {
        isWifiOnlySending = settings.wifiOnly
        timeoutInterval = settings.refreshInterval // seconds
        if (isTimedOut()) {
            loadUrlToWebView()
        }
    }

    override fun onSessionStarted(sessionId: Long) {
        this.sessionId = sessionId
        shouldRegisterSession.set(true)

        if (sessionCountingEnabled) {
            registerNewSessionIfNeeded(sessionId)
        }
    }

    private fun registerNewSessionIfNeeded(sessionId: Long) {
        if (sessionId == INVALID_SESSION_ID) {
            return
        }

        if (connectivityRetriever.isConnected() &&
            webViewStatus.get() == PageStatus.LOADED_SUCCESS &&
            shouldRegisterSession.compareAndSet(true, false)
        ) {
            backgroundScope.launch {
                val url = createSessionUrl(context.config, sessionId)
                Logger.dev(BuildConfig.TAG, "Registering new Tag Management session - $url")
                context.httpClient.get(url)
            }
        }
    }

    override fun onQueryParametersUpdated(params: Map<String, List<String>>?) {
        params?.let {
            val currentParams = queryParams.toMutableMap()
            currentParams.putAll(params)
            queryParams = currentParams.toMap()
            loadUrlToWebView()
        }
    }

    companion object {
        const val SESSION_URL_TEMPLATE =
            "https://tags.tiqcdn.com/utag/tiqapp/utag.v.js?a=%s/%s/%s&cb=%s"
        const val INVALID_SESSION_ID = -1L

        private fun isFavicon(url: String): Boolean {
            return url.toLowerCase(Locale.ROOT).contains("favicon.ico")
        }

        private fun isAboutScheme(url: String): Boolean {
            return url.toLowerCase(Locale.ROOT).startsWith("about:")
        }

        fun createSessionUrl(config: TealiumConfig, sessionId: Long): String {
            return String.format(
                Locale.ROOT, SESSION_URL_TEMPLATE,
                config.accountName,
                config.profileName,
                sessionId,
                sessionId
            )
        }
    }
}