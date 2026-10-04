package com.dlmaster.download
enum class DownloadStrategy(
    val key: String, val displayName: String, val blurb: String, val threads: Int
) {
    SINGLE("single","稳健单线程","小文件最省心",1),
    T4("t4","标准四线程","普通文件均衡",4),
    T8("t8","均衡八线程","大多数场景推荐",8),
    T16("t16","高速十六线程","大文件加速",16),
    T32("t32","极限三十二线程","跑满带宽",32),
    STEALTH("stealth","伪装请求","绕过防盗链",8),
    RESUME("resume","断点续传","接着上次走",16),
    HLS("hls","HLS 分片合并","m3u8 专用",8),
    DASH("dash","DASH 分片合并","mpd 专用",8),
    MIRROR("mirror","多镜像回退","备用地址",8),
    T64("t64","超极限六十四线程","高并发服务器",64),
    ADAPTIVE("adaptive","自适应动态线程","按速度自动增",16),
    LOW_MEM("lowmem","低内存模式","1GB 设备用",4),
    HTTP2("http2","强制 HTTP/2","多路复用",16),
    IPV6("ipv6","IPv6 优先","绕 IPv4 拥塞",16),
    FAST_PROBE("fastprobe","快速探测","先测速再决定",16),
    MULTI_MIRROR("multimirror","多镜像并行","同时多源",32),
    STRICT_CHUNK("strict","严格分块校验","每块独立校验",16),
    SMALL_FIRST("smallfirst","小文件优先","先清小文件",8),
    PERSIST_RESUME("persist","持久化断点","跨进程续传",16);
    companion object {
        fun byKey(k: String): DownloadStrategy = values().firstOrNull { it.key == k } ?: T8
    }
}
