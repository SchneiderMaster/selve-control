/* While this template provides a good starting point for using Wear Compose, you can always
 * take a look at https://github.com/android/wear-os-samples/tree/main/ComposeStarter and
 * https://github.com/android/wear-os-samples/tree/main/ComposeAdvanced to find the most up to date
 * changes to the libraries and their usages.
 */

package com.schneidermaster.selvecontrol.presentation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import com.schneidermaster.selvecontrol.presentation.theme.SelveControlTheme
import com.schneidermaster.selvecontrol.tools.httprequests.post
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

@OptIn(DelicateCoroutinesApi::class)
@Composable
fun WearApp() {

    val client = OkHttpClient()

    SelveControlTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colors.background),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
            ){
                ScalingLazyColumn {
                    item {
                        Button(
                            onClick = {
                                println("I have been pressed; " + "up")

                                GlobalScope.launch {
                                    val response = post(client, "http://192.168.188.143/cmd?auth=Auablume", "{\"XC_FNC\":\"SendGenericCmd\",\"id\":\"0B\",\"data\":{\"cmd\": \"moveUp\"}}")
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

                                println("I have been pressed; " + "down")
                                GlobalScope.launch {
                                    val response = post(client, "http://192.168.188.143/cmd?auth=Auablume", "{\"XC_FNC\":\"SendGenericCmd\",\"id\":\"0B\",\"data\":{\"cmd\": \"moveDown\"}}")
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

@Preview(device = Devices.WEAR_OS_SMALL_ROUND, showSystemUi = true)
@Composable
fun DefaultPreview() {
    WearApp()
}

