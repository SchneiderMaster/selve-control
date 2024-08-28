package com.schneidermaster.selvecontrol.tools.shutters

import android.content.Context
import android.util.Log
import com.google.gson.JsonParser
import com.schneidermaster.selvecontrol.tools.httprequests.get
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import okhttp3.OkHttpClient

@OptIn(DelicateCoroutinesApi::class)
fun getShutters(client: OkHttpClient, context: Context, serverIp: String, serverPassword: String): Deferred<List<Shutter>> {
    return GlobalScope.async {
        val shutters: List<Shutter>

        if(context.fileList().contains("shutterData.json")){
            var json = ""
            context.openFileInput("shutterData.json").bufferedReader().useLines {lines ->
                lines.forEach {
                    json += it
                }
            }
            println(context.filesDir.absolutePath)
            println(json)

            shutters = JsonParser.parseString(json).asJsonArray.map { it ->
                val obj = it.asJsonObject
                Shutter(
                    sid = obj.get("sid").asString,
                    adr = obj.get("adr").asString,
                    position = obj.get("position").asInt,
                    name = obj.get("name")?.takeIf { !it.isJsonNull }?.asString
                )
            }

            println(shutters)

            return@async shutters
        }

        Log.println(Log.WARN, "SelveControl", "shutterData.json not found\nFetching Shutter Data from Server...")

        val deferredStatesResponse =
            get(client, "http://$serverIp/cmd?XC_FNC=GetStates&auth=$serverPassword")

        val statesResponse = deferredStatesResponse.await()
        val statesJsonResponse = statesResponse.body?.string()

        println(statesJsonResponse)

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

        shutters.forEach{
            it.name = getName(client, it, serverIp, serverPassword).await()
        }

        return@async shutters
    }
}