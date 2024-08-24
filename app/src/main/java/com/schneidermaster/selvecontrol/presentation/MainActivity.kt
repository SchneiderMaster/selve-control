/* While this template provides a good starting point for using Wear Compose, you can always
 * take a look at https://github.com/android/wear-os-samples/tree/main/ComposeStarter and
 * https://github.com/android/wear-os-samples/tree/main/ComposeAdvanced to find the most up to date
 * changes to the libraries and their usages.
 */

@file:OptIn(DelicateCoroutinesApi::class)

package com.schneidermaster.selvecontrol.presentation

import android.content.Context
import android.content.res.Configuration
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.TextField
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.input.pointer.changedToUp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.core.math.MathUtils.clamp
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.CircularProgressIndicator
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import com.google.gson.GsonBuilder
import com.schneidermaster.selvecontrol.presentation.theme.SelveControlTheme
import com.schneidermaster.selvecontrol.tools.httprequests.get
import com.schneidermaster.selvecontrol.tools.httprequests.push
import com.schneidermaster.selvecontrol.tools.shutters.Shutter
import com.schneidermaster.selvecontrol.tools.shutters.getName
import com.schneidermaster.selvecontrol.tools.shutters.getShutters
import com.schneidermaster.selvecontrol.tools.shutters.updatePositionOfShutter
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import kotlin.math.roundToInt

class MainActivity : ComponentActivity() {

    private var shutters: List<Shutter>? = null

    lateinit var connectivityManager: ConnectivityManager

    private lateinit var callback: ConnectivityManager.NetworkCallback

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()

        super.onCreate(savedInstanceState)

