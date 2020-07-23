package com.tealium.location

import android.Manifest
import android.app.Application
import android.content.pm.PackageManager
import android.location.Location
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationServices
import com.tealium.core.Environment
import com.tealium.core.Tealium
import com.tealium.core.TealiumConfig
import com.tealium.core.TealiumContext
import io.mockk.*
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.newSingleThreadContext
import kotlinx.coroutines.test.setMain
import org.junit.*
import org.junit.rules.TestRule
import java.io.File

class LocationTests {

    val LOCATION_CLIENT = "location_client"

    @MockK
    lateinit var mockContext: Application

    @MockK
    lateinit var mockFile: File

    @MockK
    lateinit var mockFusedLocationProviderClient: FusedLocationProviderClient

    @get:Rule
    var instantExecutorRule: TestRule = InstantTaskExecutorRule()

    lateinit var config: TealiumConfig
    lateinit var tealium: Tealium
    lateinit var tealiumContext: TealiumContext
    lateinit var location: LocationManager
    private val mainThreadSurrogate = newSingleThreadContext("UI thread")

    @Before
    fun setUp() {
        MockKAnnotations.init(this)
        Dispatchers.setMain(mainThreadSurrogate)

        tealium = mockk<Tealium>()
        every { tealium.track(any()) } returns mockk()
        mockkConstructor(TealiumConfig::class)
        every { mockContext.filesDir } returns mockFile
        every { anyConstructed<TealiumConfig>().tealiumDirectory.mkdir() } returns mockk()
        every { mockContext.applicationContext } returns mockContext

        mockkStatic(LocationServices::class)
        every { LocationServices.getFusedLocationProviderClient(any()) } returns mockk()

        config = TealiumConfig(mockContext, "test_account", "test_profile", Environment.QA)

        mockkStatic(LocationServices::class)
        every { LocationServices.getFusedLocationProviderClient(any()) } returns mockFusedLocationProviderClient
    }

    @Test
    fun geofenceUrlValid() {
        tealiumContext = TealiumContext(config, "", mockk(), mockk(), mockk(), mockk(), tealium)

        location = LocationManager(tealiumContext)

        val expectedUrl = "https://tags.tiqcdn.com/dle/test_account/test_profile/geofences.json"
        Assert.assertEquals(expectedUrl, location.geofenceUrl)
    }

