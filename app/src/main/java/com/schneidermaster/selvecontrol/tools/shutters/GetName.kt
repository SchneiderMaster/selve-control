package com.schneidermaster.selvecontrol.tools.shutters

import com.google.gson.JsonParser
import com.schneidermaster.selvecontrol.tools.httprequests.get
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import okhttp3.OkHttpClient

fun getName(client: OkHttpClient, shutter: Shutter, serverIp: String, serverPassword: String): Deferred<String> {
    return GlobalScope.async {
        val deferredConfigResponse = get(
            client,
            "http://$serverIp/cmd?XC_FNC=GetConfig&type=CM&adr=" + shutter.adr + "&auth=$serverPassword"
        )
        val configResponse = deferredConfigResponse.await()
        val configJsonResponse = configResponse.body?.string()

        if (configJsonResponse != null) {
            val jsonObject = JsonParser.parseString(configJsonResponse).asJsonObject
            println("got the name")
            return@async jsonObject.get("XC_SUC").asJsonObject.get("info").asJsonObject.get("configurable").asJsonObject.get(
                "name"
            ).asString
        }
        else{
            error("No Response received.")
        }
    }
}