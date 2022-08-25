package com.tealium.transformations

import app.cash.quickjs.QuickJs
import app.cash.quickjs.QuickJsException
import com.tealium.core.JsonLoader
import com.tealium.core.JsonUtils
import com.tealium.core.Logger
import com.tealium.core.Module
import com.tealium.core.ModuleFactory
import com.tealium.core.Modules
import com.tealium.core.Tealium
import com.tealium.core.TealiumContext
import com.tealium.core.Transformer
import com.tealium.core.messaging.InstanceShutdownListener
import com.tealium.dispatcher.Dispatch
import com.tealium.transformations.internal.JsConsole
import com.tealium.transformations.internal.Storage
import com.tealium.transformations.internal.Utag
import com.tealium.transformations.internal.Util
import com.tealium.transformations.internal.impl.JsConsoleImpl
import com.tealium.transformations.internal.impl.StorageImpl
import com.tealium.transformations.internal.impl.UtilImpl
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.lang.ref.WeakReference

interface TransformationModule : Module {

    fun executeJavascript(js: String)
    fun executeJavascript(js: String, file: String)

    fun getTransforms(scope: String): Transformation

    companion object Factory : ModuleFactory {
        const val MODULE_NAME = "Transformations"

        override fun create(context: TealiumContext): TransformationModule {
            return TransformationModuleImpl(context)
        }
    }
}

// TODO - Ensure access to `QuickJS` is thread restricted.
private class TransformationModuleImpl(private val context: TealiumContext) :
    TransformationModule,
    InstanceShutdownListener,
    Transformer {
    override val name: String = TransformationModule.MODULE_NAME
    override var enabled: Boolean = true

    private lateinit var engine: QuickJs
    private val background = context.executors.background

    init {
        // TODO - make Tealium dispatchers available somehow
        // TODO - move this off Main thread
        // TODO - ensure connectivity etc
        background.launch {
            try {
                engine = QuickJs.create()
                engine.set("console", JsConsole::class.java, JsConsoleImpl())
                engine.set("util", Util::class.java, UtilImpl)
                engine.set("storage", Storage::class.java, StorageImpl(context.dataLayer))

                val loader = JsonLoader.getInstance(context.config.application)
                val js = loader.loadFromAsset(UTAG_JS)
//                val js = loader.loadFromAsset("utag.full.js")
//                val js = context.httpClient.get("https://tags.tiqcdn.com/utag/services-james/lib-mobile/dev/utag.js")
                js?.let {
                    engine.evaluate(it, UTAG_JS)
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
        try {
            val result = engine.evaluate(js)
            Logger.dev(TAG, "$result")
        } catch (ex: QuickJsException) {
        }
    }

    override fun executeJavascript(js: String, file: String) {
        try {
            val result = engine.evaluate(js, file)
            Logger.dev(TAG, "$result")
        } catch (ex: QuickJsException) {
        }
    }

    override suspend fun transform(dispatch: Dispatch) {
        withContext(background.coroutineContext) {
            try {
                val utag = engine.get("utag", Utag::class.java)

                val str = utag.transform("link", JSONObject(dispatch.payload()).toString(), "alr")
                Logger.dev(TAG, "from interface - $str")
                merge(str, dispatch)
            } catch (ex: QuickJsException) {
                Logger.qa(TAG, "QuickJS Evaluation issue: ${ex.message}")
            }
        }
    }

    override fun getTransforms(scope: String): Transformation {
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
        private val TAG = TransformationModule::class.java.simpleName
        const val UTAG_JS = "utag.js"
    }
}

fun interface Transformation {
    suspend fun transform(dispatch: Dispatch)
}

val Tealium.transformations: TransformationModule?
    get() = modules.getModule(TransformationModule::class.java)

val Modules.Transformations: ModuleFactory
    get() = TransformationModule

