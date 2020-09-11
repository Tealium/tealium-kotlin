package com.tealium.location

import android.os.Build
import com.tealium.core.JsonLoader
import io.mockk.MockKAnnotations
import org.json.JSONArray
import org.json.JSONException
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.P])
class LocationGeofenceLoaderTests {

    @Before
    fun setUp() {
        MockKAnnotations.init(this)
    }

    @Test
    fun jsonToGeofenceLocationValidString() {
        val jsonString: String = ("[\n" +
                "{\n" +
                "    \"name\": \"Tealium_Reading\",\n" +
                "    \"latitude\": 51.4610304,\n" +
                "    \"longitude\": -0.9707625,\n" +
                "    \"radius\": 100,\n" +
                "    \"expire_after\": -1,\n" +
                "    \"trigger_on_enter\": true,\n" +
                "    \"trigger_on_exit\": true,\n" +
                "    \"minimum_dwell_time\": 0\n" +
                "  },\n" +
                "  {\n" +
                "       \"name\": \"Tealium_San_Diego\",\n" +
                "       \"latitude\": 32.9061189,\n" +
                "       \"longitude\": -117.2379163,\n" +
                "       \"radius\": 100,\n" +
                "       \"expire_after\": -1,\n" +
                "       \"trigger_on_enter\": true,\n" +
                "       \"trigger_on_exit\": true,\n" +
                "\"minimum_dwell_time\": 0\n" +
                "}\n" +
                "  ]")

        val geofenceLocationResult = GeofenceLocation.jsonArrayToGeofenceLocation(JSONArray(jsonString))

        val geofenceObj1 = GeofenceLocation.create("Tealium_Reading", 51.4610304, -0.9707625, 100, -1, 0, true, true)
        val geofenceObj2 = GeofenceLocation.create("Tealium_San_Diego", 32.9061189, -117.2379163, 100, -1, 0, true, true)

        Assert.assertEquals(geofenceLocationResult[0].geofence, geofenceObj1?.geofence)
        Assert.assertEquals(geofenceLocationResult[1].geofence, geofenceObj2?.geofence)
    }

    @Test(expected = JSONException::class)
    fun jsonToGeofenceLocationInvalidString() {
        val jsonString = ("[\n" +
                "    {\n" +
                "        \"name\": \"\",\n" +
                "        \"latitude\": 51.4610304,\n" +
                "        \"longitude\": -0.9707625,\n" +
                "        \"radius\": ,\n" +
                "        \"expire_after\": -1,\n" +
                "        \"trigger_on_enter\": true,\n" +
                "        \"trigger_on_exit\": true,\n" +
                "        \"minimum_dwell_time\": 0\n" +
                "      },\n" +
                "  ]")

        val geofenceLocationResult = GeofenceLocation.jsonArrayToGeofenceLocation(JSONArray(jsonString))

        Assert.assertEquals(geofenceLocationResult.size, 0)
    }

    @Test
    fun jsonToGeofenceLocationValidAndInvalidStringCombo() {
        val jsonString = ("[\n" +
                "    {\n" +
                "        \"name\": \"Tealium_Reading\",\n" +
                "        \"latitude\": 51.4610304,\n" +
                "        \"longitude\": -0.9707625,\n" +
                "        \"radius\": 100,\n" +
                "        \"expire_after\": -1,\n" +
                "        \"trigger_on_enter\": true,\n" +
                "        \"trigger_on_exit\": true,\n" +
                "        \"minimum_dwell_time\": 0\n" +
                "      },\n" +
                "      {\n" +
                "        \"name\": \"\",\n" +
                "        \"latitude\": 32.9061189,\n" +
                "        \"longitude\": -117.2379163,\n" +
                "        \"radius\": 100,\n" +
                "        \"expire_after\": -1,\n" +
                "        \"trigger_on_enter\": true,\n" +
                "        \"trigger_on_exit\": true,\n" +
                "        \"minimum_dwell_time\": 0\n" +
                "      }\n" +
                "  ]")

        val geofenceLocationResult = GeofenceLocation.jsonArrayToGeofenceLocation(JSONArray(jsonString))

        val geofenceObj1 = GeofenceLocation.create("Tealium_Reading", 51.4610304, -0.9707625, 100, -1, 0, true, true)

        Assert.assertEquals(geofenceLocationResult[0].geofence, geofenceObj1?.geofence)
        Assert.assertEquals(geofenceLocationResult.size, 1)
    }

    @Test
    fun validLoadFromAsset() {
        val app = RuntimeEnvironment.application
        Assert.assertNotNull(app)

        val assetLoader = JsonLoader.getInstance(app)
        val jsonString = assetLoader.loadFromAsset("validGeofence.json")

        val geofenceLocationResult = GeofenceLocation.jsonArrayToGeofenceLocation(JSONArray(jsonString))
        Assert.assertEquals(geofenceLocationResult.size, 2)
    }

    @Test
    fun invalidLoadFromAsset() {
        val app = RuntimeEnvironment.application
        Assert.assertNotNull(app)

        val assetLoader = JsonLoader.getInstance(app)
        val jsonString = assetLoader.loadFromAsset("invalidGeofence.json")

        val geofenceLocationResult = GeofenceLocation.jsonArrayToGeofenceLocation(JSONArray(jsonString))
        Assert.assertEquals(geofenceLocationResult.size, 0)
    }

    @Test
    fun mixedValidityLoadFromAsset() {
        val app = RuntimeEnvironment.application
        Assert.assertNotNull(app)

        val assetLoader = JsonLoader.getInstance(app)
        val jsonString = assetLoader.loadFromAsset("validAndInvalidGeofence.json")

        val geofenceObj1 = GeofenceLocation.create("Tealium_asset1", 59.4610304, -9.9707625, 100, -1, 0, true, true)

        val geofenceLocationResult = GeofenceLocation.jsonArrayToGeofenceLocation(JSONArray(jsonString))
        Assert.assertEquals(geofenceLocationResult.size, 1)
        Assert.assertEquals(geofenceLocationResult[0].name, "Tealium_asset1")
        Assert.assertEquals(geofenceLocationResult[0].geofence, geofenceObj1?.geofence)
    }
}