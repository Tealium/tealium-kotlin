package com.tealium.core.network

/**
 * The [ResourceEntity] describes the response and associated information relating to a remote resource.
 *
 * @param response The body of any response from the remote resource
 * @param etag The relevant etag if received in the response headers
 * @param status The status of this request
 */
data class ResourceEntity(
    val response: String? = null,
    val etag: String? = null,
    val status: ResponseStatus? = null
)

/**
 * Class denoting the supported response statuses when fetching a remote resource
 */
sealed class ResponseStatus {
    /**
     * The response was successful, typically an HTTP 200
     */
    object Success: ResponseStatus()

    /**
     * Fetching the resource was cancelled, or the Coroutine is no longer running.
     */
    object Cancelled: ResponseStatus()

    /**
     * Network connectivity was deemed to be unavailable, so the response is not available.
     */
    object NotConnected: ResponseStatus()

    /**
     * An unexpected exception occurred while fetching the resource.
     *
     * @param cause The underlying cause of the failure, if available.
     */
    class UnknownError(val cause: Throwable?): ResponseStatus()

    /**
     * A response was received, but was not deemed to be a [Success] - typically any non-200 response code.
     */
    class Non200Response(val code: Int): ResponseStatus()
}