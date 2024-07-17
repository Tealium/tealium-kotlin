package com.tealium.momentsapi

sealed class MomentsApiRegion(val value: String) {
    object Germany : MomentsApiRegion("eu-central-1")
    object UsEast : MomentsApiRegion("us-east-1")
    object Sydney : MomentsApiRegion("ap-southeast-2")
    object Oregon : MomentsApiRegion("us-west-2")
    object Tokyo : MomentsApiRegion("ap-northeast-1")
    object HongKong : MomentsApiRegion("ap-east-1")
    data class Custom(private val region: String) : MomentsApiRegion(region)
}