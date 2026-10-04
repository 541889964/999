package com.dlmaster.netdisk
object NetdiskResolver {
    private val pats = listOf(
        Regex("pan\\.baidu\\.com/s/\\w+"), Regex("pan\\.quark\\.cn/s/\\w+"),
        Regex("drive\\.uc\\.cn/s/\\w+"), Regex("pan\\.xunlei\\.com/s/\\w+"),
        Regex("(www\\.)?123pan\\.com/s/\\w+"), Regex("caiyun\\.139\\.com/\\w+"))
    fun isNetdiskLink(url: String) = pats.any { it.containsMatchIn(url) }
}
