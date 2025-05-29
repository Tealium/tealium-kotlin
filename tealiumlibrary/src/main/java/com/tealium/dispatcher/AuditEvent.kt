package com.tealium.dispatcher

class AuditEvent(
    val eventName: String,
    data: Map<String, Any>? = null
) : Dispatch {

    private val delegate: Dispatch = TealiumEvent(eventName, data)

    override val id: String =
        "$AUDIT_ID_PREFIX${delegate.id}"

    override var timestamp: Long? = delegate.timestamp

    override fun payload(): Map<String, Any> =
        delegate.payload()

    override fun addAll(data: Map<String, Any>) =
        delegate.addAll(data)

    override fun remove(key: String) =
        delegate.remove(key)

    companion object {
        const val AUDIT_ID_PREFIX = "audit-"

        fun isAuditEvent(dispatch: Dispatch): Boolean =
            dispatch is AuditEvent || isAuditEvent(dispatch.id)

        fun isAuditEvent(id: String): Boolean =
            id.startsWith(AUDIT_ID_PREFIX)
    }
}