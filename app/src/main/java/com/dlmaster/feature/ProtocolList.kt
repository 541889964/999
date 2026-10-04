package com.dlmaster.feature
object ProtocolList {
    val SUPPORTED = listOf(
        "HTTP/HTTPS" to "标准网页下载",
        "HTTP/2" to "多路复用加速",
        "HTTP/3 (QUIC)" to "低延迟传输",
        "FTP" to "文件传输协议",
        "SFTP" to "安全文件传输",
        "WebDAV" to "网盘协议",
        "BitTorrent" to "磁力/P2P(需种子)",
        "Metalink" to "多源描述文件",
        "HLS (m3u8)" to "苹果流媒体",
        "DASH (mpd)" to "国际流媒体",
        "迅雷 (thunder://)" to "解码后下载",
        "data: URI" to "内嵌资源",
        "blob:" to "浏览器临时资源"
    )
}
