package com.dlmaster.feature
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
object NetworkPolicy {
    fun isWifi(ctx: Context): Boolean = try {
        val cm = ctx.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val n = cm.activeNetwork ?: return false
        val cap = cm.getNetworkCapabilities(n) ?: return false
        cap.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
        cap.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
    } catch (_:Throwable) { true }
    fun canDownload(ctx: Context): Boolean {
        if (!Settings.wifiOnly()) return true
        return isWifi(ctx)
    }
}
