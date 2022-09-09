package com.tealium.transformations.internal.impl

import com.tealium.core.JsonLoader
import com.tealium.core.Loader
import com.tealium.core.Logger
import com.tealium.core.Tealium
import com.tealium.core.TealiumContext
import com.tealium.core.messaging.InstanceShutdownListener
import com.tealium.dispatcher.Dispatch
import com.tealium.transformations.Transformation
import com.tealium.transformations.internal.Storage
import com.tealium.transformations.internal.TransformationsAdapter
import com.tealium.transformations.internal.Util
import com.tealium.transformations.rhino.internal.RhinoJsConsoleImpl
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.mozilla.javascript.Context
import org.mozilla.javascript.NativeJavaMap
import org.mozilla.javascript.RhinoException
import org.mozilla.javascript.ScriptableObject
import org.mozilla.javascript.annotations.JSConstructor
import org.mozilla.javascript.annotations.JSFunction
import java.lang.ref.WeakReference

class RhinoTransformationsAdapter(
    private val context: TealiumContext,
    private val loader: Loader = JsonLoader.getInstance(context.config.application)
) : TransformationsAdapter, InstanceShutdownListener {

    private lateinit var rhino: Context
    private lateinit var scope: ScriptableObject
    private lateinit var utag: ScriptableObject
    private val background = context.executors.background

    override fun init(): Deferred<Unit> {
        return background.async {
            try {
                rhino = Context.enter()
                // required by Android
                rhino.optimizationLevel = -1

                // setup standard prototypes etc
                scope = rhino.initStandardObjects()

                val js = loader.loadFromAsset(UTAG_JS)
                // evaluate Utag code:
                js?.let {
                    rhino.evaluateString(scope, it, UTAG_JS, 1, null)
                    utag = scope.get("utag") as ScriptableObject
                }

                ScriptableObject.defineClass(scope, RhinoJsConsoleImpl::class.java)
                val console = rhino.newObject(scope, "Console")
                scope.defineProperty("console", console, ScriptableObject.CONST)

                ScriptableObject.defineClass(scope, UtilProxy::class.java)
                val utils = rhino.newObject(scope, "Utils")
                scope.defineProperty("util", utils, ScriptableObject.CONST)

                ScriptableObject.defineClass(scope, RhinoStorageImpl::class.java)
                val storage = rhino.newObject(scope, "Storage", arrayOf<Any>(StorageImpl(context.dataLayer)))
                scope.defineProperty("storage", storage, ScriptableObject.CONST)
                rhino.wrapFactory

            } catch (ex: RhinoException) {
                Logger.qa(TAG, "Error initializing Rhino: ${ex.message}")
            }
        }
    }

    override fun onInstanceShutdown(name: String, instance: WeakReference<Tealium>) {
        Context.exit()
    }

    override fun executeJavascript(js: String) {
        background.launch {
            try {
                val result = rhino.evaluateString(scope, js, null, 1, null)
                Logger.dev(TAG, "$result")
            } catch (ex: RhinoException) {
                Logger.dev(TAG, "error executing JS: ${ex.message}")
            }
        }
    }

    override suspend fun transform(dispatch: Dispatch) {
        withContext(background.coroutineContext) {
            try {
                val payload = Context.javaToJS(dispatch.payload(), scope)
                val result = ScriptableObject.callMethod(rhino, utag, "transformJson", arrayOf<Any>(
                    "link",
                    payload,
                    "alr"
                ))
//                Logger.dev(TAG, Context.toString(result))
                dispatch.addAll((result as NativeJavaMap).unwrap() as Map<String, Any>)
            } catch (ex: RhinoException) {
                Logger.dev(TAG, "error executing JS: ${ex.message}")
            }
        }
    }

    override fun getTransformation(scope: String): Transformation {
        return Transformation {  }
    }

    companion object {
        val TAG = RhinoTransformationsAdapter::class.java.simpleName
        const val UTAG_JS = "utag.js"
    }
}
//
//class RhinoConsoleImpl
//    @JSConstructor constructor(): JsConsole {
//
//    @JSFunction
//    override fun log(msg: String?) {
//
//    }
//
//    @JSFunction
//    override fun info(msg: String?) {
//        TODO("Not yet implemented")
//    }
//
//    @JSFunction
//    override fun error(msg: String?) {
//        Logger.prod(RhinoTransformationsAdapter.TAG, )
//    }
//}

class UtilProxy @JSConstructor constructor() : ScriptableObject(), Util {
    override fun getClassName(): String {
        return "Utils"
    }

    @JSFunction
    override fun hash(algorithm: String, input: String): String = UtilImpl.hash(algorithm, input)
}

class RhinoStorageImpl(): ScriptableObject(), Storage {

    private var delegate: Storage? = null

    @JSConstructor
    constructor(storage: Any) : this() {
        // Rhino uses reflection to check parameter types... it doesn't like unknown types
        // So specify Object and cast instead.

        if (storage is Storage)
            delegate = storage
    }


    override fun getClassName(): String {
        return "Storage"
    }

    @JSFunction
    override fun save(key: String, data: Any, expiry: String?) {
        delegate?.save(key, data, expiry) ?: Unit
    }

    @JSFunction
    override fun read(key: String): Any? {
        return delegate?.read(key)
    }
}