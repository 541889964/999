package com.dlmaster.sniffer
import android.util.Base64
object ThunderParser {
    fun parse(t: String): String? {
        if (!t.startsWith("thunder://")) return null
        return try {
            val d = String(Base64.decode(t.removePrefix("thunder://"), Base64.DEFAULT))
            d.removePrefix("AA").removeSuffix("ZZ")
        } catch (_: Throwable) { null }
    }
}
