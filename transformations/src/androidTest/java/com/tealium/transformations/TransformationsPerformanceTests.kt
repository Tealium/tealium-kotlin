package com.tealium.transformations

import android.util.Log
import com.tealium.core.JsonUtils
import com.tealium.core.Loader
import com.tealium.core.TealiumContext
import com.tealium.core.TealiumExecutors
import com.tealium.core.persistence.DataLayer
import com.tealium.core.persistence.Expiry
import com.tealium.dispatcher.Dispatch
import com.tealium.dispatcher.TealiumEvent
import com.tealium.transformations.internal.impl.J2v8TransformationsAdapter
import com.tealium.transformations.internal.impl.QuickJsTransformationsAdapter
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.impl.annotations.RelaxedMockK
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.util.concurrent.Executors
import kotlin.system.measureTimeMillis

class TransformationsPerformanceTests {

    data class TestTask(
        val name: String,
        val task: suspend () -> Unit
    )

    data class TaskMetrics(
        val total: Long,
        val average: Double,
        val max: Long,
        val min: Long
    )

    @MockK
    lateinit var context: TealiumContext
    @MockK
    lateinit var executors: TealiumExecutors

    @RelaxedMockK
    lateinit var dataLayer: DataLayer

    @RelaxedMockK
    lateinit var loader: Loader

    val testScope = CoroutineScope(Executors.newSingleThreadExecutor().asCoroutineDispatcher())

    val testDispatchJson = JSONObject("""
        {"tealium_event_type":"event","tealium_event":"event1","request_uuid":"8b36b502-fb64-4d58-bd66-59718edac56f","key1":"value1","key2":2,"tealium_account":"tealiummobile","tealium_profile":"android","tealium_environment":"dev","tealium_datasource":"","tealium_visitor_id":"69e0d1286fcc485bbe6e9d47045bf50f","tealium_library_name":"android-kotlin","tealium_library_version":"1.4.2","tealium_random":"1944650671034697","tealium_session_id":1661271030063,"app_uuid":"e5467900-ed41-44ef-a40f-eb7225f6ed71","google_adid":"b776faba-fe1f-4805-b649-057c10b8df4a","google_limit_ad_tracking":false,"google_app_set_id":"80763da2-aa24-4924-9014-36deebe19d82","google_app_set_scope":1,"key":5,"app_rdns":"com.tealium.mobile","app_name":"Tealium Kotlin Example","app_version":"1.0","app_build":"1","app_memory_usage":90,"connection_type":"wifi","device_connected":true,"carrier":"T-Mobile","carrier_iso":"us","carrier_mcc":"310","carrier_mnc":"260","device":"Google sdk_gphone64_x86_64","device_model":"sdk_gphone64_x86_64","device_manufacturer":"Google","device_architecture":"64bit","device_cputype":"x86_64","device_resolution":"1080x2072","device_logical_resolution":"1080x2340","device_android_runtime":"2.1.0","origin":"mobile","platform":"android","os_name":"Android","device_os_build":"7818354","device_os_version":"12","device_free_system_storage":50380800,"device_free_external_storage":5293375488,"device_orientation":"Portrait","device_language":"en-US","device_battery_percent":100,"device_ischarging":false,"timestamp":"2022-08-23T16:10:45Z","timestamp_local":"2022-08-23T17:10:45","timestamp_offset":"1","timestamp_unix":1661271045,"timestamp_unix_milliseconds":1661271045952,"timestamp_epoch":1661271045,"remote_commands":["localjsoncommand-0.0"],"enabled_modules":["AppData","Collect","Connectivity","DeviceData","RemoteCommands"],"enabled_modules_versions":["1.4.2","1.1.0","1.4.2","1.4.2","1.1.1"]}
    """.trimIndent())

    val testDispatch: Dispatch = TealiumEvent("", JsonUtils.mapFor(testDispatchJson))

    private var _count = 0

