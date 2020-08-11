package com.tealium.core.validation

import android.app.Activity
import com.tealium.core.messaging.ActivityObserverListener
import com.tealium.core.messaging.EventRouter
import com.tealium.core.messaging.LibrarySettingsUpdatedListener
import com.tealium.core.settings.LibrarySettings
import com.tealium.core.persistence.DispatchStorage
import com.tealium.dispatcher.Dispatch

/**
 * The BatchingValidator will queue events until the batch limit, set in the Library Settings, has
 * been reached.
 */
internal class BatchingValidator(private val dispatchStorage: DispatchStorage,
                                 librarySettings: LibrarySettings,
                                 private val eventRouter: EventRouter) : DispatchValidator, LibrarySettingsUpdatedListener, ActivityObserverListener {

    override val name: String = "BATCHING_VALIDATOR"
    override var enabled: Boolean = true

    private var batchSettings = librarySettings.batching
    private var activityCount = 0

    override fun shouldQueue(dispatch: Dispatch?): Boolean {
        val count = dispatchStorage.count()
        return batchSettings.maxQueueSize != 0 &&
                count + 1 < batchSettings.batchSize
    }

    override fun shouldDrop(dispatch: Dispatch): Boolean {
        return false
    }

    override fun onLibrarySettingsUpdated(settings: LibrarySettings) {
        batchSettings = settings.batching
    }

    override fun onActivityResumed(activity: Activity?) {
        ++activityCount
    }

    override fun onActivityStopped(activity: Activity?, isChangingConfiguration: Boolean) {
        --activityCount
        if (activityCount == 0 && !isChangingConfiguration) {
            eventRouter.onRevalidate(BatchingValidator::class.java)
        }
    }

    override fun onActivityPaused(activity: Activity?) {
        // do nothing
    }
}