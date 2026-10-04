package com.dlmaster.feature
object TorrentHelper {
    val DEFAULT_TRACKERS = listOf(
        "udp://tracker.opentrackr.org:1337/announce",
        "udp://open.tracker.cl:1337/announce",
        "udp://9.rarbg.com:2810/announce",
        "udp://tracker.openbittorrent.com:6969/announce",
        "udp://opentracker.i2p.rocks:6969/announce",
        "https://tracker.tamersunion.org:443/announce",
        "udp://tracker.torrent.eu.org:451/announce",
        "udp://exodus.desync.com:6969/announce"
    )
    fun appendTrackers(magnet: String): String {
        if (!magnet.startsWith("magnet:")) return magnet
        if (magnet.contains("tr=")) return magnet
        val suffix = DEFAULT_TRACKERS.joinToString("") { "&tr=" + java.net.URLEncoder.encode(it, "UTF-8") }
        return magnet + suffix
    }
}