    @Before
    fun setUp() {
        MockKAnnotations.init(this)

//        val count = slot<Int>()
        val countList = mutableListOf<Int>()
        every { context.executors } returns executors
        every { context.dataLayer } returns dataLayer
        every { dataLayer.get("key") } answers { _count }
        every { dataLayer.putInt(any(), capture(countList), Expiry.SESSION) } answers {
            _count = countList.last()
        }
        every { executors.background } returns testScope
        every { loader.loadFromAsset(any()) } returns utag_js
    }

    @Test
    fun J2v8_Init() {
        testPerformance(listOf(
            TestTask("J2v8_Init") {
                val adapter = J2v8TransformationsAdapter(context, loader)
                adapter.init().await()
            }
        ))
    }

    @Test
    fun QuickJs_Init() {
        testPerformance(listOf(
            TestTask("QuickJs_Init") {
                val adapter = QuickJsTransformationsAdapter(context, loader)
                adapter.init().await()
            }
        ))
    }

    @Test
    fun J2v8_Transform() = runBlocking {
        val adapter = J2v8TransformationsAdapter(context, loader)
        adapter.init().await()

        testPerformance(listOf(
            TestTask("J2v8_Transform") {
                adapter.transform(dispatch = testDispatch)
                assertEquals("transformations", testDispatch["event_source"])
            }
        ))
    }

    @Test
    fun QuickJs_Transform() = runBlocking {
        val adapter = QuickJsTransformationsAdapter(context, loader)
        adapter.init().await()

        testPerformance(listOf(
            TestTask("QuickJs_Transform") {
                adapter.transform(dispatch = testDispatch)
                assertEquals("transformations", testDispatch["event_source"])
            }
        ))
    }

    private fun testPerformance(tasks: List<TestTask>, count: Int = 15) = runBlocking {
        val metricsList: List<TaskMetrics> = tasks.map { task ->
            averageExecutionTime(count, task).also {
                Log.d("metrics", "Task(${task.name}) took ${it.average}ms (avg) (max=${it.max}ms; min=${it.min}ms} ")
            }
        }

        val overallTime = metricsList.sumOf { metric -> metric.total.toInt() }
        val overallAverages = metricsList.sumOf { metric -> metric.average.toInt() }

        Log.d("metrics", " +++++ ")
        Log.d("metrics", "Total: ${overallTime}ms")
        Log.d("metrics", "Average: ${overallAverages}ms")
        Log.d("metrics", " +++++ ")
        Unit
    }

    private fun averageExecutionTime(count: Int, test: TestTask): TaskMetrics {
        val mapped = (1..count).map { _ ->
            measureTimeMillis {
                runBlocking {
                    test.task()
                }
            }
        }
        return TaskMetrics(
            total = mapped.sum(),
            average = mapped.average(),
            max = mapped.maxOf { it },
            min = mapped.minOf { it }
        )
    }

