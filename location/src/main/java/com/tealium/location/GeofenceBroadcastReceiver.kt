package com.tealium.location

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingEvent
import com.tealium.core.Logger

class GeofenceBroadcastReceiver : BroadcastReceiver() {

    override fun onReceive(p0: Context?, p1: Intent?) {
        p1?.let { intent ->
            val geofencingEvent = GeofencingEvent.fromIntent(intent)

            if (geofencingEvent.hasError()) {
                Logger.dev(BuildConfig.TAG, geofencingEvent.toString())
                return
            }

            val geofenceTransition = getTransitionString(geofencingEvent.geofenceTransition)
            geofenceTransition?.let { transition ->

                val triggeringGeofencesList = geofencingEvent.triggeringGeofences

                for (geofence in triggeringGeofencesList) {
                    val geofenceName = geofence.requestId
                    LocationManager.sendGeofenceEvent(geofenceName, transition)
                    Logger.dev(BuildConfig.TAG, "Triggered $geofenceTransition on $geofenceName")
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
                Logger.dev(BuildConfig.TAG, "Error in geofence trasition type")
                null
            }
        }
    }
}