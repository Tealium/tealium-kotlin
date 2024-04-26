package com.tealium.core.network

import com.tealium.core.Logger
import com.tealium.core.TealiumConfig
import com.tealium.core.persistence.getTimestampMilliseconds
import com.tealium.tealiumlibrary.BuildConfig
import kotlinx.coroutines.*
import java.text.SimpleDateFormat
import java.util.*

/**
 * Fetches a resource with the option to check if the resource is modified first, as well as retry capability.
 */
class ResourceRetriever @JvmOverloads constructor(
    private val config: TealiumConfig,
    private val resourceUrlString: String,
    var networkClient: NetworkClient = HttpClient(config)
) {

    /**
     * Set this property to false if the URL does not support If-Modified.
     * Default is true.
     */
    var useIfModifed = true // TODO - typo, should be useIfModified

    /**
     * Set this property to set the interval on which this resource is fetched again.
     * Time is in minutes.
     * Set this to 0 to always fetch.
     */
    var refreshInterval = 60

    /**
     * Set this property to set the number of times the fetching of a resource should
     * be retried. Max is 5.
     *
     */
    var maxRetries = 5

    @Volatile
    var lastFetchTimestamp: Long? = null
        private set

    private val MS_IN_MINUTES = 60000L

    private val dateFormat: SimpleDateFormat =
        SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss 'GMT'", Locale.ROOT)

    private var _isFetching: Boolean = false

    init {
        dateFormat.timeZone = TimeZone.getTimeZone("GMT")
    }

    val shouldRefresh: Boolean
        get() {
            if (_isFetching) return false

            val lastFetch = lastFetchTimestamp ?: return true

            return shouldFetchFromTimestamp(lastFetch)
        }

    suspend fun fetch(): String? = coroutineScope {
        fetch {
            retryWithBackoff(maxRetries, retryWhile = { it == null }) {
                fetchResource()
            }
        }
    }

    suspend fun fetchWithEtag(etag: String?): ResourceEntity? = coroutineScope {
        fetch {
            retryWithBackoff(
                maxRetries,
                retryWhile = { entity -> shouldRetry(entity?.status) },
            ) {
                fetchResourceEntity(etag)
            }
        }
    }

    private inline fun <T> fetch(block: () -> T?): T? {
        if (!shouldRefresh) {
            Logger.dev(BuildConfig.TAG, "Resource timeout has not expired. Will not fetch: $resourceUrlString")
            return null
        }

        return try {
            _isFetching = true
            Logger.dev(BuildConfig.TAG, "Fetching resource: $resourceUrlString")

            block()
        } finally {
            lastFetchTimestamp = getTimestampMilliseconds()
            _isFetching = false
        }
    }

    private suspend fun fetchResource(): String? = coroutineScope {
        withContext(Dispatchers.Default) {
            if (!isActive) {
                return@withContext null
            }

            if (!useIfModifed) {
                val json = networkClient.get(resourceUrlString)
                Logger.dev(BuildConfig.TAG, "Fetched resource with JSON: $json.")
                json
            } else {
                fetchIfModified()
            }
        }
    }

    private suspend fun fetchResourceEntity(etag: String?): ResourceEntity? = coroutineScope {
        withContext(Dispatchers.Default) {
            if (!isActive) {
                return@withContext ResourceEntity(status = ResponseStatus.Cancelled)
            }

            val entity = networkClient.getResourceEntity(resourceUrlString, etag)
            Logger.dev(BuildConfig.TAG, "Fetched resource with JSON: ${entity?.response}.")
            entity
        }
    }

    private suspend fun fetchIfModified(): String? = coroutineScope {
        withContext(Dispatchers.Default) {
            if (!isActive) {
                return@withContext null
            }

            val modified = shouldFetchIfModified()
            if (modified == null || modified) {
                return@withContext null
            }

            val json = networkClient.get(resourceUrlString)
            Logger.dev(
                BuildConfig.TAG,
                "Fetched resource with JSON at $resourceUrlString: $json."
            )
            lastFetchTimestamp = getTimestampMilliseconds()
            json
        }
    }

    companion object {

        fun shouldRetry(status: ResponseStatus?): Boolean {
            return when (status) {
                null -> true
                is ResponseStatus.Success, is ResponseStatus.Cancelled -> false
                is ResponseStatus.Non200Response -> status.code == 408 || status.code == 429 || status.code in 500..599
                is ResponseStatus.UnknownError -> true
                else -> false
            }
        }

        suspend fun <T> retry(numRetries: Int, timeout: Long, block: suspend (Int) -> T?): T? {
            for (i in 1..numRetries) {
                try {
                    return withTimeout(timeout) {
                        Logger.dev(
                            BuildConfig.TAG,
                            "Fetching resource; attempt number $i of $numRetries."
                        )
                        block(i)
                    }
                } catch (e: TimeoutCancellationException) {
                    Logger.prod(BuildConfig.TAG, "Timed out, could not fetch resource.")
                }
            }
            return block(0)
        }

        /**
         * Utility method to retry a task [block] a set number of times. With optional failure
         * backoff and timeout for each retry
         *
         * @param numRetries The maximum number of times after the initial attempt to retry.
         * @param retryDelayMs The delay in milliseconds that should elapse before retrying
         * @param timeout The timeout for each execution of [block] complete
         * @param retryWhile Predicate checked at the end of each attempt to determine if it should still retry.
         * Use this if the result was successfully returned from [block] but was not a satisfactory result.
         * @param block The block of code to be executed, and retried if necessary.
         */
        internal suspend fun <T> retryWithBackoff(
            numRetries: Int,
            retryDelayMs: Long = 500L,
            timeout: Long = 1000L,
            retryWhile: (T?) -> Boolean,
            block: suspend (Int) -> T?
        ): T? {
            var result: T? = null
            for (i in 0..numRetries) {
                if (retryDelayMs > 0) {
                    delay(retryDelayMs * i)
                }

                result = withTimeoutOrNull(timeout) {
                    Logger.dev(
                        BuildConfig.TAG,
                        "Fetching resource; attempt number $i of $numRetries."
                    )
                    block(i)
                }

                if (result == null) {
                    Logger.prod(BuildConfig.TAG, "Timed out, could not fetch resource.")
                }

                if (!retryWhile(result)) {
                    return result
                }
            }

            return result
        }
    }

    fun shouldFetchFromTimestamp(timestamp: Long?): Boolean {
        if (refreshInterval == 0) {
            return true
        }
        timestamp?.let {
            return System.currentTimeMillis() - it > refreshInterval * MS_IN_MINUTES
        }
        return true
    }

    suspend fun shouldFetchIfModified(): Boolean? {
        lastFetchTimestamp?.let { timestamp ->
            return networkClient.ifModified(resourceUrlString, timestamp)
        }
        return true
    }
}
