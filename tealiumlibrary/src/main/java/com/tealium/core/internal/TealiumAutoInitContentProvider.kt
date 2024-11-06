package com.tealium.core.internal

import android.app.Application
import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.net.Uri
import android.util.Log
import com.tealium.core.ActivityManager
import com.tealium.tealiumlibrary.BuildConfig

class TealiumAutoInitContentProvider: ContentProvider() {

    override fun onCreate(): Boolean {
        try {
            Log.d(BuildConfig.TAG, "Auto-initializing Tealium")
            val app = context!!.applicationContext as? Application
            if (app != null) {
                ActivityManager.getInstance(app)
                Log.d(BuildConfig.TAG, "Auto-init successful")
            } else {
                Log.d(BuildConfig.TAG, "Auto-init failed.")
            }
        } catch (ignore: NullPointerException) {
        }

        return false
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