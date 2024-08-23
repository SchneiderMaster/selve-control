/* While this template provides a good starting point for using Wear Compose, you can always
 * take a look at https://github.com/android/wear-os-samples/tree/main/ComposeStarter and
 * https://github.com/android/wear-os-samples/tree/main/ComposeAdvanced to find the most up to date
 * changes to the libraries and their usages.
 */

@file:OptIn(DelicateCoroutinesApi::class)

package com.schneidermaster.selvecontrol.presentation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import com.google.gson.JsonParser
import com.schneidermaster.selvecontrol.presentation.theme.SelveControlTheme
import com.schneidermaster.selvecontrol.tools.httprequests.get
import com.schneidermaster.selvecontrol.tools.httprequests.post
import com.schneidermaster.selvecontrol.tools.shutters.Shutter
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()

        super.onCreate(savedInstanceState)

        setTheme(android.R.style.Theme_DeviceDefault)

        setContent {
            WearApp()
        }
    }
}

@Composable
fun WearApp() {

    val client = OkHttpClient()

    getShutters(client)

    SelveControlTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colors.background),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
            ) {
                ScalingLazyColumn {
                    item {
                        Button(
                            onClick = {
                                println("I have been pressed; " + "up")

                                GlobalScope.launch {
                                    val response = post(
                                        client,
                                        "http://192.168.188.143/cmd?auth=Auablume",
                                        "{\"XC_FNC\":\"SendGenericCmd\",\"id\":\"0B\",\"data\":{\"cmd\": \"moveUp\"}}"
                                    )
                                    println(response.await().body?.string())
                                }
                            })
                        {
                            Text(text = "Up")
                        }
                    }

                    item {
                        Button(
                            onClick = {

                                println("I have been pressed; " + "stop")
                                GlobalScope.launch {
                                    val response = post(
                                        client,
                                        "http://192.168.188.143/cmd?auth=Auablume",
                                        "{\"XC_FNC\":\"SendGenericCmd\",\"id\":\"0B\",\"data\":{\"cmd\": \"stop\"}}"
                                    )
                                    println(response.await().body?.string())
                                }
                            })
                        {
                            Text(text = "Stop")
                        }
                    }

                    item {
                        Button(
                            onClick = {

                                println("I have been pressed; " + "down")
                                GlobalScope.launch {
                                    val response = post(
                                        client,
                                        "http://192.168.188.143/cmd?auth=Auablume",
                                        "{\"XC_FNC\":\"SendGenericCmd\",\"id\":\"0B\",\"data\":{\"cmd\": \"moveDown\"}}"
                                    )
                                    println(response.await().body?.string())
                                }
                            })
                        {
                            Text(text = "Down")
                        }
                    }
                }
            }
        }
    }
}

fun getShutters(client: OkHttpClient){
    GlobalScope.launch {

        val shutters: List<Shutter>

        val deferredStatesResponse = get(client, "http://192.168.188.143/cmd?XC_FNC=GetStates&auth=Auablume")

        val statesResponse = deferredStatesResponse.await()
        val statesJsonResponse = statesResponse.body?.string()

        if(statesJsonResponse != null){
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
        }
        else{
            error("Didn't receive shutter data.")
        }

        shutters.forEach {
            GlobalScope.launch {
                val deferredConfigResponse = get(
                    client,
                    "http://192.168.188.143/cmd?XC_FNC=GetConfig&type=CM&adr=" + it.adr + "&auth=Auablume"
                )
                val configResponse = deferredConfigResponse.await()
                val configJsonResponse = configResponse.body?.string()

                if (configJsonResponse != null) {
                    val jsonObject = JsonParser.parseString(configJsonResponse).asJsonObject
                    it.name =
                        jsonObject.get("XC_SUC").asJsonObject.get("info").asJsonObject.get("configurable").asJsonObject.get(
                            "name"
                        ).asString
                }
            }
        }


        println(shutters)

    }
}