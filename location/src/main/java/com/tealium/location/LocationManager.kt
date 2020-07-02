package com.tealium.location

import android.content.Intent
import android.location.Location
import com.tealium.core.*
import com.tealium.dispatcher.EventDispatch
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONArray
import java.net.URL

/**
 * The LocationManager controls the starting and stopping of location tracking - including both
 * co-ordinate data as well as interactivity with geofences.
 */
class LocationManager(private val context: TealiumContext) :
        Collector {

    private val geofenceFilename: String? = context.config.geofenceFilename
    internal val geofenceUrl: String
        get() = context.config.overrideGeofenceUrl
                ?: "https://tags.tiqcdn.com/dle/" +
                "${context.config.accountName}/" +
                "${context.config.profileName}/geofences.json"

    private val locationProviderClientLoader = FusedLocationProviderClientLoader(context)
    private val scope = CoroutineScope(Dispatchers.IO)
    private val jsonLoader = JsonLoader(context.config.application)

    init {
        loadGeofenceAsset()
    }

    /**
     * Start Location tracking updates
     */
    fun startLocationTracking(isHighAccuracy: Boolean, updateInterval: Int) {
        locationProviderClientLoader.startLocationTracking(isHighAccuracy, updateInterval)
    }

    /**
     * Stops location updates
     */
    fun stopLocationTracking() {
        locationProviderClientLoader.stopLocationUpdates()
    }

    /**
     * Creates and adds new GeofenceLocation object to allGeofencesLocations
     *
     * @param name         name of the location.
     * @param latitude     latitude of the location.
     * @param longitude    longitude of the location.
     * @param radius       radius of the geofence.
     * @param expireTime   the time after you want the geofence to expire, (-1 if expire = false).
     * @param loiterTime   time waited in a geofence before events are fired, (0 if triggerDwell = false).
     * @param triggerEnter whether you want events to be fired upon entering a geofence.
     * @param triggerExit  whether you want events to be fired upon exiting a geofence.
     *
     */
    fun addGeofence(name: String,
                    latitude: Double,
                    longitude: Double,
                    radius: Int,
                    expireTime: Int,
                    loiterTime: Int,
                    triggerEnter: Boolean,
                    triggerExit: Boolean) {
        val newGeofenceLocation = GeofenceLocation.create(name, latitude, longitude, radius, expireTime, loiterTime, triggerEnter, triggerExit)

        newGeofenceLocation?.let {
            allGeofenceLocations.add(it)
            Logger.dev(BuildConfig.TAG, "Geofence ${it.name} added to active monitoring")
        }
    }

    /**
     * Returns last location latitude
     */
    fun lastLocationLatitude(): Double? {
        return locationProviderClientLoader.lastLocation?.latitude
    }

    /**
     * Returns last location longitude
     */
    fun lastLocationLongitude(): Double? {
        return locationProviderClientLoader.lastLocation?.longitude
    }

    /**
     * Returns List of all created GeofenceLocation names
     */
    fun allGeofenceNames(): List<String>? {
        return allGeofenceLocations.map { it.name }
    }

    /**
     * Returns the last known location.
     */
    fun lastLocation(): Location? {
        return locationProviderClientLoader.locationClient.lastLocation.result
    }

    private fun loadGeofenceAsset() {
        // Check for asset first, else load from URL
        geofenceFilename?.let { filename ->
            val jsonArray = JSONArray(jsonLoader.loadFromAsset(filename))
            val geofenceList = GeofenceLocation.jsonArrayToGeofenceLocation(jsonArray)
            geofenceList.forEach { geofence ->
                Logger.dev(BuildConfig.TAG, "Loading Geofence from assets: ${geofence.name}")
                allGeofenceLocations.add(geofence)
            }
        } ?: run {
            scope.launch {
                jsonLoader.loadFromUrl(URL(geofenceUrl))?.let {
                    val geofenceList = GeofenceLocation.jsonArrayToGeofenceLocation(it as JSONArray)
                    geofenceList.forEach { geofence ->
                        Logger.dev(BuildConfig.TAG, "Loading Geofence from URL: ${geofence.name}")
                        allGeofenceLocations.add(geofence)
                    }
                }
            }
        }
    }

    override suspend fun collect(): Map<String, Any> {
        lastLocation()?.let { location ->
            return mapOf(LocationConstants.LOCATION_ACCURACY to locationProviderClientLoader.isHighAccuracy.toString(),
                    LocationConstants.DEVICE_LAST_LATITUDE to location.latitude,
                    LocationConstants.DEVICE_LAST_LONGITUDE to location.longitude)
        }
        return emptyMap()
    }

    companion object : CollectorFactory {
        private lateinit var tealiumContext: TealiumContext

        val activeGeofences = mutableSetOf<String>()
        val allGeofenceLocations = mutableSetOf<GeofenceLocation>()

        override fun create(context: TealiumContext): Collector {
            tealiumContext = context
            return LocationManager(context)
        }

        fun sendGeofenceEvent(geofenceName: String, transitionType: String) {
            val dispatch = EventDispatch(transitionType,
                    mapOf(GeofenceEventConstants.GEOFENCE_NAME to geofenceName,
                            GeofenceEventConstants.GEOFENCE_TRANSITION_TYPE to transitionType))
            tealiumContext.track(dispatch)
        }

        fun fetchLocationIntent(context: TealiumContext): Intent {
            return Intent(context.config.application, GeofenceBroadcastReceiver::class.java)
        }
    }

    override val name: String
        get() = "LOCATION_MANAGER"
    override var enabled: Boolean = true
}

val Collectors.Location: CollectorFactory
    get() = LocationManager

/**
 * Returns the LocationManager module for this Tealium instance.
 */
val Tealium.location: LocationManager?
    get() = modules.getModule(LocationManager::class.java)