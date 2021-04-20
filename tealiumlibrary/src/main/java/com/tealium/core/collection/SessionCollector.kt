package com.tealium.core.collection

import com.tealium.core.Collector
import com.tealium.core.Session
import com.tealium.core.messaging.NewSessionListener

class SessionCollector(private var sessionId: Long) : NewSessionListener, Collector {

    /**
     * Updates the session id to the new one.
     */
    override fun onNewSession(sessionId: Long) {
        this.sessionId = sessionId
    }

    /**
     * Only collect the Session Id for now.
     */
    override suspend fun collect(): Map<String, Any> {
        return mapOf(
                Session.KEY_SESSION_ID to sessionId
        )
    }

    override val name: String = MODULE_NAME
    override var enabled: Boolean = true

    companion object {
        const val MODULE_NAME = "SessionCollector"
    }
}