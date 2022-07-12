package com.tealium.media.v2.messaging

import com.tealium.core.messaging.Messenger
import com.tealium.media.segments.Ad
import com.tealium.media.segments.AdBreak
import com.tealium.media.segments.Chapter
import com.tealium.media.v2.messaging.MediaListener
import kotlin.reflect.KClass

abstract class MediaMessenger<T : MediaListener>(listenerClass: KClass<T>) :
    Messenger<T>(listenerClass)

class OnStartSessionMessenger :
    MediaMessenger<OnStartSessionListener>(OnStartSessionListener::class) {
    override fun deliver(listener: OnStartSessionListener) {
        listener.onStartSession()
    }
}

class OnResumeSessionMessenger :
    MediaMessenger<OnResumeSessionListener>(OnResumeSessionListener::class) {
    override fun deliver(listener: OnResumeSessionListener) {
        listener.onResumeSession()
    }
}

class OnPlayMessenger : MediaMessenger<OnPlayListener>(OnPlayListener::class) {
    override fun deliver(listener: OnPlayListener) {
        listener.onPlay()
    }
}

class OnPausedMessenger : MediaMessenger<OnPausedListener>(OnPausedListener::class) {
    override fun deliver(listener: OnPausedListener) {
        listener.onPaused()
    }
}

class OnStartChapterMessenger(
    private val chapter: Chapter
) : MediaMessenger<OnStartChapterListener>(OnStartChapterListener::class) {
    override fun deliver(listener: OnStartChapterListener) {
        listener.onStartChapter(chapter)
    }
}

class OnSkipChapterMessenger : MediaMessenger<OnSkipChapterListener>(OnSkipChapterListener::class) {
    override fun deliver(listener: OnSkipChapterListener) {
        listener.onSkipChapter()
    }
}

class OnEndChapterMessenger : MediaMessenger<OnEndChapterListener>(OnEndChapterListener::class) {
    override fun deliver(listener: OnEndChapterListener) {
        listener.onEndChapter()
    }
}

class OnStartBufferMessenger : MediaMessenger<OnStartBufferListener>(OnStartBufferListener::class) {
    override fun deliver(listener: OnStartBufferListener) {
        listener.onStartBuffer()
    }
}

class OnEndBufferMessenger : MediaMessenger<OnEndBufferListener>(OnEndBufferListener::class) {
    override fun deliver(listener: OnEndBufferListener) {
        listener.onEndBuffer()
    }
}

class OnStartSeekMessenger(
    private val position: Int?
) : MediaMessenger<OnStartSeekListener>(OnStartSeekListener::class) {
    override fun deliver(listener: OnStartSeekListener) {
        listener.onStartSeek(position)
    }
}


class OnEndSeekMessenger(
    private val position: Int?
) : MediaMessenger<OnEndSeekListener>(OnEndSeekListener::class) {
    override fun deliver(listener: OnEndSeekListener) {
        listener.onEndSeek(position)
    }
}

class OnStartAdBreakMessenger(
    private val adBreak: AdBreak
) : MediaMessenger<OnStartAdBreakListener>(OnStartAdBreakListener::class) {
    override fun deliver(listener: OnStartAdBreakListener) {
        listener.onStartAdBreak(adBreak)
    }
}

class OnEndAdBreakMessenger(
//    private val adBreak: AdBreak
) : MediaMessenger<OnEndAdBreakListener>(OnEndAdBreakListener::class) {
    override fun deliver(listener: OnEndAdBreakListener) {
        listener.onEndAdBreak()
    }
}

class OnStartAdMessenger(
    private val ad: Ad
) : MediaMessenger<OnStartAdListener>(OnStartAdListener::class) {
    override fun deliver(listener: OnStartAdListener) {
        listener.onStartAd(ad)
    }
}

class OnClickAdMessenger(
//    private val ad: Ad
) : MediaMessenger<OnClickAdListener>(OnClickAdListener::class) {
    override fun deliver(listener: OnClickAdListener) {
        listener.onClickAd() // should have Ad?
    }
}

class OnSkipAdMessenger(
//    private val ad: Ad
) : MediaMessenger<OnSkipAdListener>(OnSkipAdListener::class) {
    override fun deliver(listener: OnSkipAdListener) {
        listener.onSkipAd() // should have Ad?
    }
}

class OnEndAdMessenger(
//    private val ad: Ad
) : MediaMessenger<OnEndAdListener>(OnEndAdListener::class) {
    override fun deliver(listener: OnEndAdListener) {
        listener.onEndAd() // should have Ad?
    }
}

class OnEndContentMessenger : MediaMessenger<OnEndContentListener>(OnEndContentListener::class) {
    override fun deliver(listener: OnEndContentListener) {
        listener.onEndContent()
    }
}

class OnEndSessionMessenger : MediaMessenger<OnEndSessionListener>(OnEndSessionListener::class) {
    override fun deliver(listener: OnEndSessionListener) {
        listener.onEndSession()
    }
}

class OnCustomEventMessenger(
    private val event: String?
) : MediaMessenger<OnCustomEventListener>(OnCustomEventListener::class) {
    override fun deliver(listener: OnCustomEventListener) {
        listener.onCustomEvent()
    }
}