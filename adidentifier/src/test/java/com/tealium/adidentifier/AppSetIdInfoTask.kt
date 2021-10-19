package com.tealium.adidentifier

import android.app.Activity
import com.google.android.gms.appset.AppSetIdInfo
import com.google.android.gms.tasks.OnFailureListener
import com.google.android.gms.tasks.OnSuccessListener
import com.google.android.gms.tasks.Task
import java.lang.Exception
import java.util.concurrent.Executor

class AppSetIdInfoTask(private val mockAppSetIdInfo: AppSetIdInfo) : Task<AppSetIdInfo>() {

    override fun isSuccessful(): Boolean {
        return true
    }

    override fun isCanceled(): Boolean {
        return false
    }

    override fun getResult(): AppSetIdInfo? {
        return mockAppSetIdInfo
    }

    override fun <X : Throwable?> getResult(p0: Class<X>): AppSetIdInfo? {
        return mockAppSetIdInfo
    }

    override fun getException(): Exception? {
        return null
    }

    override fun addOnSuccessListener(p0: OnSuccessListener<in AppSetIdInfo>): Task<AppSetIdInfo> {
        return AppSetIdInfoTask(mockAppSetIdInfo)
    }

    override fun addOnSuccessListener(p0: Executor, p1: OnSuccessListener<in AppSetIdInfo>): Task<AppSetIdInfo> {
        return AppSetIdInfoTask(mockAppSetIdInfo)
    }

    override fun addOnSuccessListener(p0: Activity, p1: OnSuccessListener<in AppSetIdInfo>): Task<AppSetIdInfo> {
        return AppSetIdInfoTask(mockAppSetIdInfo)
    }

    override fun addOnFailureListener(p0: OnFailureListener): Task<AppSetIdInfo> {
        return this
    }

    override fun addOnFailureListener(p0: Executor, p1: OnFailureListener): Task<AppSetIdInfo> {
        return this
    }

    override fun addOnFailureListener(p0: Activity, p1: OnFailureListener): Task<AppSetIdInfo> {
        return this
    }

    override fun isComplete(): Boolean {
        return true
    }
}