    private val utag_js = """
        var utag = {
            DB: function (m) {
                console.log(m)
            },
            extend: {
                "blr": [],
                "alr": [function (a, b) {
                    try {
                        if (1) {
                            b['event_source'] = 'transformations'
                        }
                    } catch (e) {
                        //        utag.DB(e);

                    }
                }
                    , function (a, b) {
                        try {
                            if (1) {
                                var stored = storage.read("key") || 0;
                                console.log("Storage value is: " + stored);
                                storage.save("key", ++stored, null);
                            }
                        } catch (e) {
                            //        utag.DB(e)
                        }
                    }
                    , function (a, b, c, d) {
                        b._ccity = (typeof b['customer_city'] != 'undefined') ? b['customer_city'] : '';
                        b._ccountry = (typeof b['customer_country'] != 'undefined') ? b['customer_country'] : '';
                        b._ccurrency = (typeof b['order_currency'] != 'undefined') ? b['order_currency'] : '';
                        b._ccustid = (typeof b['customer_id'] != 'undefined') ? b['customer_id'] : '';
                        b._corder = (typeof b['order_id'] != 'undefined') ? b['order_id'] : '';
                        b._cpromo = (typeof b['order_coupon_code'] != 'undefined') ? b['order_coupon_code'] : '';
                        b._cship = (typeof b['order_shipping'] != 'undefined') ? b['order_shipping'] : '';
                        b._cstate = (typeof b['customer_state'] != 'undefined') ? b['customer_state'] : '';
                        b._cstore = (typeof b['order_store'] != 'undefined') ? b['order_store'] : 'web';
                        b._csubtotal = (typeof b['order_subtotal'] != 'undefined') ? b['order_subtotal'] : '';
                        b._ctax = (typeof b['order_tax'] != 'undefined') ? b['order_tax'] : '';
                        b._ctotal = (typeof b['order_total'] != 'undefined') ? b['order_total'] : '';
                        b._ctype = (typeof b['order_type'] != 'undefined') ? b['order_type'] : '';
                        b._czip = (typeof b['customer_zip'] != 'undefined') ? b['customer_zip'] : '';
                        b._cprod = (typeof b['product_id'] != 'undefined' && b['product_id'].length > 0) ? b['product_id'].split(',') : [];
                        b._cprodname = (typeof b['product_name'] != 'undefined' && b['product_name'].length > 0) ? b['product_name'].split(',') : [];
                        b._cbrand = (typeof b['product_brand'] != 'undefined' && b['product_brand'].length > 0) ? b['product_brand'].split(',') : [];
                        b._ccat = (typeof b['product_category'] != 'undefined' && b['product_category'].length > 0) ? b['product_category'].split(',') : [];
                        b._ccat2 = (typeof b['product_subcategory'] != 'undefined' && b['product_subcategory'].length > 0) ? b['product_subcategory'].split(',') : [];
                        b._cquan = (typeof b['product_quantity'] != 'undefined' && b['product_quantity'].length > 0) ? b['product_quantity'].split(',') : [];
                        b._cprice = (typeof b['product_unit_price'] != 'undefined' && b['product_unit_price'].length > 0) ? b['product_unit_price'].split(',') : [];
                        b._csku = (typeof b['product_sku'] != 'undefined' && b['product_sku'].length > 0) ? b['product_sku'].split(',') : [];
                        b._cpdisc = (typeof b['product_discount'] != 'undefined' && b['product_discount'].length > 0) ? b['product_discount'].split(',') : [];
                        if (b._cprod.length == 0) {
                            b._cprod = b._csku.slice()
                        }
                        ; if (b._cprodname.length == 0) {
                            b._cprodname = b._csku.slice()
                        }
                        ; function tf(a) {
                            if (a == '' || isNaN(parseFloat(a))) {
                                return a
                            } else {
                                return (parseFloat(a)).toFixed(2)
                            }
                        }
                        ; b._ctotal = tf(b._ctotal);
                        b._csubtotal = tf(b._csubtotal);
                        b._ctax = tf(b._ctax);
                        b._cship = tf(b._cship);
                        for (c = 0; c < b._cprice.length; c++) {
                            b._cprice[c] = tf(b._cprice[c])
                        }
                        ; for (c = 0; c < b._cpdisc.length; c++) {
                            b._cpdisc[c] = tf(b._cpdisc[c])
                        }
                        ;
                    }
                ],
                "firebaseAnalytics" : [
                    function (a, b) {
                        try {
                            if (1) {
                                b['firebase_api'] = '12345'
                            }
                        } catch (e) {
                            //        utag.DB(e);
                        }
                    }
                ],
                "localjsoncommand": [
                    function (a, b) {
                        try {
                            if (1) {
                                b['hello'] = 'world'
                            }
                        } catch (e) {
                            //        utag.DB(e);
                        }
                    }
                ]
            },
            transform: function (a, b, c) {
                if (typeof b === "string") {
                    b = JSON.parse(b);
                }
                // else -> assume object

                var extend = utag.extend || {};
                var scope = extend[c || ""] || [];

                for (i = 0; i < scope.length; i++) {
                    scope[i](a, b)
                }
                return JSON.stringify(b);
            },
            transformJson: function (a, b, c) {
                    if (typeof b === "string") {
                        b = JSON.parse(b);
                    }
                    // else -> assume object

                    var extend = utag.extend || {};
                    var scope = extend[c || ""] || [];

                    for (i = 0; i < scope.length; i++) {
                        scope[i](a, b)
                    }
                    return b;
                }
        };
    """.trimIndent()
}

