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
    const val HEARTBEAT = "media_heartbeat"
    const val MILESTONE = "media_milestone"
    const val PAUSE = "media_pause"
    const val PLAY = "media_play"
    const val PLAYER_STATE_START = "player_state_start"
    const val PLAYER_STATE_STOP = "player_state_stop"
    const val SEEK_START = "media_seek_start"
    const val SEEK_COMPLETE = "media_seek_complete"
    const val SESSION_END = "media_session_end"
    const val SESSION_START = "media_session_start"
    const val STOP = "media_stop"
    const val SUMMARY = "media_summary"
}

object ChapterKey {
    const val NAME = "chapter_name"
    const val DURATION = "chapter_length"
    const val POSITION = "chapter_position"
    const val START_TIME = "chapter_start_time"
    const val METADATA = "chapter_metadata"
}

object AdKeys {
    const val UUID = "ad_uuid"
    const val NAME = "ad_name"
    const val ID = "ad_id"
    const val DURATION = "ad_length"
    const val POSITION = "ad_position"
    const val ADVERTISER = "advertiser"
    const val CREATIVE_ID = "ad_creative_id"
    const val CAMPAIGN_ID = "ad_campaign_id"
    const val PLACEMENT_ID = "ad_placement_id"
    const val SITE_ID = "ad_site_id"
    const val CREATIVE_URL = "ad_creative_url"
    const val NUMBER_OF_LOADS = "ad_load"
    const val POD = "ad_pod"
    const val PLAYER_NAME = "ad_player_name"
}

object AdBreakKeys {
    const val UUID = "ad_break_uuid"
    const val TITLE = "ad_break_title"
    const val ID = "ad_break_id"
    const val DURATION = "ad_break_length"
    const val INDEX = "ad_break_index"
    const val POSITION = "ad_break_position"
}