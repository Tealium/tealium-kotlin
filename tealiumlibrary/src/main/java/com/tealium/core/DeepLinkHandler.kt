package com.tealium.core

import android.app.Activity
import android.content.Intent
import android.net.Uri
import com.tealium.core.messaging.ActivityObserverListener
import com.tealium.core.persistence.Expiry
import com.tealium.dispatcher.Dispatch
import com.tealium.dispatcher.TealiumEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class DeepLinkHandler(
    private val context: TealiumContext,
    private val backgroundScope: CoroutineScope
) : ActivityObserverListener {

    /**
     * Adds the supplied Trace ID to the data layer for the current session.
     */
    fun joinTrace(id: String) {
        context.dataLayer.putString(Dispatch.Keys.TRACE_ID, id, Expiry.SESSION)
    }

    /**
     * Removes the Trace ID from the data layer if present, and calls killVisitorSession.
     */
    @Suppress("unused")
    fun leaveTrace() {
        context.dataLayer.remove(Dispatch.Keys.TRACE_ID)
    }

    /**
     * Kills the visitor session remotely to test end of session events (does not terminate the SDK session
     * or reset the session ID).
     */
    fun killTraceVisitorSession() {
        val dispatch = TealiumEvent(
            eventName = KILL_VISITOR_SESSION,
            data = hashMapOf(Dispatch.Keys.EVENT to KILL_VISITOR_SESSION)
        )
        context.track(dispatch)
    }

    fun handleActivityResumed(activity: Activity) {
        val intent = activity.intent ?: return
        if (Intent.ACTION_VIEW != intent.action) return

        val uri = intent.data ?: return
        if (uri.isOpaque) return

        if (context.config.qrTraceEnabled) {
            uri.getQueryParameter(TRACE_ID_QUERY_PARAM)?.let { traceId ->
                uri.getQueryParameter(KILL_VISITOR_SESSION)?.let {
                    killTraceVisitorSession()
                }
                uri.getQueryParameter(LEAVE_TRACE_QUERY_PARAM)?.let {
                    leaveTrace()
                } ?: joinTrace(traceId)
            }
        }
        if (context.config.deepLinkTrackingEnabled) {
            handleDeepLink(uri)
        }
    }

    /**
     * If the app was launched from a deep link, adds the link and query parameters to the data layer for the current session.
     */
    fun handleDeepLink(uri: Uri) {
        if (uri.isOpaque || uri == Uri.EMPTY) return

        val oldDeepLink = context.dataLayer.getString(Dispatch.Keys.DEEP_LINK_URL)
        if (uri.toString() == oldDeepLink) return

        removeOldDeepLinkData()
        val deepLinkData = mutableMapOf(
            Dispatch.Keys.DEEP_LINK_URL to uri.toString()
        )

        uri.queryParameterNames.forEach { name ->
            uri.getQueryParameter(name)?.let { value ->
                deepLinkData["${Dispatch.Keys.DEEP_LINK_QUERY_PREFIX}_$name"] = value
            }
        }

        deepLinkData.forEach { (key, value) ->
            context.dataLayer.putString(key, value, Expiry.SESSION)
        }

        if (context.config.sendDeepLinkEvent) {
            val dispatch = TealiumEvent(
                eventName = DEEPLINK,
                data = deepLinkData
            )
            context.track(dispatch)
        }
    }

    /**
     * Removes all the Deep Link data related to any previous Deep Link.
     */
    fun removeOldDeepLinkData() {
        context.dataLayer.keys().filter { it.startsWith(Dispatch.Keys.DEEP_LINK_QUERY_PREFIX) }
            .forEach { key ->
                context.dataLayer.remove(key)
            }
    }

    override fun onActivityPaused(activity: Activity?) {
        // not used
    }

    /**
     * Handles deep linking and joinTrace, leaveTrace, killVisitorSession requests.
     */
    override fun onActivityResumed(activity: Activity?) {
        activity?.let {
            backgroundScope.launch {
                handleActivityResumed(it)
            }
        }
    }

    override fun onActivityStopped(activity: Activity?, isChangingConfiguration: Boolean) {
        // not used
    }

    internal companion object {
        internal const val TRACE_ID_QUERY_PARAM = Dispatch.Keys.TEALIUM_TRACE_ID
        internal const val LEAVE_TRACE_QUERY_PARAM = "leave_trace"
        internal const val KILL_VISITOR_SESSION = "kill_visitor_session"
        internal const val DEEPLINK = "deep_link"
    }
}