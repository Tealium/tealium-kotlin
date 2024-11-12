package com.tealium.core.internal

import android.app.Application
import android.content.ComponentName
import android.content.ContentProvider
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import com.tealium.core.ActivityManager
import com.tealium.tealiumlibrary.BuildConfig

/**
 * The [TealiumInitProvider] is responsible for registering Tealium components that are
 * required at application initialization - this happens during the [ContentProvider.onCreate] method.
 *
 * Although it extends [ContentProvider] it is a non-functional implementation and should not be used as such.
 */
class TealiumInitProvider : ContentProvider() {

    private val COMPONENT_NAME = "com.tealium.core.internal.TealiumInitProvider"
    private val TIMEOUT_METADATA_KEY = "$COMPONENT_NAME.TIMEOUT_SECONDS"

    override fun onCreate(): Boolean {
        initializeActivityManager()

        return false
    }

    private fun initializeActivityManager() {
        Log.d(BuildConfig.TAG, "Initializing Tealium ActivityManager")

        val app = context?.applicationContext as? Application
        if (app == null) {
            Log.d(BuildConfig.TAG, "Tealium ActivityManager init failed; Context was null.")
            return
        }

        val timeout = getTimeout(app)
        if (timeout != null) {
            ActivityManager.getInstance(app, timeout.toLong())
        } else {
            ActivityManager.getInstance(app)
        }
        Log.d(BuildConfig.TAG, "Tealium ActivityManager init successful")
    }

    private fun getTimeout(context: Context): Int? {
        val metaData = getMetaData(context) ?: return null

        val timeout = metaData.getInt(TIMEOUT_METADATA_KEY, Int.MIN_VALUE)
        return if (timeout != Int.MIN_VALUE) {
            timeout
        } else {
            null
        }
    }

    private fun getMetaData(context: Context): Bundle? {
        return try {
            val componentName = ComponentName(context, COMPONENT_NAME)
            val appInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.getProviderInfo(
                    componentName,
                    PackageManager.ComponentInfoFlags.of(PackageManager.GET_META_DATA.toLong())
                )
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getProviderInfo(
                    componentName,
                    PackageManager.GET_META_DATA
                )
            }
            appInfo.metaData
        } catch (e: Exception) {
            null
        }
    }

    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?
    ): Cursor? {
        return null
    }

    override fun getType(uri: Uri): String? {
        return null
    }

    override fun insert(uri: Uri, values: ContentValues?): Uri? {
        return null
    }

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int {
        return 0
    }

    override fun update(
        uri: Uri,
        values: ContentValues?,
        selection: String?,
        selectionArgs: Array<out String>?
    ): Int {
        return 0
    }
}