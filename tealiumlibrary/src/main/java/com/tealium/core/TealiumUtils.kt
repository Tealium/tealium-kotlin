package com.tealium.core

import android.content.Context
import android.os.Looper

class TealiumUtils {

    companion object {
        fun isMainThread(): Boolean {
            return Looper.getMainLooper() == Looper.myLooper()
        }

        fun getAppVersion(context: Context): String {
            val packageName = context.packageName
            val packageInfo = context.packageManager.getPackageInfo(packageName, 0)
            return packageInfo.versionName
        }
    }
}
