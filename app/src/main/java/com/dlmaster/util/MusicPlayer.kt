package com.dlmaster.util
import android.content.Context
import android.media.MediaPlayer
import java.io.File
import kotlin.random.Random
object MusicPlayer {
    private val DIRS = arrayOf("/storage/emulated/0/Music/玄音", "/storage/emulated/0/Music", "/storage/emulated/0/Download")
    private val EXTS = setOf("mp3","m4a","flac","ogg","wav","aac","wma")
    private var player: MediaPlayer? = null
    private var tracks: List<File> = emptyList()
    private var currentIndex = -1
    private var listener: (() -> Unit)? = null
    fun setListener(l: () -> Unit) { listener = l }
    fun start(ctx: Context) {
        if (tracks.isEmpty()) rescan(ctx)
        if (tracks.isEmpty()) return
        if (player == null) playRandom()
    }
    fun rescan(ctx: Context) {
        val list = mutableListOf<File>()
        for (d in DIRS) {
            val dir = File(d)
            if (dir.exists() && dir.isDirectory) try {
                dir.listFiles { f -> f.isFile && f.extension.lowercase() in EXTS }?.let { list.addAll(it) }
            } catch (_: Throwable) {}
        }
        tracks = list
    }
    fun next(ctx: Context) { playRandom() }
    fun pause() { try { player?.takeIf { it.isPlaying }?.pause() } catch (_: Throwable) {}; listener?.invoke() }
    fun resume() { try { player?.start() } catch (_: Throwable) {}; listener?.invoke() }
    fun isPlaying() = try { player?.isPlaying == true } catch (_: Throwable) { false }
    fun currentTrackName(): String? = if (currentIndex in tracks.indices) tracks[currentIndex].nameWithoutExtension else null
    fun stop() { try { player?.stop(); player?.release() } catch (_: Throwable) {}; player = null }
    private fun playRandom() {
        if (tracks.isEmpty()) return
        val idx: Int = if (tracks.size == 1) 0 else {
            var n: Int
            do { n = Random.nextInt(tracks.size) } while (n == currentIndex); n
        }
        currentIndex = idx
        try { player?.release() } catch (_: Throwable) {}
        try {
            player = MediaPlayer().apply {
                setDataSource(tracks[idx].absolutePath)
                setOnCompletionListener { playRandom() }
                setOnErrorListener { _, _, _ -> true }
                setVolume(0.7f, 0.7f); prepare(); start()
            }
            listener?.invoke()
        } catch (_: Throwable) { player = null }
    }
}
