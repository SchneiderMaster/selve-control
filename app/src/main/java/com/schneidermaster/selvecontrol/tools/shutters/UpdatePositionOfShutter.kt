package com.schneidermaster.selvecontrol.tools.shutters

import com.google.gson.JsonParser
import com.schneidermaster.selvecontrol.tools.httprequests.get
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient

suspend fun updatePositionOfShutter(
    client: OkHttpClient,
    currentShutter: Shutter?,
    onUpdateShutter: (Shutter?) -> Unit
) {
    var currentShutter1 = currentShutter
    do {
        withContext(Dispatchers.IO) {
            Thread.sleep(1000)
        }
        val response = get(
            client,
            "http://192.168.188.143/cmd?XC_FNC=GetStates&auth=Auablume"
        ).await()
        val shutter =
            JsonParser.parseString(
                response.body?.string()
            ).asJsonObject.get(
                "XC_SUC"
            ).asJsonArray.first {
                it.asJsonObject.get(
                    "type"
                ).asString == "CM" && it.asJsonObject.get(
                    "sid"
                ).asString == currentShutter1?.sid
            }

        currentShutter1 = currentShutter1?.copy(
            position = shutter.asJsonObject.get("state").asJsonObject.get("position").asInt
        )
        onUpdateShutter(currentShutter1)
        println(currentShutter1?.position)
    } while (shutter.asJsonObject.get("state").asJsonObject.get(
            "run_state"
        ).asInt != 0
    )
}
