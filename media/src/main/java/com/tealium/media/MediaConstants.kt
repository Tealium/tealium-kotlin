package com.tealium.media

object MediaEvent {
    const val ADBREAK_COMPLETE = "media_adbreak_complete"
    const val ADBREAK_START = "media_adbreak_start"
    const val AD_CLICK = "media_ad_click"
    const val AD_COMPLETE = "media_ad_complete"
    const val AD_SKIP = "media_ad_skip"
    const val AD_START = "media_ad_start"

    const val BITRATE_CHANGE = "media_bitrate_change"

    const val BUFFER_COMPLETE = "media_buffer_complete"
    const val BUFFER_START = "media_buffer_start"

    const val CHAPTER_COMPLETE = "media_chapter_complete"
    const val CHAPTER_SKIP = "media_chapter_skip"
    const val CHAPTER_START = "media_chapter_start"

    const val INTERVAL = "media_interaval"
    const val MILESTONE = "media_milestone"

    const val PAUSE = "media_pause"
    const val PLAY = "media_play"

    const val PLAYER_STATE_START = "media_player_state_start"
    const val PLAYER_STATE_STOP = "media_player_state_stop"

    const val SEEK_START = "media_seek_start"
    const val SEEK_COMPLETE = "media_seek_complete"

    const val SESSION_START = "media_session_start"
    const val SESSION_RESUME = "media_session_resume"
    const val SESSION_END = "media_session_end"
    const val CONTENT_END = "media_content_end"
}

object SegmentKey {
    const val METADATA = "media_segment_metadata"
}

object ChapterKey {
    const val UUID = "media_chapter_uuid"
    const val NAME = "media_chapter_name"
    const val DURATION = "media_chapter_duration"
    const val POSITION = "media_chapter_position"
    const val START_TIME = "media_chapter_start_time"
    const val SKIPPED = "media_chapter_skipped"
}

object AdKey {
    const val UUID = "media_ad_uuid"
    const val NAME = "media_ad_name"
    const val ID = "media_ad_id"
    const val DURATION = "media_ad_duration"
    const val POSITION = "media_ad_position"
    const val ADVERTISER = "media_ad_advertiser"
    const val CREATIVE_ID = "media_ad_creative_id"
    const val CAMPAIGN_ID = "media_ad_campaign_id"
    const val PLACEMENT_ID = "media_ad_placement_id"
    const val SITE_ID = "media_ad_site_id"
    const val CREATIVE_URL = "media_ad_creative_url"
    const val NUMBER_OF_LOADS = "media_ad_load"
    const val POD = "media_ad_pod"
    const val PLAYER_NAME = "media_ad_player_name"
    const val SKIPPED = "media_ad_skipped"
}

object AdBreakKey {
    const val UUID = "media_ad_break_uuid"
    const val NAME = "media_ad_break_name"
    const val ID = "media_ad_break_id"
    const val DURATION = "media_ad_break_duration"
    const val INDEX = "media_ad_break_index"
    const val POSITION = "media_ad_break_position"
}

object SummaryKey {
    const val SESSION_START_TIME = "media_session_start_time"
    const val PLAYS = "media_total_plays"
    const val PAUSES = "media_total_pauses"
    const val AD_SKIPS = "media_total_ad_skips"
    const val CHAPTER_SKIPS = "media_total_chapter_skips"
    const val STOPS = "media_total_stops"
    const val ADS = "media_total_ads"
    const val AD_UUIDS = "media_ad_uuids"
    const val PLAY_TO_END = "media_played_to_end"
    const val DURATION = "media_session_duration"
    const val TOTAL_PLAY_TIME = "media_total_play_time"
    const val TOTAL_AD_TIME = "media_total_ad_time"
    const val PERCENTAGE_AD_TIME = "media_percentage_ad_time"
    const val PERCENTAGE_AD_COMPLETE = "media_percentage_ad_complete"
    const val PERCENTAGE_CHAPTER_COMPLETE = "media_percentage_chapter_complete"
    const val TOTAL_BUFFER_TIME = "media_total_buffer_time"
    const val TOTAL_SEEK_TIME = "media_total_seek_time"
    const val SESSION_END_TIME = "media_session_end_time"
}

object SessionKey {
    const val UUID = "media_uuid"
    const val NAME = "media_name"
    const val STREAM_TYPE = "media_stream_type"
    const val MEDIA_TYPE = "media_type"
    const val TRACKING_TYPE = "media_tracking_type"
    const val START_TIME = "media_session_start_time"
    const val STATE = "media_player_state"
    const val CUSTOM_ID = "media_custom_id"
    const val DURATION = "media_duration"
    const val PLAYER_NAME = "media_player_name"
    const val CHANNEL_NAME = "media_channel_name"
    const val MILESTONE = "media_milestone"
    const val PERCENT_COMPLETE = "media_percent_complete"
    const val RESUMED = "media_resumed"
    const val SUMMARY = "media_summary"
}

object QoEKey {
    const val BITRATE = "media_qoe_bitrate"
    const val START_TIME = "media_qoe_startup_time"
    const val FPS = "media_qoe_frames_per_second"
    const val DROPPED_FRAMES = "media_qoe_dropped_frames"
    const val PLAYBACK_SPEED = "media_qoe_playback_speed"
    const val METADATA = "media_qoe_metadata"
}

enum class StreamType(val value: String) {
    AOD("aod"),
    AUDIOBOOK("audiobook"),
    CUSTOM("custom"),
    DVOD("dvod"),
    LIVE("live"),
    LINEAR("linear"),
    PODCAST("podcast"),
    RADIO("radio"),
    SONG("song"),
    UGC("ugc"),
    VOD("vod"),
}

enum class MediaType(val value: String) {
    ALL("all"),
    AUDIO("audio"),
    VIDEO("video"),
}

enum class TrackingType(val value: String) {
    INTERVAL("interval"),
    MILESTONE("milestone"),
    FULL_PLAYBACK("full_playback"),
    INTERVAL_MILESTONE("interval_milestone"),
    SUMMARY("summary"),

}

enum class Milestone(val value: String) {
    TEN("10%"),
    TWENTY_FIVE("25%"),
    FIFTY("50%"),
    SEVENTY_FIVE("75%"),
    NINETY("90%"),
    ONE_HUNDRED("100%"),
}

enum class PlayerState(val value: String) {
    CLOSED_CAPTION("closed_caption"),
    FULLSCREEN("fullscreen"),
    IN_FOCUS("in_focus"),
    MUTE("mute"),
    PICTURE_IN_PICTURE("picture_in_picture"),
}