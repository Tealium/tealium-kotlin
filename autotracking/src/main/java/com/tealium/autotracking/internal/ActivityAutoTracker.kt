package com.tealium.autotracking.internal

import android.app.Activity
import com.tealium.autotracking.*
import com.tealium.core.Logger
import com.tealium.core.TealiumContext
import com.tealium.dispatcher.TealiumView
import java.lang.Exception

internal class ActivityAutoTracker(
        private val context: TealiumContext,
        private val trackingMode: AutoTrackingMode,
        private val globalActivityDataCollector: ActivityDataCollector? = null,
        private val blacklist: ActivityBlacklist = ActivityBlacklist(context.config)
) : ActivityTracker {

    private var activityChangingConfiguration = ""

    override fun trackActivity(activity: Activity, data: Map<String, Any>?) {
        trackActivity(getName(activity), activity as? ActivityDataCollector, data)
    }

    override fun trackActivity(activityDataCollector: ActivityDataCollector, data: Map<String, Any>?) {
        trackActivity(getName(activityDataCollector), activityDataCollector, data)
    }

    private fun trackActivity(activityName: String, activityDataCollector: ActivityDataCollector?, data: Map<String, Any>?) {
        Logger.dev(BuildConfig.TAG, "Tracking Activity Event for: $activityName")

        if (blacklist.isBlacklisted(activityName)) {
            Logger.dev(BuildConfig.TAG, "Activity ($activityName) is blacklisted; no event will be sent.")
            return
        }

        val eventData = mutableMapOf<String, Any>("autotracked" to true)
        try {
            data?.let { eventData.putAll(it) }
            globalActivityDataCollector?.onCollectActivityData(activityName)?.let { eventData.putAll(it) }
            activityDataCollector?.onCollectActivityData(activityName)?.let { eventData.putAll(it) }
        } catch (ex: Exception) {
            Logger.qa(BuildConfig.TAG, "Error auto-collecting all activity data for $activityName: ${ex.message}")
        }
        context.track(TealiumView(activityName, eventData.toMap()))
    }

    override fun onActivityPaused(activity: Activity?) {
        // do nothing
    }

    override fun onActivityResumed(activity: Activity?) {
        if (trackingMode == AutoTrackingMode.NONE) return

        activity?.let {
            val autotracked: Autotracked? = it::class.java.getAnnotation(Autotracked::class.java)
            if (autotracked != null && !autotracked.track) return

            val activityName = getName(it, autotracked)

            if (!(activityChangingConfiguration == activityName)) {

                if (trackingMode == AutoTrackingMode.FULL) {
                    trackActivity(activityName, it as? ActivityDataCollector, null)
                } else if (trackingMode == AutoTrackingMode.ANNOTATED) {
                    if (autotracked != null) {
                        trackActivity(activityName, it as? ActivityDataCollector, null)
                    }
                }
            }
        }
    }

    override fun onActivityStopped(activity: Activity?, isChangingConfiguration: Boolean) {
        activity?.let {
            if (isChangingConfiguration) {
                activityChangingConfiguration = getName(it)
            } else {
                activityChangingConfiguration = ""
            }
        }
    }

    private fun getName(annotated: Any, autotracked: Autotracked? = annotated::class.java.getAnnotation(Autotracked::class.java)): String {
        val annotationName: String? = autotracked?.name?.let { name ->
            if (name.isNotBlank()) {
                name
            } else {
                null
            }
        }

        return annotationName ?: annotated::class.simpleName ?: "anonymous"
    }
}