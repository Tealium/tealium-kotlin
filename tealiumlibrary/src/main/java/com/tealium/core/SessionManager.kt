package com.tealium.core

import android.app.Activity
import com.tealium.core.messaging.ActivityObserverListener
import com.tealium.core.messaging.EventRouter
import com.tealium.dispatcher.Dispatch
import com.tealium.tealiumlibrary.BuildConfig

/**
 * Class to handle the creation, persistence and notifications relating to [Session] instances.
 * There should be only a single session in use at any given time, accessible through the
 * [currentSession] property.
 */
class SessionManager(config: TealiumConfig,
                     private val eventRouter: EventRouter): ActivityObserverListener {

    /**
     * Represents the current session.
     */
    var currentSession: Session
        private set

    /**
     * Storage for the current session.
     */
    private val sessionPreferences = config.application.getSharedPreferences(sharedPreferencesName(config), 0)

    init {
        // first try and retrieve the stored session from disk.
        val storedSession = Session.readFromSharedPreferences(sessionPreferences)

        // check that we have a stored session and that it's not expired.
        // otherwise create a new one.
        currentSession = when (isExpired(storedSession)) {
            true -> newSession()
            false -> {
                Logger.qa(BuildConfig.TAG, "Found existing session; resuming.")
                storedSession
            }
        }
    }

    /**
     * Used to notify the Session Manager that a new Dispatch has been sent. Post creation of the
     * Session Manager, this method will drive the creation and starting of new sessions as well as
     * updates to session event counts and last event times.
     */
    fun track(dispatch: Dispatch) {
        // Check for expired session.
        if (isExpired(currentSession)) {
            newSession()
        }
        currentSession.eventCount++

        if (shouldStartSession(currentSession)) {
            startSession()
        }

        // Update this last as it's used in shouldStartSession
        currentSession.lastEventTime = getTimestamp()
    }

    override fun onActivityPaused(activity: Activity?) {
        Session.writeToSharedPreferences(sessionPreferences, currentSession)
    }

    override fun onActivityResumed(activity: Activity?) {
        // nothing to do.
    }

    override fun onActivityStopped(activity: Activity?, isChangingConfiguration: Boolean) {
        // nothing to do.
    }

    /**
     * Creates a new [Session] instance for the current timestamp and notifies others that this new
     * session has been generated.
     */
    fun newSession(): Session {
        Logger.qa(BuildConfig.TAG, "Creating new session.")
        currentSession = Session(getTimestamp())
        Session.writeToSharedPreferences(sessionPreferences, currentSession)
        notifyNewSession(currentSession)
        return currentSession
    }

    /**
     * Marks the current session as started and notifies any listeners.
     */
    fun startSession() {
        Logger.qa(BuildConfig.TAG, "Starting session ${currentSession.id}")
        currentSession.sessionStarted = true
        Session.writeToSharedPreferences(sessionPreferences, currentSession)

        notifySessionStarted(currentSession)
    }

    /**
     * Informs listeners that a new Session has been generated
     */
    private fun notifyNewSession(session: Session) {
        eventRouter.onNewSession(session.id)
    }

    //TODO: Implement on the TagManagement module to trigger a "utag.v.js" call
    /**
     * Informs listeners that a new Session has been marked as Started.
     */
    private fun notifySessionStarted(session: Session) {
        eventRouter.onSessionStarted(session.id)
    }

    companion object {
        //TODO: Consider making this customizable.
        const val SESSION_LENGTH_MS = 30 * 60 * 1000 // 30 minutes
        const val SESSION_START_WINDOW_LENGTH_MS = 30 * 1000 // 30 seconds

        /**
         * Returns true if the session has passed the expiration date as well as the lastEventTime. Null values are considered
         * expired
         */
        fun isExpired(session: Session): Boolean {
            return maxOf(session.id, session.lastEventTime) + SESSION_LENGTH_MS < getTimestamp()
        }

        /**
         * Determines whether a session should be started based on the following three criteria:
         *  - the session has not already been marked as started
         *  - there has been more than a single event recorded on this session
         *  - the last event and this new event are within the set window [SESSION_START_WINDOW_LENGTH_MS]
         */
        fun shouldStartSession(session: Session, newEventTime: Long = getTimestamp()): Boolean {
            return !session.sessionStarted
                    && session.eventCount > 1
                    && newEventTime <= session.lastEventTime + SESSION_START_WINDOW_LENGTH_MS
        }

        /**
         * Returns a consistent timestamp for use throughout comparisons. Current implementation is
         * just the System Current Time in Millis.
         */
        private fun getTimestamp(): Long {
            return System.currentTimeMillis()
        }

        /**
         * Returns the Account and Profile specific preferences file name
         */
        private fun sharedPreferencesName(config: TealiumConfig): String {
            return "tealium.sessionpreferences." + Integer.toHexString((config.accountName + config.profileName + config.environment.environment).hashCode())
        }
    }
}