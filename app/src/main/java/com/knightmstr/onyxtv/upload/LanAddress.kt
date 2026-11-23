package com.knightmstr.onyxtv.upload

import android.content.Context
import android.net.ConnectivityManager
import java.net.Inet4Address
import java.net.NetworkInterface

/** Finds the TV's own IPv4 address on the local network, so the uploader URL can point at it. */
object LanAddress {

    fun wifiIpv4(context: Context): String? =
        fromConnectivityManager(context) ?: fromInterfaces()

    private fun fromConnectivityManager(context: Context): String? {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return null
        val network = cm.activeNetwork ?: return null
        val linkProps = cm.getLinkProperties(network) ?: return null
        return linkProps.linkAddresses
            .map { it.address }
            .firstOrNull { it is Inet4Address && !it.isLoopbackAddress && it.isSiteLocalAddress }
            ?.hostAddress
    }

    private fun fromInterfaces(): String? = runCatching {
        NetworkInterface.getNetworkInterfaces().toList()
            .asSequence()
            .filter { runCatching { it.isUp && !it.isLoopback }.getOrDefault(false) }
            .flatMap { it.inetAddresses.toList().asSequence() }
            .firstOrNull { it is Inet4Address && !it.isLoopbackAddress && it.isSiteLocalAddress }
            ?.hostAddress
    }.getOrNull()
}
