@file:JvmName("Constants")

package com.tealium.core.consent

object ConsentManagerConstants {
    const val CONSENT_POLICY = "policy"
    const val CONSENT_STATUS = "consent_status"
    const val CONSENT_CATEGORIES = "consent_categories"
    const val CONSENT_DO_NOT_SELL = "do_not_sell"

    const val GRANT_FULL_CONSENT = "grant_full_consent"
    const val GRANT_PARTIAL_CONSENT = "grant_partial_consent"
    const val DECLINE_CONSENT = "decline_consent"

    @Deprecated(
        "Constant has been moved.",
        ReplaceWith("Dispatch.Keys.CONSENT_STATUS", "com.tealium.dispatcher.Dispatch")
    )
    const val KEY_STATUS = "status"

    @Deprecated(
        "Constant has been moved.",
        ReplaceWith("Dispatch.Keys.CONSENT_CATEGORIES", "com.tealium.dispatcher.Dispatch")
    )
    const val KEY_CATEGORIES = "categories"

    @Deprecated(
        "Constant has been moved.",
        ReplaceWith("Dispatch.Keys.CONSENT_LAST_STATUS_UPDATE", "com.tealium.dispatcher.Dispatch")
    )
    const val KEY_LAST_STATUS_UPDATE = "last_updated"
}