        connectivityManager = getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager
        callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                super.onAvailable(network)
                connectivityManager.bindProcessToNetwork(network)
                println("Network acquired")
            }
        }

        connectivityManager.requestNetwork(
            NetworkRequest.Builder().addTransportType(NetworkCapabilities.TRANSPORT_WIFI).build(),
            callback
        )




        println(fileList().asList())

        setTheme(android.R.style.Theme_DeviceDefault)

        setContent {
            WearApp()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (!isChangingConfigurations) {
            val gson = GsonBuilder()
                .serializeNulls()
                .create()
            openFileOutput("shutterData.json", Context.MODE_PRIVATE).use {
                it.write(gson.toJson(shutters).toByteArray())
                println(fileList().asList())
            }
        }

        connectivityManager.bindProcessToNetwork(null)
        connectivityManager.unregisterNetworkCallback(callback)

    }

    @Composable
    fun WearApp(debugging: Boolean = false) {

        val sharedPreferences =
            getSharedPreferences("com.schneidermaster.selvecontrol", MODE_PRIVATE)
        val editor = sharedPreferences.edit()

        // editor.putString("serverIP", "192.168.188.143")
        // editor.putString("serverPassword", "Auablume")

        editor.apply()

        var serverIP by remember {
            mutableStateOf(
                sharedPreferences.getString("serverIP", "").toString()
            )
        }
        var serverPassword by remember {
            mutableStateOf(
                sharedPreferences.getString(
                    "serverPassword",
                    ""
                ).toString()
            )
        }

        val client = OkHttpClient()

        val scope = rememberCoroutineScope()

        var job: Job? = null

        var isLoading by remember { mutableStateOf(true) }
        var currentShutter by remember { mutableStateOf<Shutter?>(null) }
        var shutters by remember { mutableStateOf<List<Shutter>?>(null) }
        var shutterIndex by remember { mutableIntStateOf(0) }
        var tapPosition by remember { mutableIntStateOf(0) }
        var height by remember { mutableIntStateOf(0) }
        var previewPosition by remember { mutableIntStateOf(-1) }
        var serverIpText by remember { mutableStateOf("") }
        var serverPasswordText by remember { mutableStateOf("") }



        SelveControlTheme(
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colors.background),
                contentAlignment = Alignment.Center
            )
            {
                if (serverIP == "") {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        TextField(
                            value = serverIpText,
                            onValueChange = {
                                serverIpText = it
                            },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                            label = { Text("Server IP") })
                        TextField(
                            value = serverPasswordText,
                            onValueChange = {
                                serverPasswordText = it
                            },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                            label = { Text("Server Password") })

                        Button(onClick = {

                            try {
                                scope.launch {
                                    println("I'm trying really hard")
                                    get(
                                        client,
                                        "http://$serverIP/cmd?XC_FNC=GetStates&auth=$serverPassword"
                                    ).await()
                                    serverIP = serverIpText
                                    serverPassword = serverPasswordText
                                }
                            } catch (e: Error) {
                                println("ERROR LOLOLOL")
                                val text = "Wrong Server IP/ Password!"
                                val duration = Toast.LENGTH_SHORT

                                Toast.makeText(this@MainActivity, text, duration).show()
                            }


                        }) {
                            Text("Accept")
                        }

                    }
                } else {

                    if (isLoading && !debugging) {
                        LaunchedEffect(Unit) {
                            scope.launch {
                                shutters = getShutters(client, this@MainActivity).await()
                                    .toMutableList()
                                val firstShutter = shutters!![0]
                                if (firstShutter.name == null) {
                                    firstShutter.name =
                                        getName(client, firstShutter).await()
                                }
                                currentShutter = firstShutter
                                updateShutters(shutters!!)
                                isLoading = false
                            }
                        }

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier
                                    .padding(bottom = Dp(10f))
                            )
                            Text(
                                text = "Receiving data from shutters...",
                                textAlign = TextAlign.Center
                            )
                        }

                    } else {
                        Box(
                            contentAlignment = Alignment.TopCenter
                        ) {
                            Row(
                                verticalAlignment = Alignment.Bottom
                            ) {
                                Button(
                                    onClick = {
                                        if (shutterIndex > 0) {
                                            shutterIndex--
                                        } else {
                                            shutterIndex = shutters!!.size - 1
                                        }

                                        val previousShutter = shutters!![shutterIndex]

                                        job?.cancel()
                                        currentShutter = previousShutter
                                        job = scope.launch {
                                            withContext(Dispatchers.IO) {
                                                updatePositionOfShutter(
                                                    client,
                                                    previousShutter
                                                ) { updatedShutter ->
                                                    scope.launch {
                                                        withContext(Dispatchers.Main) {
                                                            currentShutter = updatedShutter
                                                            updateShutters(shutters!!)
                                                            shutters =
                                                                shutters!!.mapIndexed { index, value ->
                                                                    if (index == shutterIndex) currentShutter!! else value
                                                                }
                                                        }
                                                    }
                                                }
                                            }
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
                                        text = currentShutter?.name ?: "Kitchen Window test long",
                                        textAlign = TextAlign.Center,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis,

                                        )
                                }
                                Button(
                                    onClick = {
                                        if (shutterIndex < shutters!!.size - 1) {
                                            shutterIndex++
                                        } else {
                                            shutterIndex = 0
                                        }

                                        val nextShutter = shutters!![shutterIndex]

                                        job?.cancel()
                                        currentShutter = nextShutter
                                        job = scope.launch {
                                            withContext(Dispatchers.IO) {
                                                updatePositionOfShutter(
                                                    client,
                                                    nextShutter
                                                ) { updatedShutter ->
                                                    scope.launch {
                                                        withContext(Dispatchers.Main) {
                                                            currentShutter = updatedShutter
                                                            updateShutters(shutters!!)
                                                            shutters =
                                                                shutters!!.mapIndexed { index, value ->
                                                                    if (index == shutterIndex) currentShutter!! else value
                                                                }
                                                        }
                                                    }
                                                }
                                            }
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
                                        .padding(top = Dp(10f), end = Dp(8f), bottom = Dp(5f))
                                        .onSizeChanged { size ->
                                            height = size.height
                                        }
                                        .pointerInput(Unit) {
                                            while (true) {
                                                awaitPointerEventScope {
                                                    val event = awaitPointerEvent()
                                                    event.changes.forEach { pointerInputChange ->

                                                        if (pointerInputChange.pressed) {
                                                            val position =
                                                                pointerInputChange.position
                                                            tapPosition = position.y.toInt()
                                                            val targetPosition =
                                                                (tapPosition.toFloat() / height.toFloat() * 100).toInt()

                                                            previewPosition =
                                                                clamp(targetPosition, 0, 100)

                                                        }

                                                        if (pointerInputChange.changedToUp()) {
                                                            previewPosition = -1
                                                            val position =
                                                                pointerInputChange.position
                                                            tapPosition = position.y.toInt()
                                                            val targetPosition =
                                                                clamp(
                                                                    (tapPosition.toFloat() / height.toFloat() * 100).toInt(),
                                                                    0,
                                                                    100
                                                                )
                                                            GlobalScope.launch {
                                                                push(
                                                                    client,
                                                                    "http://$serverIP/cmd?auth=$serverPassword",
                                                                    "{\"XC_FNC\":\"SendGenericCmd\",\"id\":\"" + currentShutter?.sid + "\",\"data\":{\"cmd\": \"moveTo\", \"value\": " + targetPosition + "}}"
                                                                ).await()

                                                                updatePositionOfShutter(
                                                                    client,
                                                                    currentShutter
                                                                ) { updatedShutter ->
                                                                    currentShutter = updatedShutter
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    BoxWithConstraints(
                                        modifier = Modifier
                                            .border(Dp(1f), color = Color.Cyan)
                                            .fillMaxSize()
                                            .clip(RectangleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        val parentHeight = constraints.maxHeight

                                        val yOffset = if (debugging) {
                                            (parentHeight - parentHeight * 0.4).roundToInt()
                                        } else {
                                            if (previewPosition == -1) {
                                                (parentHeight - parentHeight * currentShutter!!.position!!.toFloat() / 100).roundToInt()
                                            } else {
                                                (parentHeight - parentHeight * previewPosition.toFloat() / 100).roundToInt()
                                            }
                                        }

                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .offset { IntOffset(0, -yOffset) },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .background(Color.Blue)
                                            )
                                        }
                                        Text(
                                            text = if (debugging) {
                                                "40%"
                                            } else {
                                                if (previewPosition == -1) {
                                                    currentShutter!!.position.toString() + "%"
                                                } else {
                                                    "$previewPosition%"
                                                }
                                            },
                                            textAlign = TextAlign.Center
                                        )
                                    }

                                }
                                LazyColumn(
                                    modifier = Modifier
                                        .padding(top = Dp(38f))
                                ) {
                                    item {
                                        Button(
                                            onClick = {
                                                GlobalScope.launch {
                                                    push(
                                                        client,
                                                        "http://$serverIP/cmd?auth=$serverPassword",
                                                        "{\"XC_FNC\":\"SendGenericCmd\",\"id\":\"" + (currentShutter?.sid) + "\",\"data\":{\"cmd\": \"moveUp\"}}"
                                                    ).await()
                                                    updatePositionOfShutter(
                                                        client,
                                                        currentShutter
                                                    ) { updatedShutter ->
                                                        currentShutter = updatedShutter
                                                    }
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
                                                GlobalScope.launch {
                                                    push(
                                                        client,
                                                        "http://$serverIP/cmd?auth=$serverPassword",
                                                        "{\"XC_FNC\":\"SendGenericCmd\",\"id\":\"" + (currentShutter?.sid) + "\",\"data\":{\"cmd\": \"stop\"}}"
                                                    ).await()
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
                                                GlobalScope.launch {
                                                    push(
                                                        client,
                                                        "http://$serverIP/cmd?auth=$serverPassword",
                                                        "{\"XC_FNC\":\"SendGenericCmd\",\"id\":\"" + (currentShutter?.sid) + "\",\"data\":{\"cmd\": \"moveDown\"}}"
                                                    ).await()
                                                    updatePositionOfShutter(
                                                        client,
                                                        currentShutter
                                                    ) { updatedShutter ->
                                                        currentShutter = updatedShutter
                                                    }
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
    }


    private fun updateShutters(shutters: List<Shutter>) {
        this.shutters = shutters
    }

    @Preview(
        device = Devices.WEAR_OS_SMALL_ROUND,
        showSystemUi = true,
        showBackground = true,
        uiMode = Configuration.UI_MODE_TYPE_WATCH
    )
    @Composable
    fun DefaultPreview() {
        WearApp(true)
    }
}


