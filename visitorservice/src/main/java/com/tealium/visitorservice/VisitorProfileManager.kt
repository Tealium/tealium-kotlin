package com.tealium.visitorservice

import com.tealium.core.*
import com.tealium.core.messaging.*
import com.tealium.core.network.ResourceRetriever
import com.tealium.dispatcher.Dispatch
import kotlinx.coroutines.*
import kotlinx.coroutines.Dispatchers
import org.json.JSONException
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

class VisitorUpdatedMessenger(private val visitorProfile: VisitorProfile) :
    Messenger<VisitorUpdatedListener>(VisitorUpdatedListener::class) {
    override fun deliver(listener: VisitorUpdatedListener) {
        listener.onVisitorUpdated(visitorProfile)
    }
}

interface VisitorUpdatedListener : ExternalListener {
    fun onVisitorUpdated(visitorProfile: VisitorProfile)
}

class RequestVisitorProfileMessenger :
    Messenger<RequestVisitorProfileListener>(RequestVisitorProfileListener::class) {
    override fun deliver(listener: RequestVisitorProfileListener) {
        listener.onRequestVisitorProfile()
    }
}

interface RequestVisitorProfileListener : ExternalListener {
    fun onRequestVisitorProfile()
}

interface VisitorProfileManager {
    val visitorProfile: VisitorProfile
    fun requestVisitorProfile()
}

