package com.dlmaster.sniffer

import android.util.Base64

object ThunderParser {
    fun parse(thunderUrl: String): String? {
        if (!thunderUrl.startsWith("thunder://")) return null
        return runCatching {
            val encoded = thunderUrl.removePrefix("thunder://")
            val decoded = String(Base64.decode(encoded, Base64.DEFAULT))
            decoded.removePrefix("AA").removeSuffix("ZZ")
        }.getOrNull()
    }
    fun isThunder(url: String) = url.startsWith("thunder://")
}
