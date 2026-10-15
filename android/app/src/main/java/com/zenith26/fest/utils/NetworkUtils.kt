package com.zenith26.fest.utils

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

object NetworkUtils {

    fun isNetworkAvailable(context: Context): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
        val activeNetwork = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(activeNetwork) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    fun getReadableErrorMessage(e: Throwable): String {
        return when (e) {
            is ConnectException -> "Cannot connect to server. Ensure uvicorn is running on your host machine."
            is SocketTimeoutException -> "Connection timed out. Server took too long to respond."
            is UnknownHostException -> "Server host unreachable. Check your network configuration."
            else -> e.localizedMessage ?: "An unexpected network error occurred."
        }
    }
}
