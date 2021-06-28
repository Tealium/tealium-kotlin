package com.tealium.lifecycle

object LifecycleDefaults {
    const val FORMAT_ISO_8601 = "yyyy-MM-dd'T'HH:mm:ss'Z'";
    const val TIMESTAMP_INVALID = Long.MIN_VALUE
    const val DAY_IN_MS = 86400000L
    const val SLEEP_THRESHOLD = 5000L
}

object LifecycleSPKey {
    const val TIMESTAMP_UPDATE = "timestamp_update"
    const val TIMESTAMP_LAUNCH = "timestamp_launch"
    const val TIMESTAMP_FIRST_LAUNCH = "timestamp_first_launch"
    const val TIMESTAMP_LAST_LAUNCH = "timestamp_last_launch"
    const val TIMESTAMP_LAST_WAKE = "timestamp_last_wake"
    const val TIMESTAMP_LAST_SLEEP = "timestamp_last_sleep"
    const val COUNT_LAUNCH = "count_launch"
    const val COUNT_SLEEP = "count_sleep"
    const val COUNT_WAKE = "count_wake"
    const val COUNT_TOTAL_CRASH = "count_total_crash"
    const val COUNT_TOTAL_LAUNCH = "count_total_launch"
    const val COUNT_TOTAL_SLEEP = "count_total_sleep"
    const val COUNT_TOTAL_WAKE = "count_total_wake"
    const val LAST_EVENT = "last_event"
    const val TOTAL_SECONDS_AWAKE = "total_seconds_awake"
    const val PRIOR_SECONDS_AWAKE = "prior_seconds_awake"
}

object LifecycleEvent {
    const val LAUNCH = "launch"
    const val WAKE = "wake"
    const val SLEEP = "sleep"
    const val DISABLE = "disable"
    const val PAUSE = "pause"
}

object LifecycleStateKey {
    const val LIFECYCLE_DAYOFWEEK_LOCAL = "lifecycle_dayofweek_local"
    const val LIFECYCLE_DAYSSINCELAUNCH = "lifecycle_dayssincelaunch"
    const val LIFECYCLE_DAYSSINCELASTWAKE = "lifecycle_dayssincelastwake"
    const val LIFECYCLE_HOUROFDAY_LOCAL = "lifecycle_hourofday_local"
    const val LIFECYCLE_DIDDETECTCRASH = "lifecycle_diddetectcrash"
    const val LIFECYCLE_FIRSTLAUNCHDATE = "lifecycle_firstlaunchdate"
    const val LIFECYCLE_FIRSTLAUNCHDATE_MMDDYYYY = "lifecycle_firstlaunchdate_MMDDYYYY"
    const val LIFECYCLE_LAUNCHCOUNT = "lifecycle_launchcount"
    const val LIFECYCLE_SLEEPCOUNT = "lifecycle_sleepcount"
    const val LIFECYCLE_WAKECOUNT = "lifecycle_wakecount"
    const val LIFECYCLE_SECONDSAWAKE = "lifecycle_secondsawake"
    const val LIFECYCLE_TOTALCRASHCOUNT = "lifecycle_totalcrashcount"
    const val LIFECYCLE_TOTALLAUNCHCOUNT = "lifecycle_totallaunchcount"
    const val LIFECYCLE_TOTALSLEEPCOUNT = "lifecycle_totalsleepcount"
    const val LIFECYCLE_TOTALWAKECOUNT = "lifecycle_totalwakecount"
    const val LIFECYCLE_TOTALSECONDSAWAKE = "lifecycle_totalsecondsawake"
    const val LIFECYCLE_LASTLAUNCHDATE = "lifecycle_lastlaunchdate"
    const val LIFECYCLE_LASTWAKEDATE = "lifecycle_lastwakedate"
    const val LIFECYCLE_LASTSLEEPDATE = "lifecycle_lastsleepdate"
    const val LIFECYCLE_UPDATELAUNCHDATE = "lifecycle_updatelaunchdate"
    const val LIFECYCLE_DAYSSINCEUPDATE = "lifecycle_dayssinceupdate"

    const val LIFECYCLE_ISFIRSTLAUNCH = "lifecycle_isfirstlaunch"
    const val LIFECYCLE_ISFIRSTLAUNCHUPDATE = "lifecycle_isfirstlaunchupdate"
    const val LIFECYCLE_ISFIRSTWAKEMONTH = "lifecycle_isfirstwakemonth"
    const val LIFECYCLE_ISFIRSTWAKETODAY = "lifecycle_isfirstwaketoday"

    const val LIFECYCLE_PRIORSECONDSAWAKE = "lifecycle_priorsecondsawake"
    const val LIFECYCLE_TYPE = "lifecycle_type"

    const val AUTOTRACKED = "autotracked"
}