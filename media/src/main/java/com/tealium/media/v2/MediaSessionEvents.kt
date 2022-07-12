package com.tealium.media.v2

import com.tealium.media.v2.messaging.MediaListener

interface MediaSessionEvents {
    fun subscribe(mediaListener: MediaListener)
    fun unsubscribe(mediaListener: MediaListener)
}

