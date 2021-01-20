package com.tealium.media.segments

import com.tealium.media.AdKeys
import java.util.*

class Ad(var id: String,
         var name: String? = null,
         var position: Int? = null,
         var advertiser: String? = null,
         var creativeId: String? = null,
         var campaignId: String? = null,
         var placementId: String? = null,
         var siteId: String? = null,
         var creativeUrl: String? = null,
         var numberOfLoads: Int? = null,
         var pod: String? = null,
         var playerName: String? = null,
         var startTime: Long? = System.currentTimeMillis(),
         var duration: Long? = null,
         var uuid: String = UUID.randomUUID().toString()) : Segment {

    override fun segmentInfo(): Map<String, Any> {
        val data = mutableMapOf<String, Any>()
        data[AdKeys.ID] = id
        name?.let { data[AdKeys.NAME] = it }
        position?.let { data[AdKeys.POSITION] = it }
        advertiser?.let { data[AdKeys.ADVERTISER] = it }
        creativeId?.let { data[AdKeys.CREATIVE_ID] = it }
        campaignId?.let { data[AdKeys.CAMPAIGN_ID] = it }
        placementId?.let { data[AdKeys.PLACEMENT_ID] = it }
        siteId?.let { data[AdKeys.SITE_ID] = it }
        creativeUrl?.let { data[AdKeys.CREATIVE_URL] = it }
        numberOfLoads?.let { data[AdKeys.NUMBER_OF_LOADS] = it }
        pod?.let { data[AdKeys.POD] = it }
        playerName?.let { data[AdKeys.PLAYER_NAME] = it }
//        startTime?.let { data[AdKeys.START_TIME] = it }
        duration?.let { data[AdKeys.DURATION] = it }
        uuid?.let { data[AdKeys.UUID] = it }
//        numberOfAds?.let { data[AdKeys.NUMBER_OF_ADS] = it }

        return data
    }
}