class VisitorManager(
    private val context: TealiumContext,
    private val refreshInterval: Long =
        context.config.visitorServiceRefreshInterval
            ?: DEFAULT_REFRESH_INTERVAL,
    private val visitorServiceUrl: String =
        context.config.overrideVisitorServiceUrl
            ?: DEFAULT_VISITOR_SERVICE_TEMPLATE,
    private val loader: Loader = JsonLoader(context.config.application),
    private val delay: suspend (Long) -> Unit = { millis -> kotlinx.coroutines.delay(millis) }
) : VisitorProfileManager, DispatchSendListener, BatchDispatchSendListener,
    VisitorIdUpdatedListener, RequestVisitorProfileListener {

    private val file = File(context.config.tealiumDirectory, VISITOR_PROFILE_FILENAME)
    private val visitorServiceProfileOverride: String? =
        context.config.overrideVisitorServiceProfile
    private val ioScope = CoroutineScope(Dispatchers.IO)

    val isUpdating = AtomicBoolean(false)
    internal var profileUpdateJob: Job? = null

    @Volatile
    private var lastUpdate: Long = -1L
    private var visitorId = context.visitorId
        set(value) {
            if (field != value) {
                field = value
                // URL needs updating
                resourceRetriever = createResourceRetriever()
                cancelVisitorProfileUpdate("Visitor Id has changed.")
            }
        }
    private var resourceRetriever: ResourceRetriever = createResourceRetriever()

    override val visitorProfile: VisitorProfile
        get() = _visitorProfile

    @Volatile
    private var _visitorProfile: VisitorProfile = loadCachedProfile() ?: VisitorProfile()
        private set(value) {
            field = value
            context.events.send(VisitorUpdatedMessenger(value))
        }

    private fun createResourceRetriever(): ResourceRetriever {
        return ResourceRetriever(
            context.config,
            generateVisitorServiceUrl(),
            context.httpClient
        ).apply {
            useIfModifed = false
            maxRetries = 1
            refreshInterval = 0
        }
    }

    internal fun generateVisitorServiceUrl(): String {
        return visitorServiceUrl.replace(PLACEHOLDER_ACCOUNT, context.config.accountName)
            .replace(
                PLACEHOLDER_PROFILE,
                visitorServiceProfileOverride ?: context.config.profileName
            )
            .replace(PLACEHOLDER_VISITOR_ID, visitorId)
    }

    fun loadCachedProfile(): VisitorProfile? {
        return loader.loadFromFile(file)?.let {
            return try {
                val jsonObject = JSONObject(it)
                VisitorProfile.fromJson(jsonObject)
            } catch (jex: JSONException) {
                Logger.dev(BuildConfig.TAG, "Failed to read cached visitor profile.")
                null
            }
        }
    }

    fun saveVisitorProfile(visitorProfile: VisitorProfile) {
        try {
            file.writeText(VisitorProfile.toJson(visitorProfile).toString(), Charsets.UTF_8)
        } catch (ioe: IOException) {
            Logger.dev(BuildConfig.TAG, "Error writing to file (${file.name}): ${ioe.message}")
        }
    }

    override suspend fun onDispatchSend(dispatch: Dispatch) {
        updateProfile()
    }

    override suspend fun onBatchDispatchSend(dispatches: List<Dispatch>) {
        updateProfile()
    }

    override fun onVisitorIdUpdated(visitorId: String) {
        this.visitorId = visitorId
        _visitorProfile = VisitorProfile() // empty visitor
        saveVisitorProfile(VisitorProfile()) // empty visitor saved to file
        requestVisitorProfile()
    }

    override fun onRequestVisitorProfile() {
        requestVisitorProfile()
    }

    fun updateProfile() {
        if (refreshIntervalReached()) {
            requestVisitorProfile()
        } else {
            Logger.dev(
                BuildConfig.TAG,
                "Visitor Profile refresh interval not reached, will not update."
            )
        }
    }

    private fun cancelVisitorProfileUpdate(reasonMessage: String = "") {
        profileUpdateJob?.cancel(reasonMessage)
        isUpdating.set(false)
    }

    /**
     * Fetches a new VisitorProfile if it is not already being fetched.
     * Ignores the refresh interval as defined on [TealiumConfig.visitorServiceRefreshInterval]
     */
    override fun requestVisitorProfile() {
        if (!isUpdating.compareAndSet(false, true)) {
            Logger.dev(BuildConfig.TAG, "Visitor profile is already being updated.")
            return
        }

        val retriever = resourceRetriever
        val visitorId = context.visitorId
        val currentProfile = visitorProfile

        profileUpdateJob = ioScope.launch {
            for (i in 1..5) {
                Logger.dev(
                    BuildConfig.TAG,
                    "Fetching visitor profile for $visitorId."
                )

                val newProfile = parseVisitorProfile(retriever.fetch())
                if (newProfile != null) {
                    if (currentProfile.totalEventCount < newProfile.totalEventCount) {
                        if (!isActive) break

                        lastUpdate = System.currentTimeMillis()
                        saveVisitorProfile(newProfile)
                        _visitorProfile = newProfile
                        break
                    } else {
                        Logger.dev(BuildConfig.TAG, "Visitor Profile found but it was stale.")
                    }
                }

                // back off a bit
                delay(750L * i)
            }
            if (isActive)
                isUpdating.set(false)
        }
    }

    private fun refreshIntervalReached(): Boolean {
        return lastUpdate + TimeUnit.SECONDS.toMillis(refreshInterval) <= System.currentTimeMillis()
    }

    companion object {
        const val VISITOR_PROFILE_FILENAME = "visitor_profile.json"
        const val DEFAULT_REFRESH_INTERVAL = 300L

        // url replacements
        const val PLACEHOLDER_ACCOUNT = "{{account}}"
        const val PLACEHOLDER_PROFILE = "{{profile}}"
        const val PLACEHOLDER_VISITOR_ID = "{{visitorId}}"

        const val DEFAULT_VISITOR_SERVICE_TEMPLATE =
            "https://visitor-service.tealiumiq.com/$PLACEHOLDER_ACCOUNT/$PLACEHOLDER_PROFILE/$PLACEHOLDER_VISITOR_ID"

        fun parseVisitorProfile(json: String?): VisitorProfile? {
            if (json == null || json == "{}") {
                Logger.dev(BuildConfig.TAG, "Invalid visitor profile found.")
                return null
            }

            return try {
                Logger.dev(BuildConfig.TAG, "Fetched visitor profile: $json.")
                VisitorProfile.fromJson(JSONObject(json))
            } catch (ex: JSONException) {
                Logger.dev(BuildConfig.TAG, "Failed to parse VisitorProfile: ${ex.message}")
                null
            }
        }
    }
}