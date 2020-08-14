package com.tealium.tagmanagementdispatcher

import android.annotation.TargetApi
import android.net.http.SslError
import android.os.Build
import android.os.SystemClock
import android.view.View
import android.webkit.*
import com.tealium.core.Logger
import com.tealium.core.TealiumConfig
import com.tealium.core.TealiumContext
import com.tealium.core.messaging.AfterDispatchSendCallbacks
import com.tealium.core.messaging.LibrarySettingsUpdatedListener
import com.tealium.core.messaging.NewSessionListener
import com.tealium.core.messaging.SessionStartedListener
import com.tealium.core.network.ConnectivityRetriever
import com.tealium.core.settings.LibrarySettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean

class WebViewLoader(private val context: TealiumContext,
                    private val urlString: String,
                    private val afterDispatchSendCallbacks: AfterDispatchSendCallbacks)
    : LibrarySettingsUpdatedListener,
        SessionStartedListener {

    val connectivityRetriever = ConnectivityRetriever(context.config.application)
    val isWebViewLoaded = AtomicBoolean(false)
    var lastUrlLoadTimestamp = Long.MIN_VALUE
    private var isWifiOnlySending = false
    private var timeoutInterval = -1
    private val scope = CoroutineScope(Dispatchers.Main)
    private val backgroundScope = CoroutineScope(Dispatchers.IO)
    private var sessionId: Long = INVALID_SESSION_ID
    private val shouldRegisterSession = AtomicBoolean(false)

    @Volatile
    lateinit var webView: WebView

    init {
        initializeWebView()
        context.events.subscribe(this)
    }

    fun initializeWebView() {
        scope.launch {
            webView = WebView(context.config.application.applicationContext)
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
                view.webViewClient = createWebViewClient()
            }

            loadUrlToWebView()
            enableCookieManager()
            isWebViewLoaded.set(PageStatus.LOADED_SUCCESS)
        }
    }

    fun createWebViewClient(): WebViewClient {
        return object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)

                lastUrlLoadTimestamp = SystemClock.elapsedRealtime()

                val webViewStatus = isWebViewLoaded.getAndSet(PageStatus.LOADED_SUCCESS)
                if (PageStatus.LOADED_ERROR == webViewStatus) {
                    isWebViewLoaded.set(PageStatus.LOADED_ERROR)
                    Logger.dev(BuildConfig.TAG, "Error loading URL $url in WebView $view")
                    return
                }

                registerNewSessionIfNeeded(sessionId)

                // Run JS evaluation here
                view?.loadUrl("javascript:(function(){\n" +
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
            override fun onReceivedHttpError(view: WebView?, request: WebResourceRequest?, errorResponse: WebResourceResponse?) {
                super.onReceivedHttpError(view, request, errorResponse)

                errorResponse?.let { error ->
                    request?.let { req ->
                        Logger.prod(BuildConfig.TAG,
                                "Received http error with ${req.url}: ${error.statusCode}: ${error.reasonPhrase}")
                    }
                }
            }

            @TargetApi(Build.VERSION_CODES.M)
            override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
                error?.let {
                    request?.let { req ->
                        onReceivedError(view, it.errorCode, it.description.toString(), req.url.toString())
                    }
                }
                super.onReceivedError(view, request, error)
            }

            // deprecated in API 23, but still supporting API level
            override fun onReceivedError(view: WebView?, errorCode: Int, description: String?, failingUrl: String?) {
                failingUrl?.let {
                    if (it.toLowerCase().contains("favicon.ico")) {
                        return
                    }
                }

                super.onReceivedError(view, errorCode, description, failingUrl)

                if (PageStatus.LOADED_ERROR == isWebViewLoaded.getAndSet(PageStatus.LOADED_ERROR)) {
                    // error already occurred
                    return
                }

                lastUrlLoadTimestamp = SystemClock.uptimeMillis()
                Logger.prod(BuildConfig.TAG, "Received err: {\n" +
                        "\tcode: $errorCode,\n" +
                        "\tdesc:\"${description?.replace("\"", "\\\"")}\",\n" +
                        "\turl:\"$failingUrl\"\n" +
                        "}")
            }

            override fun shouldInterceptRequest(view: WebView?, url: String?): WebResourceResponse? {
                val faviconUrl = "/favicon.ico"

                url?.let {
                    if (!url.toLowerCase().contains(faviconUrl)) {
                        return null
                    }
                }

                return WebResourceResponse("image/png", null, null)
            }

            @TargetApi(Build.VERSION_CODES.LOLLIPOP)
            override fun shouldInterceptRequest(view: WebView?, request: WebResourceRequest?): WebResourceResponse? {
                return shouldInterceptRequest(view, request?.url.toString())
            }

            override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                context.config.options[TagManagementRemoteCommand.TIQ_CONFIG].let {
                    url?.let {
                        if (url.startsWith(TagManagementRemoteCommand.PREFIX)) {
                            afterDispatchSendCallbacks.sendRemoteCommand(url)
                        }
                    }
                }
                return true
            }

            @TargetApi(Build.VERSION_CODES.LOLLIPOP)
            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                return shouldOverrideUrlLoading(view, request?.url.toString())
            }

            override fun onReceivedSslError(view: WebView?, handler: SslErrorHandler?, error: SslError?) {
                isWebViewLoaded.set(PageStatus.LOADED_ERROR)

                view?.let {
                    Logger.dev(BuildConfig.TAG, "Received SSL Error in WebView $it (${it.url}): $error")
                }
                super.onReceivedSslError(view, handler, error)
            }

            override fun onRenderProcessGone(view: WebView?, detail: RenderProcessGoneDetail?): Boolean {
                view?.destroy()
                isWebViewLoaded.set(PageStatus.LOADED_ERROR)
                initializeWebView()
                return true
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
        val cannotSendWifiUnavailable = isWifiOnlySending && !connectivityRetriever.isConnectedWifi()

        if (cannotSendWifiUnavailable || !connectivityRetriever.isConnected()) {
            return
        }

        val webViewStatus = isWebViewLoaded.getAndSet(PageStatus.LOADED_SUCCESS)
        if (PageStatus.LOADED_SUCCESS == webViewStatus) {
            return // already loading
        }

        val cacheBuster = if (urlString.contains("?")) '&' else '?'
        val timestamp = "timestamp_unix=${(System.currentTimeMillis() / 1000)}"
        val url = "$urlString$cacheBuster$timestamp"

        try {
            scope.launch {
                webView.loadUrl(url)
            }
        } catch (t: Throwable) {
            Logger.prod(BuildConfig.TAG, t.localizedMessage)
        }
    }

    override fun onLibrarySettingsUpdated(settings: LibrarySettings) {
        loadUrlToWebView()
        isWifiOnlySending = settings.wifiOnly
        timeoutInterval = settings.refreshInterval
    }

    override fun onSessionStarted(sessionId: Long) {
        this.sessionId = sessionId
        shouldRegisterSession.set(true)

        registerNewSessionIfNeeded(sessionId)
    }

    private fun registerNewSessionIfNeeded(sessionId: Long) {
        if (sessionId == INVALID_SESSION_ID) {
            return
        }

        if (connectivityRetriever.isConnected() &&
                shouldRegisterSession.compareAndSet(PageStatus.LOADED_SUCCESS, false)) {
            backgroundScope.launch {
                val url = createSessionUrl(context.config, sessionId)
                Logger.dev(BuildConfig.TAG, "Registering new Tag Management session - $url")
                context.httpClient.get(url)
            }
        }
    }

    companion object {
        const val SESSION_URL_TEMPLATE = "https://tags.tiqcdn.com/utag/tiqapp/utag.v.js?a=%s/%s/%s&cb=%s"
        const val INVALID_SESSION_ID = -1L

        fun createSessionUrl(config: TealiumConfig, sessionId: Long): String {
            return String.format(Locale.ROOT, SESSION_URL_TEMPLATE,
                    config.accountName,
                    config.profileName,
                    sessionId,
                    sessionId)
        }
    }
}