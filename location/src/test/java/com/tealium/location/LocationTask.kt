package com.tealium.location

import android.app.Activity
import android.location.Location
import com.google.android.gms.tasks.OnFailureListener
import com.google.android.gms.tasks.OnSuccessListener
import com.google.android.gms.tasks.Task
import java.lang.Exception
import java.util.concurrent.Executor

class LocationTask(private val mockLocation: Location) : Task<Location>() {

    override fun isSuccessful(): Boolean {
        return true
    }

    override fun isCanceled(): Boolean {
        return false
    }

    override fun getResult(): Location? {
        return mockLocation
    }

    override fun <X : Throwable?> getResult(p0: Class<X>): Location? {
        return mockLocation
    }

    override fun getException(): Exception? {
        return null
    }

    override fun addOnSuccessListener(p0: OnSuccessListener<in Location>): Task<Location> {
        return LocationTask(mockLocation)
    }

    override fun addOnSuccessListener(p0: Executor, p1: OnSuccessListener<in Location>): Task<Location> {
        return LocationTask(mockLocation)
    }

    override fun addOnSuccessListener(p0: Activity, p1: OnSuccessListener<in Location>): Task<Location> {
        return LocationTask(mockLocation)
    }

    override fun addOnFailureListener(p0: OnFailureListener): Task<Location> {
        return this
    }

    override fun addOnFailureListener(p0: Executor, p1: OnFailureListener): Task<Location> {
        return this
    }

    override fun addOnFailureListener(p0: Activity, p1: OnFailureListener): Task<Location> {
        return this
    }

    override fun isComplete(): Boolean {
        return true
    }
}