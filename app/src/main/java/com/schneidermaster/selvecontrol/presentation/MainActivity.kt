@file:OptIn(DelicateCoroutinesApi::class)

package com.schneidermaster.selvecontrol.presentation

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.ImageShader
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.input.pointer.changedToUp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.core.math.MathUtils.clamp
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.CircularProgressIndicator
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.dialog.Alert
import com.google.gson.GsonBuilder
import com.schneidermaster.selvecontrol.R
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

    @OptIn(ExperimentalFoundationApi::class)
    @Composable
    fun WearApp() {

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

        var cycleJob: Job? = null
        var moveJob: Job? = null
        var updateJob: Job? = null

        var isLoading by remember { mutableStateOf(true) }
        var currentShutter by remember { mutableStateOf<Shutter?>(null) }
        var shutters by remember { mutableStateOf<List<Shutter>?>(null) }
        var shutterIndex by remember { mutableIntStateOf(0) }
        var tapPosition by remember { mutableIntStateOf(0) }
        var height by remember { mutableIntStateOf(0) }
        var previewPosition by remember { mutableIntStateOf(-1) }
        var serverIpText by remember { mutableStateOf("") }
        var serverPasswordText by remember { mutableStateOf("") }
        var isResetting by remember { mutableStateOf(false) }
        var isDeleting by remember { mutableStateOf(false) }



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
                            label = { Text(resources.getString(R.string.server_ip)) })
                        TextField(
                            value = serverPasswordText,
                            onValueChange = {
                                serverPasswordText = it
                            },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                            label = { Text(resources.getString(R.string.server_password)) })

                        Button(onClick = {

                            GlobalScope.launch {
                                try {
                                    get(
                                        client,
                                        "http://$serverIpText/cmd?XC_FNC=GetStates&auth=$serverPasswordText"
                                    ).await()

                                    editor.putString("serverIP", serverIpText)
                                    editor.putString("serverPassword", serverPasswordText)
                                    editor.apply()
                                    serverPassword = serverPasswordText
                                    serverIP = serverIpText

                                } catch (e: Exception) {
                                    val text = resources.getString(R.string.server_wrong)
                                    val duration = Toast.LENGTH_SHORT

                                    Toast.makeText(this@MainActivity, text, duration).show()
                                }
                            }


                        }) {
                            Text(resources.getString(R.string.accept))
                        }

                    }
                } else {
                    if (isLoading) {
                        LaunchedEffect(Unit) {
                            GlobalScope.launch {
                                shutters = getShutters(
                                    client,
                                    this@MainActivity,
                                    serverIP,
                                    serverPassword
                                ).await()
                                    .toMutableList()
                                val firstShutter = shutters!![0]
                                if (firstShutter.name == null) {
                                    firstShutter.name =
                                        getName(
                                            client,
                                            firstShutter,
                                            serverIP,
                                            serverPassword
                                        ).await()
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
                                text = resources.getString(R.string.loading),
                                textAlign = TextAlign.Center
                            )
                        }

                    } else {
                        if (isResetting) {
                            Alert(
                                contentPadding = PaddingValues(
                                    start = 10.dp,
                                    end = 10.dp,
                                    top = 24.dp,
                                    bottom = 32.dp
                                ),
                                title = {
                                    Text(
                                        resources.getString(R.string.reset_confirmation),
                                        textAlign = TextAlign.Center
                                    )
                                },
                                negativeButton = {
                                    Button(onClick = {
                                        isResetting = false
                                    }) { Text(resources.getString(R.string.no)) }
                                },
                                positiveButton = {
                                    Button(onClick = {
                                        editor.putString("serverIP", "")
                                        editor.putString("serverPassword", "")

                                        serverIpText = ""
                                        serverPasswordText = ""

                                        serverIP = ""
                                        serverPassword = ""

                                        isResetting = false
                                        isLoading = true

                                        this@MainActivity.deleteFile("shutterData.json")

                                        editor.apply()
                                    }) { Text(resources.getString(R.string.yes)) }
                                }
                            ) {

                            }
                        }
                        else if(isDeleting){

                            Alert(
                                contentPadding = PaddingValues(
                                    start = 10.dp,
                                    end = 10.dp,
                                    top = 24.dp,
                                    bottom = 32.dp
                                ),
                                title = {
                                    Text(
                                        resources.getString(R.string.delete_confirmation),
                                        textAlign = TextAlign.Center
                                    )
                                },
                                negativeButton = {
                                    Button(onClick = {
                                        isDeleting = false
                                    }) { Text(resources.getString(R.string.no)) }
                                },
                                positiveButton = {
                                    Button(onClick = {

                                        isDeleting = false
                                        isLoading = true

                                        this@MainActivity.deleteFile("shutterData.json")
                                    }) { Text(resources.getString(R.string.yes)) }
                                }
                            ) {

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
                                            if (shutterIndex > 0) {
                                                shutterIndex--
                                            } else {
                                                shutterIndex = shutters!!.size - 1
                                            }

                                            val previousShutter = shutters!![shutterIndex]

                                            moveJob?.cancel()
                                            cycleJob?.cancel()
                                            updateJob?.cancel()
                                            currentShutter = previousShutter
                                            cycleJob = GlobalScope.launch {
                                                withContext(Dispatchers.IO) {
                                                    updatePositionOfShutter(
                                                        client,
                                                        previousShutter, serverIP, serverPassword
                                                    ) { updatedShutter ->
                                                        updateJob = scope.launch {
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
                                            .combinedClickable(
                                                onClick = {
                                                    isDeleting = true
                                                },
                                                onLongClick = {
                                                    isResetting = true
                                                }
                                            ),
                                    ) {
                                        Text(
                                            text = currentShutter?.name
                                                ?: "Kitchen Window test long",
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

                                            moveJob?.cancel()
                                            cycleJob?.cancel()
                                            updateJob?.cancel()
                                            currentShutter = nextShutter
                                            cycleJob = GlobalScope.launch {
                                                withContext(Dispatchers.IO) {
                                                    updatePositionOfShutter(
                                                        client,
                                                        nextShutter, serverIP, serverPassword
                                                    ) { updatedShutter ->
                                                        updateJob = scope.launch {
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
                                                                moveJob?.cancel()
                                                                moveJob = GlobalScope.launch {
                                                                    push(
                                                                        client,
                                                                        "http://$serverIP/cmd?auth=$serverPassword",
                                                                        "{\"XC_FNC\":\"SendGenericCmd\",\"id\":\"" + currentShutter?.sid + "\",\"data\":{\"cmd\": \"moveTo\", \"value\": " + targetPosition + "}}"
                                                                    ).await()

                                                                    updatePositionOfShutter(
                                                                        client,
                                                                        currentShutter,
                                                                        serverIP,
                                                                        serverPassword
                                                                    ) { updatedShutter ->
                                                                        currentShutter =
                                                                            updatedShutter
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
                                                .border(Dp(1f), color = Color.Gray)
                                                .fillMaxSize()
                                                .clip(RectangleShape),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            val parentHeight = constraints.maxHeight

                                            val yOffset =
                                                if (previewPosition == -1) {
                                                    (parentHeight - parentHeight * currentShutter!!.position!!.toFloat() / 100).roundToInt()
                                                } else {
                                                    (parentHeight - parentHeight * previewPosition.toFloat() / 100).roundToInt()

                                            }

                                            Box(
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .offset { IntOffset(0, -yOffset) },
                                                contentAlignment = Alignment.Center
                                            ) {

                                                // image from https://www.pinterest.de/pin/612278511829119061/
                                                val image =
                                                    ImageBitmap.imageResource(id = R.drawable.shutter_image)
                                                val brush = remember(image) {
                                                    ShaderBrush(
                                                        shader = ImageShader(
                                                            image = image,
                                                            tileModeX = TileMode.Repeated,
                                                            tileModeY = TileMode.Repeated
                                                        )
                                                    )
                                                }

                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxSize()
                                                        .background(brush = brush)
                                                )

                                                Image(
                                                    painter = painterResource(id = R.drawable.shutter_image),
                                                    contentDescription = "Shutter",
                                                    modifier = Modifier
                                                        .fillMaxSize()
                                                )

                                            }
                                            Text(
                                                text =
                                                    if (previewPosition == -1) {
                                                        currentShutter!!.position.toString() + "%"
                                                    } else {
                                                        "$previewPosition%"
                                                    },
                                                color = Color.Black,
                                                textAlign = TextAlign.Center,
                                                modifier = Modifier
                                                    .background(color = Color(0.8f, 0.8f, 0.8f, 0.7f), shape = CircleShape)
                                                    .padding(2.dp)
                                                    .width(35.dp)
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
                                                    moveJob?.cancel()
                                                    moveJob = GlobalScope.launch {
                                                        push(
                                                            client,
                                                            "http://$serverIP/cmd?auth=$serverPassword",
                                                            "{\"XC_FNC\":\"SendGenericCmd\",\"id\":\"" + (currentShutter?.sid) + "\",\"data\":{\"cmd\": \"moveUp\"}}"
                                                        ).await()
                                                        updatePositionOfShutter(
                                                            client,
                                                            currentShutter, serverIP, serverPassword
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
                                                Text(text = resources.getString(R.string.up))
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
                                                Text(text = resources.getString(R.string.Stop))
                                            }
                                        }

                                        item {
                                            Button(
                                                onClick = {
                                                    moveJob?.cancel()
                                                    moveJob = GlobalScope.launch {
                                                        push(
                                                            client,
                                                            "http://$serverIP/cmd?auth=$serverPassword",
                                                            "{\"XC_FNC\":\"SendGenericCmd\",\"id\":\"" + (currentShutter?.sid) + "\",\"data\":{\"cmd\": \"moveDown\"}}"
                                                        ).await()
                                                        updatePositionOfShutter(
                                                            client,
                                                            currentShutter, serverIP, serverPassword
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
                                                Text(text = resources.getString(R.string.down))
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
    }


    private fun updateShutters(shutters: List<Shutter>) {
        this.shutters = shutters
    }
}


