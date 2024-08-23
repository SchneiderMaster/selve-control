/* While this template provides a good starting point for using Wear Compose, you can always
 * take a look at https://github.com/android/wear-os-samples/tree/main/ComposeStarter and
 * https://github.com/android/wear-os-samples/tree/main/ComposeAdvanced to find the most up to date
 * changes to the libraries and their usages.
 */

@file:OptIn(DelicateCoroutinesApi::class)

package com.schneidermaster.selvecontrol.presentation

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.changedToUp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.CircularProgressIndicator
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import com.google.gson.GsonBuilder
import com.google.gson.JsonParser
import com.schneidermaster.selvecontrol.presentation.theme.SelveControlTheme
import com.schneidermaster.selvecontrol.tools.httprequests.get
import com.schneidermaster.selvecontrol.tools.httprequests.push
import com.schneidermaster.selvecontrol.tools.shutters.Shutter
import com.schneidermaster.selvecontrol.tools.shutters.getName
import com.schneidermaster.selvecontrol.tools.shutters.getShutters
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import kotlin.concurrent.thread

class MainActivity : ComponentActivity() {

    private var shutters: List<Shutter>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()

        super.onCreate(savedInstanceState)

        println(fileList().asList())

        setTheme(android.R.style.Theme_DeviceDefault)

        setContent {
            WearApp()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if(!isChangingConfigurations){
            val gson = GsonBuilder()
                .serializeNulls()
                .create()
            openFileOutput("shutterData.json", Context.MODE_PRIVATE).use {
                it.write(gson.toJson(shutters).toByteArray())
                println(fileList().asList())
            }
        }
    }

