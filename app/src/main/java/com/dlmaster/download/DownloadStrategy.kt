package com.dlmaster.download

enum class DownloadStrategy(
    val key: String,
    val title: String,
    val desc: String,
    val threads: Int,
    val group: String
) {
    SINGLE("single","稳健单线程","小文件",1,"基础"),
    T4("t4","四线程","普通文件",4,"基础"),
    T8("t8","八线程","推荐",8,"基础"),
    T16("t16","十六线程","大文件",16,"基础"),
    T32("t32","三十二线程","跑满带宽",32,"基础"),
    T64("t64","六十四线程","高并发服务器",64,"基础"),
    STEALTH("stealth","伪装请求","绕防盗链",8,"场景"),
    RESUME("resume","断点续传","接着上次",16,"场景"),
    HLS("hls","HLS 合并","m3u8 专用",8,"场景"),
    DASH("dash","DASH 合并","mpd 专用",8,"场景"),
    MIRROR("mirror","多镜像回退","备用地址",8,"场景"),
    MULTI_MIRROR("multimirror","多镜像并行","同时多源",32,"场景"),
    HTTP2("http2","强制 HTTP/2","多路复用",16,"协议"),
    IPV6("ipv6","IPv6 优先","绕拥塞",16,"协议"),
    QUIC("quic","QUIC/HTTP3","低延迟",16,"协议"),
    FTP("ftp","FTP","传统协议",8,"协议"),
    WEBDAV("webdav","WebDAV","网盘协议",8,"协议"),
    SFTP("sftp","SFTP","安全传输",8,"协议"),
    ADAPTIVE("adaptive","自适应线程","按速自动",16,"高级"),
    LOW_MEM("lowmem","低内存","1GB 设备",4,"高级"),
    FAST_PROBE("fastprobe","快速探测","先测速",16,"高级"),
    STRICT_CHUNK("strict","严格分块","每块校验",16,"高级"),
    SMALL_FIRST("smallfirst","小文件优先","队列排序",8,"高级"),
    PERSIST_RESUME("persist","持久断点","跨进程续",16,"高级"),
    PARALLEL_MIRROR("pmirror","多源并行分片","多源分工",48,"新增"),
    SEGMENT_SYNC("segsync","段同步","每段独立重试",24,"新增"),
    PIECE_HASH("phash","分块哈希","SHA 校验",16,"新增"),
    THUNDER_ACCEL("thunder","迅雷协议","原生解析",16,"新增"),
    BT_TORRENT("torrent","BT 种子","torrent",32,"新增"),
    METALINK("metalink","Metalink","多源描述",32,"新增"),
    HTTP_PIPELINE("pipeline","HTTP 流水线","批量请求",8,"新增"),
    CACHE_FIRST("cache","缓存优先","先查本地",8,"新增"),
    BANDWIDTH_AWARE("bwaware","带宽感知","动态调",16,"新增"),
    AUTO_FALLBACK("autofb","自动降级","失败换路",16,"新增");

    companion object {
        fun byKey(k: String): DownloadStrategy = values().firstOrNull { it.key == k } ?: T8
        fun groups(): Map<String, List<DownloadStrategy>> = values().groupBy { it.group }
    }
}
