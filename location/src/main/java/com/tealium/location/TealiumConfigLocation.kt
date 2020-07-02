package com.tealium.location

import com.google.android.gms.location.FusedLocationProviderClient
import com.tealium.core.TealiumConfig

const val GEOFENCE_URL = "geofence_url"
const val GEOFENCE_FILENAME = "geofence_filename"
const val LOCATION_CLIENT = "location_client"

/**
 * Sets the file name to use when using a local asset for the Geofence definitions.
 */
var TealiumConfig.geofenceFilename: String?
    get() = options[GEOFENCE_FILENAME] as? String
    set(value) {
        value?.let {
            options[GEOFENCE_FILENAME] = it
        }
    }

/**
 * Sets the URL to use when using a remote file for the Geofence definitions. The default URL used
 * is: "https://tags.tiqcdn.com/dle/{ACCOUNT_NAME}/{PROFILE_NAME}/geofences.json"
 */
var TealiumConfig.overrideGeofenceUrl: String?
    get() = options[GEOFENCE_URL] as? String
    set(value) {
        value?.let {
            options[GEOFENCE_URL] = it
        }
    }

/**
 * Sets the FusedLocationProvider to use. If not provided then a new one will be constructed.
 */
var TealiumConfig.overrideFusedLocationProviderClient: FusedLocationProviderClient?
    get() = options[LOCATION_CLIENT] as? FusedLocationProviderClient
    set(value) {
        value?.let {
            options[LOCATION_CLIENT] = it
        }
    }