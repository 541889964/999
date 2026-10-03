package com.dlmaster.util
import android.content.Context
import android.media.MediaPlayer
import java.io.File
import kotlin.random.Random

object MusicPlayer {

    private const val MUSIC_DIR = "/storage/emulated/0/Music/玄音"
    private val EXTS = setOf("mp3", "m4a", "flac", "ogg", "wav", "aac", "wma")

    private var player: MediaPlayer? = null
    private var tracks: List<File> = emptyList()
    private var currentIndex = -1
    private var appCtx: Context? = null

    fun start(ctx: Context) {
        appCtx = ctx.applicationContext
        if (tracks.isEmpty()) rescan(ctx)
        if (tracks.isEmpty()) return
        if (player == null) playRandom(ctx)
    }

    fun rescan(ctx: Context) {
        tracks = scan(File(MUSIC_DIR))
    }

    fun next(ctx: Context) {
        playRandom(ctx.applicationContext)
    }

    fun pause() { try { player?.takeIf { it.isPlaying }?.pause() } catch (_: Throwable) {} }
    fun resume() { try { player?.start() } catch (_: Throwable) {} }
    fun isPlaying(): Boolean = try { player?.isPlaying == true } catch (_: Throwable) { false }

    fun currentTrackName(): String? {
        if (currentIndex in tracks.indices) return tracks[currentIndex].nameWithoutExtension
        return null
    }

    fun stop() {
        try { player?.stop(); player?.release() } catch (_: Throwable) {}
        player = null
    }

    private fun playRandom(ctx: Context) {
        if (tracks.isEmpty()) return
        val idx: Int = if (tracks.size == 1) 0 else {
            var n: Int
            do { n = Random.nextInt(tracks.size) } while (n == currentIndex)
            n
        }
        currentIndex = idx
        try { player?.release() } catch (_: Throwable) {}
        try {
            player = MediaPlayer().apply {
                setDataSource(tracks[idx].absolutePath)
                setOnCompletionListener { playRandom(ctx) }
                setOnErrorListener { _, _, _ -> true }
                setVolume(0.7f, 0.7f)
                prepare()
                start()
            }
        } catch (_: Throwable) {
            player = null
        }
    }

    private fun scan(dir: File): List<File> {
        if (!dir.exists() || !dir.isDirectory) return emptyList()
        val direct = try {
            dir.listFiles { f -> f.isFile && f.extension.lowercase() in EXTS }?.toList() ?: emptyList()
        } catch (_: Throwable) { emptyList() }
        return direct
    }
}
