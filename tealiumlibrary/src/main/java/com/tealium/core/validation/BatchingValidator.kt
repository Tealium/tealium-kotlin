package com.tealium.core.validation

import android.app.Activity
import com.tealium.core.messaging.ActivityObserverListener
import com.tealium.core.messaging.LibrarySettingsUpdatedListener
import com.tealium.core.persistence.DispatchStorage
import com.tealium.core.settings.LibrarySettings
import com.tealium.dispatcher.Dispatch
import kotlin.math.max

/**
 * The BatchingValidator will queue events until the batch limit, set in the Library Settings, has
 * been reached.
 */
internal class BatchingValidator(
    private val dispatchStorage: DispatchStorage,
    librarySettings: LibrarySettings,
    private val onBackgrounding: () -> Unit,
) : DispatchValidator, LibrarySettingsUpdatedListener, ActivityObserverListener {

    override val name: String = "BatchingValidator"
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
        // accounts for case where the first "Resumed" is missed.
        activityCount = max(activityCount - 1, 0)
        if (activityCount == 0 && !isChangingConfiguration) {
            onBackgrounding.invoke()
        }
    }

    override fun onActivityPaused(activity: Activity?) {
        // do nothing
    }
}