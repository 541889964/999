package com.dlmaster.netdisk

object NetdiskResolver {
    private val patterns = mapOf(
        "baidu"  to Regex("pan\\.baidu\\.com/s/\\w+"),
        "quark"  to Regex("pan\\.quark\\.cn/s/\\w+"),
        "uc"     to Regex("drive\\.uc\\.cn/s/\\w+"),
        "xunlei" to Regex("pan\\.xunlei\\.com/s/\\w+"),
        "123"    to Regex("(www\\.)?123pan\\.com/s/\\w+"),
        "139"    to Regex("caiyun\\.139\\.com/\\w+")
    )
    fun isNetdiskLink(url: String): Boolean =
        patterns.values.any { it.containsMatchIn(url) }
}
