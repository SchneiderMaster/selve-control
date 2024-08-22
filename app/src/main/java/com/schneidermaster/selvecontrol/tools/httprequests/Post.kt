package com.schneidermaster.selvecontrol.tools.httprequests

import kotlinx.coroutines.Deferred
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import kotlin.concurrent.thread

@OptIn(DelicateCoroutinesApi::class)
fun post(client: OkHttpClient, url: String, json: String): Deferred<Response> {

    return GlobalScope.async {
        val body = json.toRequestBody("application/json".toMediaType())
        val request = Request.Builder()
            .url(url)
            .post(body)
            .build()

        client.newCall(request).execute()
    }
}