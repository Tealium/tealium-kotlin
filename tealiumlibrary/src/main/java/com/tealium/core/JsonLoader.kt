package com.tealium.core

import android.app.Application
import com.tealium.tealiumlibrary.BuildConfig
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import org.json.JSONTokener
import java.net.HttpURLConnection
import java.net.URL
import javax.net.ssl.HttpsURLConnection

interface Loader {
    fun loadFromAsset(fileName: String): String
    fun loadFromUrl(url: URL): Any?
}

class JsonLoader(val application: Application): Loader {

    override fun loadFromAsset(fileName: String): String {
        application.assets.open(fileName).bufferedReader().use {
            return it.readText()
        }
    }

    override fun loadFromUrl(url: URL): Any? {
        with(url.openConnection() as HttpURLConnection) {
            requestMethod = "GET"  // optional default is GET
            if (responseCode == HttpsURLConnection.HTTP_OK) {
                val inputString = inputStream.bufferedReader().readText()
                if (isValidJson(inputString)) {
                    return when (JSONTokener(inputString).nextValue()) {
                        is JSONObject -> JSONObject(inputString)
                        is JSONArray -> JSONArray(inputString)
                        else -> null
                    }
                }
            }
        }
        return null
    }

    private fun isValidJson(input: String): Boolean {
        try {
            JSONTokener(input).nextValue()
        } catch (ex: JSONException) {
            Logger.dev(BuildConfig.TAG, "Invalid JSON input: $input")
            return false
        }
        return true
    }

}