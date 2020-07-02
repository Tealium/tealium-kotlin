package com.tealium.core.validation

import com.tealium.core.Module
import com.tealium.dispatcher.Dispatch

/**
 * A DispatchValidator can be used to control the flow of Dispatches through the system. Each new
 * Dispatch will be sent to each Dispatch Validator; any one of them can signify that the Dispatch
 * should be either queued or dropped.
 */
interface DispatchValidator: Module {

    /**
     * Will be called for each new dispatch and for any revalidation events signified by a null
     * value for the [dispatch] parameter.
     *
     * @param dispatch the new dispatch, or null
     * @return true if the dispatch should be queued, otherwise false
     */
    fun shouldQueue(dispatch: Dispatch?): Boolean

    /**
     * Will be called for each new dispatch.
     *
     * @param dispatch the new dispatch
     * @return true if the dispatch should be queued, otherwise false
     */
    fun shouldDrop(dispatch: Dispatch): Boolean
}