
final class Crash (Thread thread, Throwable ex) {
    const val KEY_STACK_CLASS_NAME = "className"
    const val KEY_STACK_FILENAME = "fileName"
    const val KEY_STACK_LINE_NUMBER = "lineNumber"
    const val KEY_STACK_METHOD_NAME = "methodName"
    const val KEY_THREAD_STATE = "state"
    const val KEY_THREAD_NUMBER = "threadNumber"
    const val KEY_THREAD_ID = "threadId"
    const val KEY_THREAD_PRIORITY = "priority"
    const val KEY_THREAD_STACK = "stack"

    private val mException: Throwable
    private val mThread: java.lang.Thread

    private val mExceptionCause: String
    private val mExceptionName: String
    private var mUuid: String
    private var mThreadState: String
    private var mThreadNumber: String
    private var mThreadPriority: String

    init {
        mException = ex
        mThread = thread
    }


    fun getCause(): String {
        if (mExceptionCause != null) {
            return mExceptionCause
        }

        return mExceptionCause = mException::class.java.simpleName
//        return mException = mException.javaClass.getName().also({ mExceptionCause = it })
    }

    fun getExceptionName(): String {
        if (mExceptionName != null) {
            return mExceptionName
        }
        return mExceptionName = mException.message
    }

    fun getUuid(): String {
        if (mUuid != null) {
            return mUuid
        }
        return mUuid = generateUuid()
    }

    fun getThreadState(): String {
        if (mThreadState != null) {
           return mThreadState
        }
        return mThreadState = mThread.getState().toString()
    }

    fun getThreadNumber(): String {
        if (mThreadNumber != null) {
            return mThreadNumber
        }
        return mThreadNumber = mThread.getId().toString()
    }

    fun getThreadId(): String {
        if (mThreadId != null) {
            return mThreadId
        }
        return mThreadId = mThread.getName()
    }

    fun getThreadPriority(): String {
        if (mThreadPriority != null) {
            return mThreadPriority
        }
        return mThreadPriority = mThread.getPriority().toString()
    }

    private fun generateUuid(): String {
        return java.util.UUID.randomUUID().toString()
    }

    /**
     * Retrieve Thread data for given crash object
     * @param crash
     * @param truncateStackTrace
     * @return
     */
    fun getThreadData(crash: Crash, truncateStackTrace: Boolean): String {
        val array = JSONArray()
        val threadData = JSONObject()
        try {
            threadData.put("crashed", "true")
            threadData.put(KEY_THREAD_STATE, crash.getThreadState())
            threadData.put(KEY_THREAD_NUMBER, crash.getThreadNumber())
            threadData.put(KEY_THREAD_ID, crash.getThreadId())
            threadData.put(KEY_THREAD_PRIORITY, crash.getThreadPriority())
            threadData.put(KEY_THREAD_STACK, crash.getStackData(crash, truncateStackTrace))


        } catch (ex: JSONException) {
            android.util.Log.e(BuildConfig.TAG, ex.message)
        }
        array.put(threadData.toString())
        return array.toString()
    }

    /**
     * Return stack track element details
     * @param crash Crash Object
     * @param truncateStackTrace Boolean
     * @return populated JSONArray
     */
    fun getStackData(crash: Crash, truncateStackTrace: Boolean): org.json.JSONArray? {
        val array = JSONArray()
        val stackTraceElements = crash.mException.getStackTrace()

        for (i in stackTraceElements.indices) {
            val element = stackTraceElements[i]
            val stackTrace = JSONObject()
            try {
                if (element.getFileName() != null) {
                    stackTrace.put(KEY_STACK_FILENAME, element.getFileName())
                }
                stackTrace.put(KEY_STACK_CLASS_NAME, element.getClassName())
                stackTrace.put(KEY_STACK_METHOD_NAME, element.getMethodName())
                stackTrace.put(cKEY_STACK_LINE_NUMBER, element.getLineNumber().toString())

                array.put(stackTrace)

                if (truncateStackTrace) {
                    break
                }
            } catch (ex: JSONException) {
                android.util.Log.e(BuildConfig.TAG, ex.message)
            }
        }
        return array
    }

    fun create(thread: Thread, ex: Throwable): Crash {
        Crash crash = new Crash(thread, ex)
        return crash
    }

    internal object Test {
        val testThread = Thread("test_thread")
        val error = Throwable("This is a test error")
        fun create(): Crash {
            return Crash.create(testThread, error)
        }

        fun getStackTrace(crash: Crash, truncateStackTrace: Boolean): JSONArray {
            return Crash.getStackData(crash, truncateStackTrace)
        }

        fun getThreadData(crash: Crash, truncateStackTrace: Boolean): String {
            return Crash.getThreadData(crash, truncateStackTrace)
        }
    }
}