@file:JvmName("Constants")

package com.tealium.core.consent

object ConsentManagerConstants {
    @Deprecated(
        "Constant has been moved.",
        ReplaceWith("Dispatch.Keys.CONSENT_POLICY", "com.tealium.dispatcher.Dispatch")
    )
    const val CONSENT_POLICY = "policy"

    @Deprecated(
        "Constant has been moved.",
        ReplaceWith("Dispatch.Keys.CONSENT_STATUS", "com.tealium.dispatcher.Dispatch")
    )
    const val CONSENT_STATUS = "consent_status"

    @Deprecated(
        "Constant has been moved.",
        ReplaceWith("Dispatch.Keys.CONSENT_CATEGORIES", "com.tealium.dispatcher.Dispatch")
    )
    const val CONSENT_CATEGORIES = "consent_categories"

    @Deprecated(
        "Constant has been moved.",
        ReplaceWith("Dispatch.Keys.CONSENT_DO_NOT_SELL", "com.tealium.dispatcher.Dispatch")
    )
    const val CONSENT_DO_NOT_SELL = "do_not_sell"

    const val GRANT_FULL_CONSENT = "grant_full_consent"
    const val GRANT_PARTIAL_CONSENT = "grant_partial_consent"
    const val DECLINE_CONSENT = "decline_consent"

    const val KEY_STATUS = "status"
    const val KEY_CATEGORIES = "categories"
    const val KEY_LAST_STATUS_UPDATE = "last_updated"
}