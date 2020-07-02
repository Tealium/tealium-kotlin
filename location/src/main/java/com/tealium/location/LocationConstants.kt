package com.tealium.location

object LocationConstants {
    const val LOCATION_ACCURACY = "location_accuracy"
    const val DEVICE_LAST_LATITUDE = "latitude"
    const val DEVICE_LAST_LONGITUDE = "longitude"
}

object GeofenceTransitionType {
    const val ENTER = "geofence_entered"
    const val EXIT = "geofence_exited"
    const val DWELL = "geofence_dwell"
}

object GeofenceEventConstants {
    const val GEOFENCE_NAME = "geofence_name"
    const val GEOFENCE_TRANSITION_TYPE = "geofence_transition_type"
}

object GeofenceObject {
    const val NAME = "name"
    const val LATITUDE = "latitude"
    const val LONGITUDE = "longitude"
    const val RADIUS = "radius"
    const val EXPIRE_AFTER = "expire_after"
    const val TRIGGER_ON_ENTER = "trigger_on_enter"
    const val TRIGGER_ON_EXIT = "trigger_on_exit"
    const val MINIMUM_DWELL_TIME = "minimum_dwell_time"
}