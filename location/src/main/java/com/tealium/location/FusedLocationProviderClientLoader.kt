package com.tealium.location

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.os.Build
import com.google.android.gms.location.*
import com.tealium.core.DateUtils
import com.tealium.core.Logger
import com.tealium.core.TealiumContext
import java.util.*
import kotlin.collections.ArrayList

class FusedLocationProviderClientLoader(
    private val context: TealiumContext,
    private val locationClient: FusedLocationProviderClient = context.config.overrideFusedLocationProviderClient
        ?: LocationServices.getFusedLocationProviderClient(context.config.application),
) {

    private var _lastLocation: Location? = null
    val lastLocation: Location?
        get() = _lastLocation

    private var _isHighAccuracy: Boolean? = null
    val isHighAccuracy: Boolean?
        get() = _isHighAccuracy

    internal var locationRequest: LocationRequest? = null
    internal lateinit var locationCallback: LocationCallback
    internal lateinit var geofenceLocationClient: GeofenceLocationClient

    private fun createLocationCallback(): LocationCallback {
        return object : LocationCallback() {
            override fun onLocationResult(p0: LocationResult?) {
                p0?.let { locationResult ->
                    val lastLocation = locationResult.lastLocation
                    _lastLocation = lastLocation

                    Logger.dev(
                        BuildConfig.TAG,
                        "Received updated Location: lat=${lastLocation?.latitude},lng=${lastLocation?.longitude}"
                    )

                    lastLocation?.let { lastLocationResult ->

                        val geofencesToAdd = ArrayList<GeofenceLocation>()
                        if (LocationManager.allGeofenceLocations.isNotEmpty()) {
                            LocationManager.allGeofenceLocations.forEach { geofence ->
                                val location = Location("geofenceLocation")
                                location.latitude = geofence.latitude
                                location.longitude = geofence.longitude

                                val distance = lastLocationResult.distanceTo(location)

                                if (distance < 500.0) {
                                    if (!LocationManager.activeGeofences.contains(geofence.name)) {
                                        geofencesToAdd.add(geofence)
                                        Logger.dev(
                                            BuildConfig.TAG,
                                            "Geofence ${geofence.name} added to active monitoring"
                                        )
                                    }
                                } else {
                                    if (LocationManager.activeGeofences.contains(geofence.name)) {
                                        removeGeofenceFromClient(geofence.name)
                                        Logger.dev(
                                            BuildConfig.TAG,
                                            "Geofence ${geofence.name} removed from active monitoring"
                                        )
                                    }
                                }
                            }
                        }
                        addGeofenceToClient(geofencesToAdd)
                    }
                }
            }
        }
    }

    fun addGeofenceToClient(geofencesList: List<GeofenceLocation>) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
            context.config.application.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) {
            Logger.dev(BuildConfig.TAG, "Please request location permission")
            return
        }

        if (geofencesList.isNotEmpty()) {
            for (geofence in geofencesList) {
                LocationManager.activeGeofences.add(geofence.name)
            }

            geofenceLocationClient.addGeofence(geofencesList)
        }
    }

    fun removeGeofenceFromClient(geofenceName: String) {
        LocationManager.activeGeofences.remove(geofenceName)
        geofenceLocationClient.removeGeofence(geofenceName)
    }

    fun startLocationTracking(isHighAccuracy: Boolean, updateInterval: Int) {
        startLocationTracking(
            LocationTrackingOptions(
                accuracy = if (isHighAccuracy) LocationTrackingAccuracy.HighAccuracy else LocationTrackingAccuracy.BalancedAccuracy,
                minTime = updateInterval.toLong()
            )
        )
    }

    fun startLocationTracking(options: LocationTrackingOptions) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
            context.config.application.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) {
            Logger.dev(BuildConfig.TAG, "Please request location permission!")
            return
        }

        if (options.minTime < 0) {
            Logger.dev(BuildConfig.TAG, "UpdateInterval value must be greater than or equal to 0")
            return
        }

        if (options.minDistance < 0) {
            Logger.dev(BuildConfig.TAG, "MinimumDistance value must be greater than or equal to 0")
            return
        }

        geofenceLocationClient = GeofenceLocationClient(context)
        locationRequest = createLocationRequest(options)
        _isHighAccuracy =
            options.accuracy == LocationTrackingAccuracy.HighAccuracy
        locationCallback = createLocationCallback()
        locationClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            null
        ) // TODO null Looper?
    }

    fun stopLocationUpdates() {
        if (!this::locationCallback.isInitialized) {
            Logger.dev(BuildConfig.TAG, "Location tracking was not started")
            return
        }
        locationClient.removeLocationUpdates(locationCallback)
        Logger.dev(BuildConfig.TAG, "Location tracking stopped")
    }

    companion object {
        fun createLocationRequest(locationTrackingOptions: LocationTrackingOptions): LocationRequest {
            return LocationRequest.create()
                .setPriority(locationTrackingOptions.accuracy.requestPriority)
                .setInterval(locationTrackingOptions.minTime)
                .setFastestInterval(locationTrackingOptions.minTime)
                .setSmallestDisplacement(locationTrackingOptions.minDistance)
        }
    }
}