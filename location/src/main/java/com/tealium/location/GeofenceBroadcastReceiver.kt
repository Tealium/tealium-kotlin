package com.tealium.location

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofenceStatusCodes
import com.google.android.gms.location.GeofencingEvent
import com.tealium.core.Logger

class GeofenceBroadcastReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        intent?.let { it ->
            val geofencingEvent = GeofencingEvent.fromIntent(it)

            if (geofencingEvent == null) {
                Logger.dev(BuildConfig.TAG, "Received intent is not a geofencing event")
                return
            }

            if (geofencingEvent.hasError()) {
                Logger.dev(
                    BuildConfig.TAG,
                    "Geofencing error: ${GeofenceStatusCodes.getStatusCodeString(geofencingEvent.errorCode)}"
                )
                if (geofencingEvent.errorCode == GeofenceStatusCodes.GEOFENCE_NOT_AVAILABLE) {
                    // Play services has removed all geofences, e.g. location was turned off, so
                    // let the next location update register them again.
                    LocationManager.activeGeofences.clear()
                }
                return
            }

            getTransitionString(geofencingEvent.geofenceTransition)?.let { transition ->
                for (geofence in geofencingEvent.triggeringGeofences.orEmpty()) {
                    val geofenceName = geofence.requestId
                    LocationManager.sendGeofenceEvent(geofenceName, transition)
                    Logger.dev(BuildConfig.TAG, "Triggered $transition on $geofenceName")
                }
            }
        }
    }

    private fun getTransitionString(geofenceTransition: Int): String? {
        return when (geofenceTransition) {
            Geofence.GEOFENCE_TRANSITION_DWELL -> GeofenceTransitionType.DWELL
            Geofence.GEOFENCE_TRANSITION_ENTER -> GeofenceTransitionType.ENTER
            Geofence.GEOFENCE_TRANSITION_EXIT -> GeofenceTransitionType.EXIT
            else -> {
                Logger.dev(BuildConfig.TAG, "Error in geofence transition type")
                null
            }
        }
    }
}