package com.tealium.core.network

import com.tealium.core.Logger
import com.tealium.core.TealiumConfig
import com.tealium.tealiumlibrary.BuildConfig
import kotlinx.coroutines.*
import java.text.SimpleDateFormat
import java.util.*

/**
 * Fetches a resource with the option to check if the resource is modified first, as well as retry capability.
 */
class ResourceRetriever(
    private val config: TealiumConfig,
    private val resourceUrlString: String,
    var networkClient: NetworkClient = HttpClient(config)
) {

    /**
     * Set this property to false if the URL does not support If-Modified.
     * Default is true.
     */
    var useIfModifed = true // ugh, typo

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
    private var lastFetchTimestamp: Long? = null

    private val MS_IN_MINUTES = 60000L

    private val dateFormat: SimpleDateFormat =
        SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss 'GMT'", Locale.ROOT)

    init {
        dateFormat.timeZone = TimeZone.getTimeZone("GMT")
    }

    suspend fun fetch(): String? = coroutineScope {
        async {
            retry(maxRetries, 500) {
                fetchResource()
            }
        }.await()
    }

    suspend fun fetchWithEtag(etag: String?): ResourceEntity? = coroutineScope {
        async {
            retry(maxRetries, 500) {
                fetchResourceEntity(etag)
            }
        }.await()
    }

    private suspend fun fetchResource(): String? = coroutineScope {
        withContext(Dispatchers.Default) {
            if (isActive) {
                if (!useIfModifed) {
                    val json = networkClient.get(resourceUrlString)
                    Logger.dev(BuildConfig.TAG, "Fetched resource with JSON: $json.")
                    json
                } else {
                    fetchIfModified()
                }
            } else {
                null
            }
        }
    }

    private suspend fun fetchResourceEntity(etag: String?): ResourceEntity? = coroutineScope {
            withContext(Dispatchers.Default) {
                if (isActive) {
                    if (etag.isNullOrBlank()) {
                        val entity = networkClient.getResourceEntity(resourceUrlString)
                        Logger.dev(BuildConfig.TAG, "Fetched resource with JSON: ${entity?.response}.")
                        entity
                    } else {
                        fetchIfNoneMatch(etag)
                    }
                } else {
                    null
                }
            }
        }

    private suspend fun fetchIfModified(): String? = coroutineScope {
        withContext(Dispatchers.Default) {
            if (isActive) {
                if (shouldFetchFromTimestamp(lastFetchTimestamp)) {
                    val resourceModified = async { shouldFetchIfModified() }.await()
                    resourceModified?.let {modified ->
                        if (modified) {
                            val json = networkClient.get(resourceUrlString)
                            Logger.dev(BuildConfig.TAG, "Fetched resource with JSON at $resourceUrlString: $json.")
                            lastFetchTimestamp = System.currentTimeMillis()
                            json
                        } else {
                            null
                        }
                    }
                } else {
                    null
                }
            } else {
                null
            }
        }
    }

    private suspend fun fetchIfNoneMatch(etag: String): ResourceEntity? = coroutineScope {
        withContext(Dispatchers.Default) {
            if (isActive) {
                val resourceModified = async { shouldFetchIfNoneMatch(etag) }.await()
                resourceModified?.let { modified ->
                    if (modified) {
                        val json = networkClient.getResourceEntity(resourceUrlString)
                        Logger.dev(
                            BuildConfig.TAG,
                            "Fetched resource with JSON at $resourceUrlString: $json."
                        )
                        json
                    } else {
                        null
                    }
                }
            } else {
                null
            }
        }
    }

    companion object {
        suspend fun <T> retry(numRetries: Int, timeout: Long, block: suspend (Int) -> T?): T? {
            for (i in 1..numRetries) {
                try {
                    return withTimeout(timeout) {
                        Logger.dev(BuildConfig.TAG, "Fetching resource; attempt number $i of $numRetries.")
                        block(i)
                    }
                } catch (e: TimeoutCancellationException) {
                    Logger.prod(BuildConfig.TAG, "Timed out, could not fetch resource.")
                }
            }
            return block(0)
        }
    }

    fun shouldFetchFromTimestamp(timestamp: Long?): Boolean {
        if (refreshInterval == 0) {
            return false
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

    suspend fun shouldFetchIfNoneMatch(etag: String): Boolean? {
        return networkClient.ifNoneMatch(resourceUrlString, etag)
    }
}