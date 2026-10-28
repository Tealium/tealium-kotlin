package com.tealium.location

import android.os.Build
import com.tealium.core.Environment
import com.tealium.core.JsonLoader
import com.tealium.core.TealiumConfig
import com.tealium.core.TealiumContext
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
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
    fun jsonToGeofenceLocation_DropsOnlyInvalidEntries() {
        val jsonArray = JSONArray()
            .put(geofenceJson("Geofence 1"))
            .put(geofenceJson("Geofence 2").put(GeofenceObject.RADIUS, 0))
            .put(geofenceJson("Geofence 3").apply { remove(GeofenceObject.RADIUS) })
            .put(geofenceJson("a".repeat(101)))
            .put(geofenceJson("Geofence 5")
                .put(GeofenceObject.TRIGGER_ON_ENTER, false)
                .put(GeofenceObject.TRIGGER_ON_EXIT, false))
            .put(geofenceJson("Geofence 6").apply { remove(GeofenceObject.LATITUDE) })
            .put("Geofence 7")

        val geofenceLocationResult = GeofenceLocation.jsonArrayToGeofenceLocation(jsonArray)

        Assert.assertEquals(1, geofenceLocationResult.size)
        Assert.assertEquals("Geofence 1", geofenceLocationResult[0].name)
    }

    @Test
    fun jsonToGeofenceLocation_KeepsFirstEntry_WhenNamesDuplicated() {
        val jsonArray = JSONArray()
            .put(geofenceJson("Geofence 1"))
            .put(geofenceJson("Geofence 2"))
            .put(geofenceJson("Geofence 1").put(GeofenceObject.LATITUDE, 30.0))

        val geofenceLocationResult = GeofenceLocation.jsonArrayToGeofenceLocation(jsonArray)

        Assert.assertEquals(listOf("Geofence 1", "Geofence 2"), geofenceLocationResult.map { it.name })
        Assert.assertEquals(10.0, geofenceLocationResult[0].latitude, 0.0)
    }

    @Test
    fun locationManager_DoesNotThrow_WhenGeofenceAssetMissing() {
        val app = RuntimeEnvironment.getApplication()
        val config = TealiumConfig(app, "test", "test", Environment.DEV)
        config.geofenceFilename = "missing.json"
        val context: TealiumContext = mockk(relaxed = true)
        every { context.config } returns config
        mockkObject(JsonLoader)
        every { JsonLoader.getInstance(any()) } returns JsonLoader(app)
        LocationManager.allGeofenceLocations.clear()

        LocationManager(context)

        Assert.assertTrue(LocationManager.allGeofenceLocations.isEmpty())
        unmockkObject(JsonLoader)
    }

    @Test
    fun validLoadFromAsset() {
        val app = RuntimeEnvironment.application
        Assert.assertNotNull(app)

        val assetLoader = JsonLoader(app)
        val jsonString = assetLoader.loadFromAsset("validGeofence.json")

        val geofenceLocationResult = GeofenceLocation.jsonArrayToGeofenceLocation(JSONArray(jsonString))
        Assert.assertEquals(geofenceLocationResult.size, 2)
    }

    @Test
    fun invalidLoadFromAsset() {
        val app = RuntimeEnvironment.application
        Assert.assertNotNull(app)

        val assetLoader = JsonLoader(app)
        val jsonString = assetLoader.loadFromAsset("invalidGeofence.json")

        val geofenceLocationResult = GeofenceLocation.jsonArrayToGeofenceLocation(JSONArray(jsonString))
        Assert.assertEquals(geofenceLocationResult.size, 0)
    }

    @Test
    fun mixedValidityLoadFromAsset() {
        val app = RuntimeEnvironment.application
        Assert.assertNotNull(app)

        val assetLoader = JsonLoader(app)
        val jsonString = assetLoader.loadFromAsset("validAndInvalidGeofence.json")

        val geofenceObj1 = GeofenceLocation.create("Tealium_asset1", 59.4610304, -9.9707625, 100, -1, 0, true, true)

        val geofenceLocationResult = GeofenceLocation.jsonArrayToGeofenceLocation(JSONArray(jsonString))
        Assert.assertEquals(geofenceLocationResult.size, 1)
        Assert.assertEquals(geofenceLocationResult[0].name, "Tealium_asset1")
        Assert.assertEquals(geofenceLocationResult[0].geofence, geofenceObj1?.geofence)
    }

    private fun geofenceJson(name: String): JSONObject = JSONObject()
        .put(GeofenceObject.NAME, name)
        .put(GeofenceObject.LATITUDE, 10.0)
        .put(GeofenceObject.LONGITUDE, 20.0)
        .put(GeofenceObject.RADIUS, 100)
        .put(GeofenceObject.EXPIRE_AFTER, -1)
        .put(GeofenceObject.TRIGGER_ON_ENTER, true)
        .put(GeofenceObject.TRIGGER_ON_EXIT, true)
        .put(GeofenceObject.MINIMUM_DWELL_TIME, 0)
}
