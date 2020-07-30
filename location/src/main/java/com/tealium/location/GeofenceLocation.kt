package com.tealium.location

import com.google.android.gms.location.Geofence
import com.tealium.core.Logger
import org.json.JSONArray

data class GeofenceLocation(val name: String,
                            val latitude: Double,
                            val longitude: Double,
                            val radius: Int,
                            val expireTime: Int,
                            val loiterTime: Int,
                            val triggerEnter: Boolean,
                            val triggerExit: Boolean) {

    val transitionType: Int = setGeofenceTransitionTypes()
    val geofence: Geofence = createGeofence()

    private fun setGeofenceTransitionTypes(): Int {
        var transition = 0

        if (triggerEnter) {
            transition = transition or Geofence.GEOFENCE_TRANSITION_ENTER
        }

        if (triggerExit) {
            transition = transition or Geofence.GEOFENCE_TRANSITION_EXIT
        }

        if (loiterTime > 0) {
            transition = transition or Geofence.GEOFENCE_TRANSITION_DWELL
        }

        return transition
    }

    private fun createGeofence(): Geofence {
        return Geofence.Builder()
                .setRequestId(name)
                .setCircularRegion(latitude, longitude, radius.toFloat())
                .setExpirationDuration(expireTime.toLong())
                .setTransitionTypes(transitionType)
                .setLoiteringDelay(loiterTime)
                .build()
    }

    companion object {
        fun create(name: String,
                   latitude: Double,
                   longitude: Double,
                   radius: Int,
                   expireTime: Int,
                   loiterTime: Int,
                   triggerEnter: Boolean,
                   triggerExit: Boolean): GeofenceLocation? {
            if (name.isEmpty()) {
                Logger.dev(BuildConfig.TAG, "Geofence must have a name")
                return null
            }

            if (latitude <= -90 || latitude >= 90) {
                Logger.dev(BuildConfig.TAG, "Latitude must be between -90.0 and 90.0!")
                return null
            }

            if (longitude <= -180 || longitude >= 180) {
                Logger.dev(BuildConfig.TAG, "Longitude must be between -180.0 and 180.0!")
                return null
            }

            if (radius < 0) {
                Logger.dev(BuildConfig.TAG, "Radius greater than 0")
                return null
            }

            if (expireTime < -1) {
                Logger.dev(BuildConfig.TAG, "Expire time must be a valid integer. 0 or greater, or -1 for never expire!")
                return null
            }

            if (loiterTime < 0) {
                Logger.dev(BuildConfig.TAG, "Loiter time must be a valid integer. 1 or greater, or 0 for trigger on enter")
                return null
            }

            return GeofenceLocation(name, latitude, longitude, radius, expireTime, loiterTime, triggerEnter, triggerExit)
        }

        fun jsonArrayToGeofenceLocation(geofenceJsonArray: JSONArray): List<GeofenceLocation> {
            val geofenceArray = ArrayList<GeofenceLocation>()

            for (i in 0 until geofenceJsonArray.length()) {
                val geofenceItem = geofenceJsonArray.getJSONObject(i)

                val geofenceName = geofenceItem.optString(GeofenceObject.NAME)
                val geofenceLatitude = geofenceItem.optDouble(GeofenceObject.LATITUDE)
                val geofenceLongitude = geofenceItem.optDouble(GeofenceObject.LONGITUDE)
                val geofenceRadius = geofenceItem.optInt(GeofenceObject.RADIUS)
                val geofenceExpireTime = geofenceItem.optInt(GeofenceObject.EXPIRE_AFTER)
                val geofenceLoiterTime = geofenceItem.optInt(GeofenceObject.MINIMUM_DWELL_TIME)
                val geofenceTriggerEnter = geofenceItem.optBoolean(GeofenceObject.TRIGGER_ON_ENTER)
                val geofenceTriggerExit = geofenceItem.optBoolean(GeofenceObject.TRIGGER_ON_EXIT)

                val geofenceLocation = create(geofenceName,
                        geofenceLatitude,
                        geofenceLongitude,
                        geofenceRadius,
                        geofenceExpireTime,
                        geofenceLoiterTime,
                        geofenceTriggerEnter,
                        geofenceTriggerExit)

                geofenceLocation?.let {
                    geofenceArray.add(it)
                }
            }
            return geofenceArray
        }
    }
}