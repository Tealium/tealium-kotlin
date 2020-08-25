package com.tealium.core

import android.app.Activity
import android.net.Uri
import com.tealium.core.messaging.ActivityObserverListener
import com.tealium.core.persistence.Expiry
import com.tealium.dispatcher.TealiumEvent

class DeepLinkHandler(private val context: TealiumContext): ActivityObserverListener {

    /**
     * Adds the supplied Trace ID to the data layer for the current session.
     */
    fun joinTrace(id: String) {
        context.dataLayer.putString(CoreConstant.TRACE_ID, id, Expiry.SESSION)
    }

    /**
     * Removes the Trace ID from the data layer if present, and calls killVisitorSession.
     */
    @Suppress("unused")
    fun leaveTrace() {
        context.dataLayer.remove(CoreConstant.TRACE_ID)
    }

    /**
     * Kills the visitor session remotely to test end of session events (does not terminate the SDK session
     * or reset the session ID).
     */
    fun killTraceVisitorSession() {
        val dispatch = TealiumEvent(eventName = CoreConstant.KILL_VISITOR_SESSION,
                data = hashMapOf(CoreConstant.KILL_VISITOR_SESSION_EVENT_KEY to  CoreConstant.KILL_VISITOR_SESSION))
        context.track(dispatch)
    }

    /**
     * If the app was launched from a deep link, adds the link and query parameters to the data layer for the current session.
     */
    fun handleDeepLink(uri: Uri) {
            context.dataLayer.putString(CoreConstant.DEEP_LINK_URL, uri.toString(), Expiry.SESSION)
            uri.queryParameterNames.forEach { name ->
                uri.getQueryParameter(name)?.let { value ->
                    context.dataLayer.putString("${CoreConstant.DEEP_LINK_QUERY_PREFIX}_$name", value, Expiry.SESSION)
                }
            }
    }

    override fun onActivityPaused(activity: Activity?) {
        // not used
    }

    /**
     * Handles deep linking and joinTrace, leaveTrace, killVisitorSession requests.
     */
    override fun onActivityResumed(activity: Activity?) {
            activity?.intent?.let { intent ->
                intent.data?.let { uri ->
                    uri.getQueryParameter(CoreConstant.TRACE_ID_QUERY_PARAM)?.let { traceId ->
                        if (context.config.qrTraceEnabled) {
                            uri.getQueryParameter(CoreConstant.KILL_VISITOR_SESSION)?.let {
                                killTraceVisitorSession()
                            }
                            uri.getQueryParameter(CoreConstant.LEAVE_TRACE_QUERY_PARAM)?.let {
                                leaveTrace()
                            } ?:
                            joinTrace(traceId)
                        }
                    }
                    if (context.config.deepLinkTrackingEnabled) {
                        handleDeepLink(uri)
                    }
                }
            }
    }

    override fun onActivityStopped(activity: Activity?, isChangingConfiguration: Boolean) {
        // not used
    }
}