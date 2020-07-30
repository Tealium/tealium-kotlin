package com.tealium.location

import android.app.PendingIntent
import android.content.Context
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingClient
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices
import com.tealium.core.Logger
import com.tealium.core.TealiumContext

class GeofenceLocationClient(private val context: TealiumContext) {
    private val geofenceLocationClient = createGeofenceLocationClient(context.config.application)

    fun createGeofenceLocationClient(context: Context): GeofencingClient {
        return LocationServices.getGeofencingClient(context)
    }

    fun addGeofence(geofencesToAdd: List<GeofenceLocation>) {
        val geofencePendingIntent = PendingIntent.getBroadcast(context.config.application, 0, LocationManager.fetchLocationIntent(context), PendingIntent.FLAG_UPDATE_CURRENT)
        geofenceLocationClient.addGeofences(geofencingRequest(geofencesToAdd), geofencePendingIntent).run {
            addOnSuccessListener {
                geofencesToAdd.forEach { newGeofence ->
                    LocationManager.activeGeofences.add(newGeofence.name)
                }
                Logger.dev(BuildConfig.TAG, "Geofences SUCCESSFULLY created.");
            }
            addOnFailureListener {
                Logger.dev(BuildConfig.TAG, "Geofences FAILED to be created.")
            }
        }
    }

    fun removeGeofence(geofenceName: String) {
        geofenceLocationClient.removeGeofences(listOf(geofenceName))
        LocationManager.activeGeofences.remove(geofenceName)
    }

    fun geofencingRequest(geofencesToAdd: List<GeofenceLocation>): GeofencingRequest {
        val geofenceObjectsList = ArrayList<Geofence>()
        geofencesToAdd.forEach {
            geofenceObjectsList.add(it.geofence)
        }
        return GeofencingRequest.Builder()
                .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_DWELL)
                .addGeofences(geofenceObjectsList)
                .build()
    }
}