    @Composable
    fun WearApp(debugging: Boolean = false) {

        val client = OkHttpClient()

        val scope = rememberCoroutineScope()

        var isLoading by remember { mutableStateOf(true) }
        var currentShutter by remember { mutableStateOf<Shutter?>(null) }
        var shutters by remember { mutableStateOf<List<Shutter>?>(null) }
        var shutterIndex by remember { mutableIntStateOf(0) }
        var tapPosition by remember { mutableIntStateOf(0) }
        var height by remember { mutableIntStateOf(0) }

        LaunchedEffect(Unit) {
            scope.launch {
                shutters = getShutters(client, this@MainActivity).await()
                val firstShutter = shutters!![0]
                if(firstShutter.name == null) {
                    firstShutter.name = getName(client, firstShutter).await()
                }
                currentShutter = firstShutter
                updateShutters(shutters!!)
                isLoading = false
            }
        }

        SelveControlTheme (
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colors.background),
                contentAlignment = Alignment.Center
            )
            {
                if(isLoading && !debugging){
                    Column (
                        horizontalAlignment = Alignment.CenterHorizontally
                    ){
                        CircularProgressIndicator(
                            modifier = Modifier
                                .padding(bottom = Dp(10f))
                        )
                        Text(
                            text = "Receiving data from shutters...",
                            textAlign = TextAlign.Center
                        )
                    }

                }
                else {
                    Box(
                        contentAlignment = Alignment.TopCenter
                    ) {
                        Row(
                            verticalAlignment = Alignment.Bottom
                        ) {
                            Button(
                                onClick = {
                                    if(shutterIndex > 0){
                                        shutterIndex--
                                    }else{
                                        shutterIndex = shutters!!.size-1
                                    }

                                    val previousShutter = shutters!![shutterIndex]

                                    if(previousShutter.name == null){
                                        scope.launch {
                                            previousShutter.name = getName(client, previousShutter).await()
                                            updateShutters(shutters!!)
                                            currentShutter = previousShutter
                                        }
                                    }
                                    else {
                                        currentShutter = previousShutter
                                    }



                                },
                                modifier = Modifier
                                    .size(Dp(23f))
                            ) {
                                Text(text = "<")
                            }
                            Box(
                                contentAlignment = Alignment.BottomCenter,
                                modifier = Modifier
                                    .width(Dp(110f))
                                    .height(Dp(42f))
                            ) {
                                Text(
                                    text = currentShutter?.name ?: "Küche Fenster",
                                    textAlign = TextAlign.Center,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,

                                )
                            }
                            Button(
                                onClick = {
                                    if(shutterIndex < shutters!!.size - 1){
                                        shutterIndex++
                                    }else{
                                        shutterIndex = 0
                                    }

                                    val nextShutter = shutters!![shutterIndex]

                                    if(nextShutter.name == null){
                                        scope.launch {
                                            nextShutter.name = getName(client, nextShutter).await()
                                            updateShutters(shutters!!)
                                            currentShutter = nextShutter
                                        }
                                    }
                                    else {
                                        currentShutter = nextShutter
                                    }
                                },
                                modifier = Modifier
                                    .size(Dp(23f))
                            ) {
                                Text(text = ">")
                            }
                        }
                        Row(
                            verticalAlignment = Alignment.Bottom
                        ) {

                            Box(
                                modifier = Modifier
                                    .height(Dp(120f))
                                    .width(Dp(80f))
                                    .padding(top= Dp(10f), end = Dp(8f), bottom = Dp(5f))
                                    .onSizeChanged { size ->
                                        height = size.height
                                    }
                                    .pointerInput(Unit){
                                        while(true){
                                            awaitPointerEventScope {
                                                val event = awaitPointerEvent()
                                                event.changes.forEach{
                                                    pointerInputChange ->
                                                    if(pointerInputChange.changedToUp()){
                                                        val position = pointerInputChange.position
                                                        tapPosition = position.y.toInt()
                                                        val targetPosition = (tapPosition.toFloat()/height.toFloat()*100).toInt()
                                                        GlobalScope.launch {
                                                            push(client, "http://192.168.188.143/cmd?auth=Auablume", "{\"XC_FNC\":\"SendGenericCmd\",\"id\":\"" + currentShutter?.sid + "\",\"data\":{\"cmd\": \"moveTo\", \"value\": " + targetPosition + "}}").await()

                                                                do {
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
                                                                            ).asString == currentShutter?.sid
                                                                        }
                                                                    currentShutter?.position =
                                                                        shutter.asJsonObject.get("state").asJsonObject.get(
                                                                            "position"
                                                                        ).asInt
                                                                } while (shutter.asJsonObject.get("state").asJsonObject.get(
                                                                        "run_state"
                                                                    ).asInt != 0
                                                                )
                                                            }


                                                    }
                                                }
                                            }
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ){
                                Box(
                                    modifier = Modifier
                                        .border(Dp(1f), color = Color.Cyan)
                                        .fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ){
                                    Text(text = currentShutter!!.position.toString() + "%", textAlign = TextAlign.Center)
                                }

                            }
                            LazyColumn(
                                modifier = Modifier
                                    .padding(top = Dp(38f))
                            ) {
                                item {
                                    Button(
                                        onClick = {
                                            println("I have been pressed; " + "up")

                                            GlobalScope.launch {
                                                val response = push(
                                                    client,
                                                    "http://192.168.188.143/cmd?auth=Auablume",
                                                    "{\"XC_FNC\":\"SendGenericCmd\",\"id\":\"" + (currentShutter?.sid) + "\",\"data\":{\"cmd\": \"moveUp\"}}"
                                                )
                                                println(response.await().body?.string())
                                            }
                                        },
                                        modifier = Modifier
                                            .height(Dp(40f))
                                            .padding(top = Dp(5f))
                                    )
                                    {
                                        Text(text = "Up")
                                    }
                                }

                                item {
                                    Button(
                                        onClick = {

                                            println("I have been pressed; " + "stop")
                                            GlobalScope.launch {
                                                val response = push(
                                                    client,
                                                    "http://192.168.188.143/cmd?auth=Auablume",
                                                    "{\"XC_FNC\":\"SendGenericCmd\",\"id\":\"" + (currentShutter?.sid) + "\",\"data\":{\"cmd\": \"stop\"}}"
                                                )
                                                println(response.await().body?.string())
                                            }
                                        },
                                        modifier = Modifier
                                            .height(Dp(40f))
                                            .padding(top = Dp(5f))
                                    )
                                    {
                                        Text(text = "Stop")
                                    }
                                }

                                item {
                                    Button(
                                        onClick = {
                                            println("I have been pressed; " + "down")
                                            GlobalScope.launch {
                                                val response = push(
                                                    client,
                                                    "http://192.168.188.143/cmd?auth=Auablume",
                                                    "{\"XC_FNC\":\"SendGenericCmd\",\"id\":\"" + (currentShutter?.sid) + "\",\"data\":{\"cmd\": \"moveDown\"}}"
                                                )
                                                println(response.await().body?.string())
                                            }
                                        },
                                        modifier = Modifier
                                            .height(Dp(40f))
                                            .padding(top = Dp(5f))
                                    )
                                    {
                                        Text(text = "Down")
                                    }
                                }

                            }
                        }
                    }
                }
            }
        }
    }


    private fun updateShutters(shutters: List<Shutter>){
        this.shutters = shutters
    }

    @Preview(device = Devices.WEAR_OS_SMALL_ROUND)
    @Composable
    fun DefaultPreview(){
        WearApp(true)
    }

}


