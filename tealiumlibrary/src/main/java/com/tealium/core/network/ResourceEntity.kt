package com.tealium.core.network

data class ResourceEntity(
    val response: String? = null,
    val etag: String? = null,
    val headers: Map<String, List<String>>? = null // keep all headers?
)
