package com.tealium.transformations.internal.impl

import android.icu.lang.UCharacter.GraphemeClusterBreak.V
import android.system.Os.read
import com.eclipsesource.v8.JavaCallback
import com.eclipsesource.v8.V8
import com.eclipsesource.v8.V8Array
import com.eclipsesource.v8.V8Object
import com.tealium.core.JsonLoader
import com.tealium.core.Loader
import com.tealium.core.Logger
import com.tealium.core.Tealium
import com.tealium.core.TealiumContext
import com.tealium.core.messaging.InstanceShutdownListener
import com.tealium.dispatcher.Dispatch
import com.tealium.transformations.Transformation
import com.tealium.transformations.internal.JsConsole
import com.tealium.transformations.internal.Storage
import com.tealium.transformations.internal.TransformationsAdapter
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.lang.ref.WeakReference


class J2v8TransformationsAdapter(
    private val context: TealiumContext,
    private val loader: Loader = JsonLoader.getInstance(context.config.application)
) : TransformationsAdapter, InstanceShutdownListener {

    private lateinit var v8: V8
    private lateinit var utag: V8Object
    private val background = context.executors.background
    private val storage: Storage = StorageImpl(context.dataLayer)
    private val console: JsConsole = JsConsoleImpl()

    private val hashCallback: JavaCallback = JavaCallback { obj, arr ->
        arr.getString(0)?.let { alg ->
            arr.getString(1)?.let { input ->
                Logger.dev(TAG, "hashing...")
                UtilImpl.hash(alg, input)
            }
        }
    }
    private val storageSaveCallback: JavaCallback = JavaCallback { obj, arr ->
        arr.getString(0)?.let { key ->
            arr.get(1)?.let { value ->
                storage.save(key, value, "session")
            }
        }
    }
    private val storageReadCallback: JavaCallback = JavaCallback { obj, arr ->
        arr.getString(0)?.let { key ->
            storage.read(key)
        }
    }
    private val consoleCallback: JavaCallback = JavaCallback { obj, arr ->
        val method = obj.keys.first()
        val message = arr.getString(0)
        message?.let {
            when (method) {
                "log" -> console.log(message)
                "info" -> console.info(message)
                "error" -> console.error(message)
            }
        }
        null
    }

    override fun init(): Deferred<Unit> {
        return background.async {
            v8 = V8.createV8Runtime()


            val js = loader.loadFromAsset(QuickJsTransformationsAdapter.UTAG_JS)

            js?.let {
                v8.executeVoidScript(it)
                utag = v8.getObject("utag")

                val util = V8Object(v8)
                v8.add("util", util)
                util.registerJavaMethod(
                    hashCallback,
                    "hash"
                )

                val storage = V8Object(v8)
                v8.add("storage", storage)
                storage.registerJavaMethod(
                    storageSaveCallback,
                    "save"
                )
                storage.registerJavaMethod(
                    storageReadCallback,
                    "read"
                )

                val console = V8Object(v8)
                v8.add("console", console)
                console.registerJavaMethod(
                    consoleCallback,
                    "log"
                )
                console.registerJavaMethod(
                    consoleCallback,
                    "info"
                )
                console.registerJavaMethod(
                    consoleCallback,
                    "error"
                )
            }
        }
    }

    override fun onInstanceShutdown(name: String, instance: WeakReference<Tealium>) {
        v8.release(false)
        utag.release()
    }

    override fun executeJavascript(js: String) {
        background.launch {
            val result = v8.executeScript(js)
            Logger.dev(TAG, "$result")
        }
    }

    override suspend fun transform(dispatch: Dispatch) {
        withContext(background.coroutineContext) {
            val params = V8Array(v8).push("link")
                .push(dispatch.payload().asV8Object(v8))
                .push("alr")

            val results = utag.executeObjectFunction(
                "transformJson",
                params
            )

            merge(results, dispatch)
        }
    }

    private fun merge(obj: V8Object, dispatch: Dispatch) {
        val transformed = obj.asMap()

        dispatch.addAll(transformed)
    }

    private fun V8Object.asMap(): Map<String, Any> {
        val map = mutableMapOf<String, Any>()
        for (key in keys) {
            val obj = get(key)
            when (obj) {
                is V8Object -> map[key] = obj.asMap()
                is V8Array -> map[key] = obj.asCollection()
                else -> map[key] = obj
            }
        }
        return map
    }

    private fun V8Array.asCollection(): Collection<Any> {
        val collection = mutableListOf<Any>()
        for (key in keys) {
            val obj = get(key)
            when (obj) {
                is V8Object -> collection.add(obj.asMap())
                is V8Array -> collection.add(obj.asCollection())
                else -> collection.add(obj)
            }
        }
        return collection
    }

    private fun Map<String, *>.asV8Object(v8: V8): V8Object {
        val obj = V8Object(v8)
        for (key in keys) {
            when (val value = get(key)) {
                is String -> obj.add(key, value)
                is Int -> obj.add(key, value)
                is Double -> obj.add(key, value)
                is Boolean -> obj.add(key, value)
                is JSONObject -> obj.add(key, value.asV8Object(v8))
                is JSONArray -> obj.add(key, value.asV8Array(v8))
                is Map<*,*> -> obj.add(key, (value as? Map<String, Any>)?.asV8Object(v8))
                is Collection<*> -> obj.add(key, (value as? Collection<Any>)?.asV8Array(v8))
            }
        }
        return obj
    }

    private fun Collection<Any>.asV8Array(v8: V8): V8Array {
        val arr = V8Array(v8)
        val iter = iterator()
        while (iter.hasNext()) {
            val item = iter.next()
            when (val value = item) {
                is String -> arr.push(value)
                is Int -> arr.push(value)
                is Double -> arr.push(value)
                is Boolean -> arr.push(value)
                is JSONObject -> arr.push(value.asV8Object(v8))
                is JSONArray -> arr.push(value.asV8Array(v8))
                is Map<*,*> -> arr.push((value as? Map<String, Any>)?.asV8Object(v8))
                is Collection<*> -> arr.push((value as? Collection<Any>)?.asV8Array(v8))
            }
        }
        return arr
    }

    private fun JSONObject.asV8Object(v8: V8): V8Object {
        val obj = V8Object(v8)
        for (key in keys()) {
            when (val value = get(key)) {
                is String -> obj.add(key, value)
                is Int -> obj.add(key, value)
                is Double -> obj.add(key, value)
                is Boolean -> obj.add(key, value)
                is JSONObject -> obj.add(key, value.asV8Object(v8))
                is JSONArray -> obj.add(key, value.asV8Array(v8))
            }
        }
        return obj
    }

    private fun JSONArray.asV8Array(v8: V8): V8Array {
        val arr = V8Array(v8)
        for (i in 0 until length()) {
            when (val value = get(i)) {
                is String -> arr.push(value)
                is Int -> arr.push(value)
                is Double -> arr.push(value)
                is Boolean -> arr.push(value)
                is JSONObject -> arr.push(value.asV8Object(v8))
                is JSONArray -> arr.push(value.asV8Array(v8))
            }
        }
        return arr
    }

    override fun getTransformation(scope: String): Transformation {
        return Transformation { }
    }

    companion object {
        private val TAG = J2v8TransformationsAdapter::class.java.simpleName
        const val UTAG_JS = "utag.js"
    }
}