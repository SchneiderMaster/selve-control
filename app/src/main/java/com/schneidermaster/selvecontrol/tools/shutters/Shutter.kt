package com.schneidermaster.selvecontrol.tools.shutters

import kotlinx.serialization.Serializable

@Serializable
data class Shutter (
    val name: String?,
    val adr: String?,
    val sid: String?,
    val position: Int?
)

@Serializable
data class JsonResponse(
    val XC_SUC: List<JsonData>
)

@Serializable
sealed class JsonData {
    @Serializable
    data class Cm(
        val sid: String?,
        val adr: String?,
        val cid: String?,
        val state: State?
    ) : JsonData()

    @Serializable
    data class State(
        val position: Int?
    )

    @Serializable
    data class Other(val type: String) : JsonData()
}