    @Test
    fun checkValidLastLocationWhenPermissionGranted() {
        val mockLocationClient = mockkClass(FusedLocationProviderClient::class)
        val mockLocation = mockk<Location>()
        every { mockLocation.latitude } returns -0.9707625
        every { mockLocation.longitude } returns 51.4610304
        every { mockLocationClient.setMockLocation(mockLocation) } returns mockk()
        mockLocationClient.setMockLocation(mockLocation)

        config.options[LOCATION_CLIENT] = mockLocationClient

        tealiumContext = TealiumContext(config, "", mockk(), mockk(), mockk(), mockk(), tealium)
        every { mockContext.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) } returns PackageManager.PERMISSION_GRANTED
        every { mockContext.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) } returns PackageManager.PERMISSION_GRANTED

        location = LocationManager(tealiumContext)

        every { mockFusedLocationProviderClient.lastLocation } returns LocationTask(mockLocation)
        every { location.lastLocation() } returns mockLocation

        val lastLocation = location.lastLocation()

        location.lastLocationLatitude()?.let { lastLat ->
            location.lastLocationLongitude()?.let { lastLong ->
                Assert.assertEquals(lastLat, mockLocation.latitude, 0.0)
                Assert.assertEquals(lastLong, mockLocation.longitude, 0.0)
            }
        }

        lastLocation?.let {
            Assert.assertEquals(it.latitude, mockLocation.latitude, 0.0)
            Assert.assertEquals(it.longitude, mockLocation.longitude, 0.0)
        }
    }

    @Test
    fun checkLastLocationWhenPermissionDenied() {
        val mockLocationClient = mockkClass(FusedLocationProviderClient::class)
        val mockLocation = mockk<Location>()
        every { mockLocation.latitude } returns -0.9707625
        every { mockLocation.longitude } returns 51.4610304
        every { mockLocationClient.setMockLocation(mockLocation) } returns mockk()
        mockLocationClient.setMockLocation(mockLocation)

        config.options[LOCATION_CLIENT] = mockLocationClient

        tealiumContext = TealiumContext(config, "", mockk(), mockk(), mockk(), mockk(), tealium)
        every { mockContext.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) } returns PackageManager.PERMISSION_DENIED
        every { mockContext.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) } returns PackageManager.PERMISSION_DENIED

        location = LocationManager(tealiumContext)

        every { mockFusedLocationProviderClient.lastLocation } returns LocationTask(mockLocation)
        every { location.lastLocation() } returns null

        val lastLocation = location.lastLocation()

        Assert.assertNull(location.lastLocationLatitude())
        Assert.assertNull(location.lastLocationLongitude())
        Assert.assertNull(lastLocation)
    }

    @Test
    fun startLocationTrackingValidIntervalValue() {
        val mockLocationClient = mockkClass(FusedLocationProviderClient::class)
        every { mockLocationClient.requestLocationUpdates(any(), any(), any()) } returns mockk()

        config.options[LOCATION_CLIENT] = mockLocationClient

        tealiumContext = TealiumContext(config, "", mockk(), mockk(), mockk(), mockk(), tealium)
        every { mockContext.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) } returns PackageManager.PERMISSION_GRANTED
        every { mockContext.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) } returns PackageManager.PERMISSION_GRANTED

        location = LocationManager(tealiumContext)

        location.startLocationTracking(true, 5000)

        verify { mockLocationClient.requestLocationUpdates(any(), any(), any()) }
    }

    @Test
    fun startLocationTrackingInvalidIntervalValue() {
        val mockLocationClient = mockkClass(FusedLocationProviderClient::class)
        every { mockLocationClient.requestLocationUpdates(any(), any(), any()) } returns mockk()

        config.options[LOCATION_CLIENT] = mockLocationClient

        tealiumContext = TealiumContext(config, "", mockk(), mockk(), mockk(), mockk(), tealium)
        every { mockContext.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) } returns PackageManager.PERMISSION_GRANTED
        every { mockContext.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) } returns PackageManager.PERMISSION_GRANTED

        location = LocationManager(tealiumContext)

        location.startLocationTracking(true, -3000)

        verify(exactly = 0) { mockLocationClient.requestLocationUpdates(any(), any(), any()) }
    }

    @Test
    fun stopLocationTracking() {
        val mockLocationClient = mockkClass(FusedLocationProviderClient::class)
        every { mockLocationClient.requestLocationUpdates(any(), any(), any()) } returns mockk()

        config.options[LOCATION_CLIENT] = mockLocationClient

        tealiumContext = TealiumContext(config, "", mockk(), mockk(), mockk(), mockk(), tealium)

        every { mockContext.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) } returns PackageManager.PERMISSION_GRANTED
        every { mockContext.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) } returns PackageManager.PERMISSION_GRANTED

        location = LocationManager(tealiumContext)

        location.startLocationTracking(true, 5000)
        every { mockLocationClient.removeLocationUpdates(any<LocationCallback>()) } returns mockk()

        location.stopLocationTracking()

        verify { mockLocationClient.removeLocationUpdates(any<LocationCallback>()) }

    }

    @Test
    fun createAndAddNewGeofenceWithValidInputs() {
        val mockLocationClient = mockkClass(FusedLocationProviderClient::class)
        every { mockLocationClient.requestLocationUpdates(any(), any(), any()) } returns mockk()

        config.options[LOCATION_CLIENT] = mockLocationClient

        tealiumContext = TealiumContext(config, "", mockk(), mockk(), mockk(), mockk(), tealium)

        every { mockContext.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) } returns PackageManager.PERMISSION_GRANTED
        every { mockContext.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) } returns PackageManager.PERMISSION_GRANTED

        location = LocationManager(tealiumContext)

        val name = "test_geofence"
        val latitude = 0.0
        val longitude = 0.0
        val radius = 100
        val expireTime = 100
        val loiterTime = 0
        val triggerEnter = true
        val triggerExit = false
        location.addGeofence(name, latitude, longitude, radius, expireTime, loiterTime, triggerEnter, triggerExit)

        Assert.assertEquals(location.allGeofenceNames()?.size, 1)
    }

    @Test
    fun createAndAddNewGeofenceWithInvalidInputs() {
        val mockLocationClient = mockkClass(FusedLocationProviderClient::class)
        every { mockLocationClient.requestLocationUpdates(any(), any(), any()) } returns mockk()

        config.options[LOCATION_CLIENT] = mockLocationClient

        tealiumContext = TealiumContext(config, "", mockk(), mockk(), mockk(), mockk(), tealium)

        every { mockContext.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) } returns PackageManager.PERMISSION_GRANTED
        every { mockContext.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) } returns PackageManager.PERMISSION_GRANTED

        location = LocationManager(tealiumContext)

        val name = ""
        val latitude = 0.0
        val longitude = 0.0
        val radius = 100
        val expireTime = 100
        val loiterTime = 0
        val triggerEnter = true
        val triggerExit = false

        location.addGeofence(name, latitude, longitude, radius, expireTime, loiterTime, triggerEnter, triggerExit)

        Assert.assertEquals(location.allGeofenceNames()?.size, 0)
    }
}