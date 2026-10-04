package com.dlmaster.feature
import java.net.InetSocketAddress
import java.net.Proxy
object ProxyManager {
    /** 返回 OkHttp 的 proxy(设置里配置的) */
    fun current(): Proxy? {
        if (!Settings.proxyEnabled()) return null
        val u = Settings.proxyUrl().trim()
        if (u.isBlank()) return null
        return try {
            val s = u.removePrefix("http://").removePrefix("https://").removePrefix("socks5://")
            val parts = s.split(":")
            if (parts.size < 2) null
            else Proxy(Proxy.Type.HTTP, InetSocketAddress(parts[0], parts[1].toInt()))
        } catch (_:Throwable) { null }
    }
}
