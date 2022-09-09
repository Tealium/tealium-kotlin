package com.tealium.transformations.quickjs

import app.cash.quickjs.QuickJs
import app.cash.quickjs.QuickJsException
import com.tealium.core.JsonLoader
import com.tealium.core.JsonUtils
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
import com.tealium.transformations.internal.Util
import com.tealium.transformations.internal.impl.StorageImpl
import com.tealium.transformations.internal.impl.UtilImpl
import com.tealium.transformations.quickjs.internal.QuickJsConsoleImpl
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.lang.ref.WeakReference

class QuickJsTransformationsAdapter(
    private val context: TealiumContext,
    private val loader: Loader = JsonLoader.getInstance(context.config.application)
) : TransformationsAdapter, InstanceShutdownListener {

    private lateinit var engine: QuickJs
    private lateinit var utag: Utag
    private val background = context.executors.background

    override fun init(): Deferred<Unit> {
        return background.async {
            try {
                engine = QuickJs.create()
                engine.set("console", JsConsole::class.java, QuickJsConsoleImpl())
                engine.set("util", Util::class.java, UtilImpl)
                engine.set("storage", Storage::class.java, StorageImpl(context.dataLayer))

                val js = loader.loadFromAsset(UTAG_JS)
//                val js = context.httpClient.get("https://tags.tiqcdn.com/utag/services-james/lib-mobile/dev/utag.js")
                js?.let {
                    engine.evaluate(it, UTAG_JS)
                    utag = engine.get("utag", Utag::class.java)
                }
            } catch (ex: Exception) {
                Logger.qa(TAG, "Error loading utag.js - ${ex.message}")
            }
        }
    }

    override fun onInstanceShutdown(name: String, instance: WeakReference<Tealium>) {
        engine.close()
    }

    override fun executeJavascript(js: String) {
        executeJavascript(js, UTAG_JS)
    }

    private fun executeJavascript(js: String, file: String) {
        try {
            val result = engine.evaluate(js, file)
            Logger.dev(TAG, "$result")
        } catch (ex: QuickJsException) {
        }
    }

    override suspend fun transform(dispatch: Dispatch) {
        withContext(background.coroutineContext) {
            try {
                val str = utag.transform(
                    "link",
                    JSONObject(dispatch.payload()).toString(),
                    "alr"
                )

                merge(str, dispatch)
            } catch (ex: QuickJsException) {
                Logger.qa(TAG, "QuickJS Evaluation issue: ${ex.message}")
            }
        }
    }

    override fun getTransformation(scope: String): Transformation {
        return Transformation { dispatch ->
            withContext(background.coroutineContext) {
                val utag = engine.get("utag", Utag::class.java)
                val result = utag.transform(
                    event = dispatch[Dispatch.Keys.TEALIUM_EVENT_TYPE] as? String ?: "link",
                    data = JSONObject(dispatch.payload()).toString(),
                    scope = scope
                )

                merge(result, dispatch)
            }
        }
    }

    private fun merge(jsonString: String, dispatch: Dispatch) {
        val json = JSONObject(jsonString)

        dispatch.addAll(
            JsonUtils.mapFor(json)
        )
    }

    companion object {
        private val TAG = QuickJsTransformationsAdapter::class.java.simpleName
        const val UTAG_JS = "utag.js"
    }
}