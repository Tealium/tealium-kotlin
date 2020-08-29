package com.tealium.crashreporter;

import android.util.Log
import com.tealium.tealiumlibrary.BuildConfig
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

data class Crash (val thread: Thread, val exception: Throwable, val exceptionCause: String = exception.javaClass.name,
                  val exceptionName: String = exception.message.toString(), val uUid: String = java.util.UUID.randomUUID().toString(),
                  val threadState: String = thread.state.toString(), val threadNumber: String = thread.id.toString(),
                  val threadId: String = thread.name.toString(), val threadPriority: String = thread.priority.toString()) {

    companion object {

        const val KEY_STACK_CLASS_NAME = "className"
        const val KEY_STACK_FILENAME = "fileName"
        const val KEY_STACK_LINE_NUMBER = "lineNumber"
        const val KEY_STACK_METHOD_NAME = "methodName"
        const val KEY_THREAD_STATE = "state"
        const val KEY_THREAD_NUMBER = "threadNumber"
        const val KEY_THREAD_ID = "threadId"
        const val KEY_THREAD_PRIORITY = "priority"
        const val KEY_THREAD_STACK = "stack"

        fun getThreadData (crash: Crash, truncateStackTrace: Boolean): String {
            var array = JSONArray()
            var threadData = JSONObject()

            try {
                threadData.put("crashed", "true")
                threadData.put(KEY_THREAD_STATE, crash.threadState)
                threadData.put(KEY_THREAD_NUMBER, crash.threadNumber)
                threadData.put(KEY_THREAD_ID, crash.threadId)
                threadData.put(KEY_THREAD_PRIORITY, crash.threadPriority)
                threadData.put(KEY_THREAD_STACK, Crash.getStackData(crash, truncateStackTrace))
            } catch (ex: JSONException) {
                Log.e(BuildConfig.TAG, ex.message)
            }

            array.put(threadData.toString())

            return array.toString()
        }


        fun getStackData(crash: Crash, truncateStackTrace: Boolean): JSONArray {
            val array = JSONArray()
            val stackTraceElements: Array<StackTraceElement> = crash.exception.stackTrace

            for (i in stackTraceElements.indices) {
                val element = stackTraceElements[i]
                val stackTrace = JSONObject()
                try {
                    if (element.fileName != null) {
                        stackTrace.put(KEY_STACK_FILENAME, element.fileName)
                    }
                    stackTrace.put(KEY_STACK_CLASS_NAME, element.className)
                    stackTrace.put(KEY_STACK_METHOD_NAME, element.methodName)
                    stackTrace.put(KEY_STACK_LINE_NUMBER, element.lineNumber.toString())
                    array.put(stackTrace)
                    if (truncateStackTrace) {
                        break
                    }
                } catch (ex: JSONException) {
                    Log.e(BuildConfig.TAG, ex.message)
                }
            }
            return array
        }


    }
}