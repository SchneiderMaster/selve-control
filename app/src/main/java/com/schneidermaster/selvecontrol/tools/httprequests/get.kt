package com.schneidermaster.selvecontrol.tools.httprequests

import kotlinx.coroutines.Deferred
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response

@OptIn(DelicateCoroutinesApi::class)
fun get(client: OkHttpClient, url: String): Deferred<Response> {
    return GlobalScope.async {
        val request = Request.Builder()
            .url(url)
            .build()

        client.newCall(request).execute()
    }
}

