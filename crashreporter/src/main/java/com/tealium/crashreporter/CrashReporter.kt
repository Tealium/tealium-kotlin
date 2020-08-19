package com.tealium.crashreporter

import com.tealium.crashreporter.CrashReporter.Tracker
import com.tealium.internal.data.Dispatch
import com.tealium.internal.listeners.ActivityResumeListener
import com.tealium.internal.listeners.DisableListener
import com.tealium.internal.listeners.PopulateDispatchListener
import com.tealium.library.DataSources
import com.tealium.library.Tealium

/**
 * Crash Reporter module for tracking crash data. When used, this module populates
 * <i>crash_*</i> attributes defined in {@link com.tealium.library.DataSources.Key}.
 */
class CrashReporter (config: Tealium.Config, tracker: Tracker, truncateStackTrace: Boolean) : Thread.UncaughtExceptionHandler {

    const val CRASH_COUNT =  "crash_count"

    val mOriginalExceptionHandler: Thread.UncaughtExceptionHandler
    val mListener: TealiumListener
    val mTracker: Tracker
    val mTruncateCrashStackTraces: Boolean
    val mSharedPreferences: SharedPreferences
    val mCrashCount: Int

    init {
        mTracker = tracker
        mTruncateCrashStackTraces = truncateStackTrace
        mListener = TealiumListener()
        config.getEventListeners().add(mListener)

        val sharedPrefsName = getSharedPreferencesName(config)

        mSharedPreferences = config.getApplication()
                .getSharedPreferences(sharedPrefsName, 0)
        mCrashCount = mSharedPreferences.getInt(CRASH_COUNT, 0)

        mOriginalExceptionHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler(this)
    }
}