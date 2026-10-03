package com.dlmaster.download

/**
 * 10 种下载方案
 */
enum class DownloadStrategy(
    val key: String,
    val displayName: String,
    val blurb: String,
    val threads: Int
) {
    SINGLE("single", "稳健单线程", "小文件最省心，不挑服务器", 1),
    T4("t4", "标准四线程", "普通文件的均衡选择", 4),
    T8("t8", "均衡八线程", "大多数场景推荐", 8),
    T16("t16", "高速十六线程", "大文件加速", 16),
    T32("t32", "极限三十二线程", "多核跑满带宽", 32),
    STEALTH("stealth", "伪装请求", "带 Referer/UA，绕过防盗链", 8),
    RESUME("resume", "断点续传", "上次没下完，从断点接着走", 16),
    HLS("hls", "HLS 分片合并", "m3u8 视频专用", 8),
    DASH("dash", "DASH 分片合并", "mpd 视频专用", 8),
    MIRROR("mirror", "多镜像回退", "多个备用地址轮流试", 8);

    companion object {
        fun byKey(k: String): DownloadStrategy =
            values().firstOrNull { it.key == k } ?: T8
    }
}
