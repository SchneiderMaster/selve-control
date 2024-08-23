package com.schneidermaster.selvecontrol.tools.shutters

import android.content.Context
import com.google.gson.JsonParser
import com.schneidermaster.selvecontrol.tools.httprequests.get
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import okhttp3.OkHttpClient

@OptIn(DelicateCoroutinesApi::class)
fun getShutters(client: OkHttpClient, context: Context): Deferred<List<Shutter>> {
    return GlobalScope.async {
        val shutters: List<Shutter>

        var json = ""
        context.openFileInput("shutterData.json").bufferedReader().useLines {lines ->
            lines.forEach {
                json += it
            }
        }

        shutters = JsonParser.parseString(json).asJsonObject.getAsJsonArray().map {
            val obj = it.asJsonObject
            Shutter(
                sid = obj.get("sid").asString,
                adr = obj.get("adr").asString,
                position = obj.get("position").asInt,
                name = obj.get("name").asString
            )
        }





        val deferredStatesResponse =
            get(client, "http://192.168.188.143/cmd?XC_FNC=GetStates&auth=Auablume")

        val statesResponse = deferredStatesResponse.await()
        val statesJsonResponse = statesResponse.body?.string()

        if (statesJsonResponse != null) {
            val jsonObject = JsonParser.parseString(statesJsonResponse).asJsonObject
            val shuttersArray = jsonObject.getAsJsonArray("XC_SUC")

            shutters = shuttersArray
                .filter { it.asJsonObject.get("type")?.asString == "CM" }
                .map {
                    val obj = it.asJsonObject
                    Shutter(
                        sid = obj.get("sid").asString,
                        adr = obj.get("adr").asString,
                        position = obj.get("state").asJsonObject.get("position").asInt,
                        name = null
                    )
                }
        } else {
            error("Didn't receive shutter data.")
        }
        println(shutters)

        return@async shutters
    }
}