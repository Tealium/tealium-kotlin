package com.tealium.core.persistence

import android.app.Application
import android.content.Context
import android.database.sqlite.SQLiteOpenHelper
import androidx.test.core.app.ApplicationProvider
import com.tealium.core.Environment
import com.tealium.core.TealiumConfig
import com.tealium.dispatcher.TealiumEvent
import io.mockk.*
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.TimeUnit

class DispatchStorageTests {

    private lateinit var dbHelper: DatabaseHelper
    private lateinit var dispatchStore: DispatchStorageDao
    private val tableName = "dispatches"

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val config = TealiumConfig(context as Application, "test", "test", Environment.DEV)
        dbHelper = DatabaseHelper(config, null) // in-memory
        dispatchStore = DispatchStorageDao(dbHelper, tableName, 20)
    }

    @Test
    fun testPushPopIndividual() {
        val timestamp = getTimestamp()
        val item = PersistentItem(
            "key_1",
            JSONObject().toString(),
            Expiry.FOREVER,
            timestamp, Serialization.JSON_OBJECT
        )
        dispatchStore.enqueue(item)

        val fromStore = dispatchStore.dequeue()
        assertNotNull(fromStore)
        assertEquals(0, dispatchStore.count()) // dequeue should also remove.

        assertEquals(item.key, fromStore?.key)
        assertEquals(item.value.toString(), fromStore?.value.toString())
        assertEquals(item.expiry, fromStore?.expiry)
        assertEquals(item.timestamp, fromStore?.timestamp)
    }

    @Test
    fun testPopMultiples() {
        val count = 10
        val timestamp = getTimestamp()
        prepopulate(count, timestamp)
        assertEquals(count, dispatchStore.count())

        // Should be in timestamp sorted order
        val firstFive = dispatchStore.dequeue(5)
        assertNotNull(firstFive)
        assertEquals(5, firstFive.count()) // dequeue should also remove.
        assertEquals(5, dispatchStore.count()) // dequeue should also remove.

        firstFive.forEachIndexed { i, v ->
            assertEquals("key_${i + 1}", v.key)
            assertEquals(timestamp + 100 * (i + 1), v.timestamp)
        }

        // Should be in timestamp sorted order
        val lastFive = dispatchStore.dequeue(5)
        assertNotNull(lastFive)
        assertEquals(5, lastFive.count()) // dequeue should also remove.
        assertEquals(0, dispatchStore.count()) // dequeue should also remove.

        lastFive.forEachIndexed { i, v ->
            val index = i + 6
            assertEquals("key_$index", v.key)
            assertEquals(timestamp + 100 * index, v.timestamp)
        }
    }

    @Test
    fun testPopOrdering() {
        val timestamp = getTimestamp()
        // three items with the same timestamp.
        val item1 = PersistentItem(
            "key_1",
            JSONObject().toString(),
            Expiry.FOREVER,
            timestamp, Serialization.JSON_OBJECT
        )
        val item2 = PersistentItem(
            "key_2",
            JSONObject().toString(),
            Expiry.FOREVER,
            timestamp, Serialization.JSON_OBJECT
        )
        val item3 = PersistentItem(
            "key_3",
            JSONObject().toString(),
            Expiry.FOREVER,
            timestamp, Serialization.JSON_OBJECT
        )

        dispatchStore.enqueue(item2)
        dispatchStore.enqueue(item1)
        dispatchStore.enqueue(item3)

        val shouldBeItem2 = dispatchStore.dequeue()
        val shouldBeItem1 = dispatchStore.dequeue()
        val shouldBeItem3 = dispatchStore.dequeue()

        assertEquals(item2.key, shouldBeItem2?.key)
        assertEquals(item1.key, shouldBeItem1?.key)
        assertEquals(item3.key, shouldBeItem3?.key)
    }

    @Test
    fun testPopOverRequest() {
        val count = 10
        val timestamp = getTimestamp()
        prepopulate(10, timestamp)

        assertEquals(count, dispatchStore.count())

        // Should be in timestamp sorted order
        // Requesting more than is available
        val allDispatches = dispatchStore.dequeue(count + 5)
        assertNotNull(allDispatches)
        assertEquals(count, allDispatches.count()) // dequeue should also remove.
        assertEquals(0, dispatchStore.count()) // dequeue should also remove.

        allDispatches.forEachIndexed { i, v ->
            assertEquals("key_${i + 1}", v.key)
            assertEquals(timestamp + 100 * (i + 1), v.timestamp)
        }
    }

    @Test
    fun testTimestampNotNull() {
        val item = PersistentItem(
            "key",
            JSONObject().toString(),
            Expiry.FOREVER,
            null, Serialization.JSON_OBJECT
        )

        dispatchStore.enqueue(item)
        val fromStore = dispatchStore.dequeue()
        assertNotNull(fromStore)
        assertNotNull(fromStore?.timestamp)
    }

    @Test
    fun testPopWhenQueueEmpty() {
        assertEquals(0, dispatchStore.count())

        val nullItem = dispatchStore.dequeue()
        assertNull(nullItem)

        val emptyList = dispatchStore.dequeue(10)
        assertNotNull(emptyList)
        assertFalse(emptyList.isNotEmpty())
    }

    @Test
    fun testStorageSizingSingle() {
        assertEquals(20, dispatchStore.maxQueueSize)
        val timestamp = getTimestamp()
        prepopulate(20, timestamp)

        assertEquals(dispatchStore.maxQueueSize, dispatchStore.count())
        val item21 = PersistentItem(
            "key_21",
            JSONObject().toString(),
            Expiry.FOREVER,
            timestamp + 100 * 21, Serialization.JSON_OBJECT
        )
        dispatchStore.enqueue(item21)

        // should still be size 20
        // key_1 should have dropped off the end of the queue
        assertEquals(dispatchStore.maxQueueSize, dispatchStore.count())
        val keyTwo = dispatchStore.dequeue()
        assertEquals(dispatchStore.maxQueueSize - 1, dispatchStore.count())
        assertNotNull(keyTwo)
        assertEquals("key_2", keyTwo?.key)
    }

    @Test
    fun testStorageSizingMultiple() {
        assertEquals(20, dispatchStore.maxQueueSize)
        val count = 20
        val timestamp = getTimestamp()
        prepopulate(count, timestamp)

        // enqueue another 5 onto the queue
        val listOfFive = mutableListOf<PersistentItem>()
        repeat(5) {
            val i = 21 + it
            val item = PersistentItem(
                "key_$i",
                JSONObject().toString(),
                Expiry.FOREVER,
                timestamp + 100 * i, Serialization.JSON_OBJECT
            )
            listOfFive.add(item)
        }
        dispatchStore.enqueue(listOfFive)
        assertEquals(dispatchStore.maxQueueSize, dispatchStore.count())
        val keysFiveToTen = dispatchStore.dequeue(5)

        // key_1 .. key_5 should have dropped off.
        keysFiveToTen.forEachIndexed { i, v ->
            assertEquals("key_${i + 6}", v.key)
            assertEquals(timestamp + 100 * (i + 6), v.timestamp)
        }
    }

    @Test
    fun testStorageResize() {
        assertEquals(20, dispatchStore.maxQueueSize)
        val count = 20
        val timestamp = getTimestamp()
        prepopulate(count, timestamp)

        assertEquals(dispatchStore.maxQueueSize, dispatchStore.count())

        var newSize = 10
        dispatchStore.resize(newSize)
        assertEquals(newSize, dispatchStore.maxQueueSize)
        assertEquals(newSize, dispatchStore.count())

        val keysTenToFifteen = dispatchStore.dequeue(5)
        // key_1 .. key_10 should have dropped off.
        keysTenToFifteen.forEachIndexed { i, v ->
            assertEquals("key_${i + 11}", v.key)
            assertEquals(timestamp + 100 * (i + 11), v.timestamp)
        }

        newSize = 20
        dispatchStore.resize(newSize)
        assertEquals(newSize, dispatchStore.maxQueueSize)
        assertEquals(
            5,
            dispatchStore.count()
        ) // maxQueueSize was 10, and we've popped 5 off already.

        val keysSixteenToTwenty = dispatchStore.dequeue(5)
        assertEquals(0, dispatchStore.count())
        // queue size was increased, nothing should have been removed.
        keysSixteenToTwenty.forEachIndexed { i, v ->
            assertEquals("key_${i + 16}", v.key)
            assertEquals(timestamp + 100 * (i + 16), v.timestamp)
        }
    }

    @Test
    fun testExpiryDates() {
        for (days in arrayOf(-1, 1, 10)) {
            dispatchStore.expiryDays = days
            val nullExpiry = PersistentItem(
                "null",
                JSONObject().toString(),
                null,
                null, Serialization.JSON_OBJECT
            )

            val tomorrow = Expiry.afterTimeUnit(1L, TimeUnit.DAYS)
            val expiresTomorrow = PersistentItem(
                "tomorrow",
                JSONObject().toString(),
                tomorrow,
                null, Serialization.JSON_OBJECT
            )

            dispatchStore.enqueue(nullExpiry)
            dispatchStore.enqueue(expiresTomorrow)

            val nullExpiryFromStore = dispatchStore.dequeue()
            val expiresTomorrowFromStore = dispatchStore.dequeue()

            assertEquals(nullExpiry.expiry, nullExpiryFromStore?.expiry)
            if (days < 0) {
                assertEquals(Expiry.FOREVER, nullExpiryFromStore?.expiry)
            } else {
                assertEquals(
                    Expiry.afterTimeUnit(days.toLong(), TimeUnit.DAYS),
                    nullExpiryFromStore?.expiry
                )
            }
            assertEquals(expiresTomorrow.expiry, expiresTomorrowFromStore?.expiry)
            assertEquals(tomorrow, expiresTomorrowFromStore?.expiry)
        }
    }

    @Test
    fun testPurgeExpired() {
        val timestamp = getTimestamp()
        // three items with the same timestamp.
        val expired = PersistentItem(
            "key_1",
            JSONObject().toString(),
            Expiry.afterEpochTime(timestamp - 1000), // already expired
            timestamp, Serialization.JSON_OBJECT
        )
        val notExpired = PersistentItem(
            "key_2",
            JSONObject().toString(),
            Expiry.afterEpochTime(timestamp + 1000),
            timestamp, Serialization.JSON_OBJECT
        )
        val forever = PersistentItem(
            "key_3",
            JSONObject().toString(),
            Expiry.FOREVER,
            timestamp, Serialization.JSON_OBJECT
        )
        val session = PersistentItem(
            "key_4",
            JSONObject().toString(),
            Expiry.SESSION,
            timestamp, Serialization.JSON_OBJECT
        )

        dispatchStore.enqueue(expired)
        dispatchStore.enqueue(notExpired)
        dispatchStore.enqueue(forever)
        dispatchStore.enqueue(session)

        assertEquals(3, dispatchStore.count())
        var cursor = dbHelper.readableDatabase.query(
            tableName,
            null,
            null,
            null,
            null,
            null,
            null
        )
        assertEquals(4, cursor.count)

        dispatchStore.purgeExpired()
        assertEquals(3, dispatchStore.count())
        cursor = dbHelper.readableDatabase.query(
            tableName,
            null,
            null,
            null,
            null,
            null,
            null
        )
        assertEquals(3, cursor.count)

    }

    @Test
    fun testDequeueReturnsNullWhenQueueAndDaoAreEmpty() {
        val dispatchStorage = DispatchStorage(dbHelper, "table", ConcurrentLinkedQueue(), dispatchStore)
        val dequeuedItem = dispatchStorage.dequeue()

        assertNull(dequeuedItem)
    }

    @Test
    fun testEnqueueAddsToQueueWhenDbIsNull() {
        val dbHelper = mockk<DatabaseHelper>()
        every { dbHelper.db } returns null
        every { dbHelper.writableDatabase } returns null
        every { dbHelper.onDbReady(any()) } just Runs

        val dispatchStorage = DispatchStorage(
            dbHelper,
            "table",
            ConcurrentLinkedQueue(),
            DispatchStorageDao(dbHelper, tableName, 20)
        )
        val dispatch = TealiumEvent("test")

        dispatchStorage.enqueue(dispatch)

        assertEquals(1, dispatchStorage.queue.size)
    }

    @Test
    fun testDequeueReturnsItemFromQueueWhenQueueIsNotEmpty() {
        val dispatchStorage = DispatchStorage(dbHelper, "table", ConcurrentLinkedQueue(), dispatchStore)
        val dispatch = TealiumEvent("test")

        dispatchStorage.queue.add(dispatch)
        val dequeuedItem = dispatchStorage.dequeue()

        assertEquals(dispatch, dequeuedItem)
        assertEquals(0, dispatchStorage.queue.size)
    }

    @Test
    fun testDequeueReturnsItemFromDaoWhenQueueIsEmpty() {
        val dispatchStorage = DispatchStorage(dbHelper, "table", ConcurrentLinkedQueue(), dispatchStore)
        val dispatch = TealiumEvent("test")

        dispatchStorage.enqueue(dispatch)
        val dequeuedItem = dispatchStorage.dequeue()

        assertEquals(dispatch.id, dequeuedItem?.id)
        assertEquals(dispatch.timestamp, dequeuedItem?.timestamp)
        assertEquals(dispatch.payload(), dequeuedItem?.payload())
    }

    /**
     * Prepopulates the storage with [count] number of items. Items are generated with ascending keys
     * using the pattern "key_1" -> "key_n" and timestamps are ascending in intervals of 100, i.e.
     * key_1 will have a timestamp equal to: [timestamp + 100 * n]
     * Key's are actually inserted in reverse order to ensure that the "oldest" key, "key_1" is actually
     * inserted last, but should still come out of the storage via [dequeue()] first
     */
    fun prepopulate(n: Int, startingTimestamp: Long) {
        repeat(n) {
            // reverse the key_X and timestamp
            // puts in order of key_10 -> key_1
            val i = n - it
            val item = PersistentItem(
                "key_$i",
                JSONObject().toString(),
                Expiry.FOREVER,
                startingTimestamp + 100 * i, Serialization.JSON_OBJECT
            )
            dispatchStore.enqueue(item)
        }
    }

    @After
    fun tearDown() {
        // clear all.
        dbHelper.db?.delete("dispatches", null, null)
    }
}