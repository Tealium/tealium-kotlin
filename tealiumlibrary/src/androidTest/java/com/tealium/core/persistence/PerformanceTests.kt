package com.tealium.core.persistence

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.test.core.app.ApplicationProvider
import com.tealium.core.Environment
import com.tealium.core.TealiumConfig
import io.mockk.mockk
import org.json.JSONObject
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.system.measureTimeMillis

class PerformanceTests {

    private lateinit var dbHelper: DatabaseHelper
    private lateinit var storage: DataLayer

    private val commonKeys = arrayOf(
        "string",
        "int",
        "double",
        "long",
        "boolean",
        "json",
        "string_array",
        "int_array",
        "double_array",
        "long_array",
        "boolean_array"
    )

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val config = TealiumConfig(context as Application, "test", "test", Environment.DEV)
//        dbHelper = DatabaseHelper(config, null) // in-memory
        dbHelper = DatabaseHelper(config, "test.db") // on-disk
        storage = PersistentStorage(dbHelper, "datalayer", eventRouter = mockk())

        storage.putString("string", "String", Expiry.FOREVER)
        storage.putInt("int", 1, Expiry.UNTIL_RESTART)
        storage.putDouble("double", 2.0)
        storage.putLong("long", 2L)
        storage.putBoolean("boolean", true)
        storage.putJsonObject("json", JSONObject())
        storage.putStringArray("string_array", arrayOf("String"))
        storage.putIntArray("int_array", arrayOf(1))
        storage.putDoubleArray("double_array", arrayOf(2.0))
        storage.putLongArray("long_array", arrayOf(2L))
        storage.putBooleanArray("boolean_array", arrayOf(true))
    }

    @After
    fun tearDown() {
        // clear all.
        dbHelper.db?.delete("datalayer", null, null)
    }

    private fun averageExecutionTime(count: Int, test: TestTask): TaskMetrics {
        val mapped = (1..count).map { _ -> measureTimeMillis { test.task() } }
        return TaskMetrics(mapped.sum(), mapped.average())
    }

    @Test
    fun performanceTest() {
        val count = 5
        val tasks = arrayOf(
            TestTask("put") {
                storage.putString("string", "String", Expiry.FOREVER)
                storage.putInt("int", 1, Expiry.UNTIL_RESTART)
                storage.putDouble("double", 2.0)
                storage.putLong("long", 2L)
                storage.putBoolean("boolean", true)
                storage.putJsonObject("json", JSONObject())
                storage.putStringArray("string_array", arrayOf("String"))
                storage.putIntArray("int_array", arrayOf(1))
                storage.putDoubleArray("double_array", arrayOf(2.0))
                storage.putLongArray("long_array", arrayOf(2L))
                storage.putBooleanArray("boolean_array", arrayOf(true))
            },
            TestTask("get") {
                commonKeys.forEach { storage.get(it) }
            },
            TestTask("getAll") {
                val map = storage.all()
            },
            TestTask("count") {
                val itemCount = storage.count()
            },
            TestTask("keys") {
                val keys = storage.keys()
            },
            TestTask("contains") {
                commonKeys.forEach { storage.contains(it) }
            },
            TestTask("expiry") {
                commonKeys.forEach { storage.getExpiry(it) }
            },
            TestTask("remove") {
                commonKeys.forEach { storage.remove(it) }
            })

        val metricsList: List<TaskMetrics> = tasks.map { task ->
            averageExecutionTime(count, task).also {
                Log.d("TestTask", "Task(${task.name}) took ${it.average}ms (avg)")
            }
        }

        val overallTime = metricsList.sumBy { metric -> metric.total.toInt() }
        val overallAverages = metricsList.sumBy { metric -> metric.average.toInt() }

        Log.d("TestTask", " +++++ ")
        Log.d("TestTask", "Total: ${overallTime}ms")
        Log.d("TestTask", "Average: ${overallAverages}ms")
        Log.d("TestTask", " +++++ ")
    }

    data class TestTask(
        val name: String,
        val task: () -> Unit
    )

    data class TaskMetrics(
        val total: Long,
        val average: Double
    )

}