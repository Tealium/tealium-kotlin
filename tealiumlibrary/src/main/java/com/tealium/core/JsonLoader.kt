package com.tealium.core

import android.app.Application
import com.tealium.tealiumlibrary.BuildConfig
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import org.json.JSONTokener
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import javax.net.ssl.HttpsURLConnection

interface Loader {
    /**
     * Loads the string data from the given file. Returns null if the file does not exist.
     */
    fun loadFromFile(file: File): String?

    /**
     * Loads the string data from the given Asset file. Returns null if the file does not exist.
     */
    fun loadFromAsset(fileName: String): String?

    /**
     * Loads the string data from the given URL. Returns null if the file does not exist or the JSON
     * data is invalid.
     */
    fun loadFromUrl(url: URL): Any?
}

class JsonLoader private constructor(val application: Application) : Loader {

    override fun loadFromFile(file: File): String? {
        return if (file.exists()) {
           file.readText(Charsets.UTF_8)
        } else {
            Logger.dev(BuildConfig.TAG, "File not found (${file.name})")
            null
        }
    }

    override fun loadFromAsset(fileName: String): String? {
        return try {
            application.assets.open(fileName).bufferedReader().use {
                return it.readText()
            }
        } catch (ioe: IOException) {
            Logger.qa(BuildConfig.TAG, "Asset not found ($fileName)")
            null
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

    companion object {
        @Volatile private var instance: JsonLoader? = null

        fun getInstance(application: Application): JsonLoader = instance ?: synchronized(this) {
            instance ?: JsonLoader(application).also { instance = it }
        }
    }
}