package com.dlmaster.download

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File

object Aria2Engine {

    private var initialized = false

    private fun directArgs(): List<String> = listOf(
        "--split=32",
        "--max-connection-per-server=32",
        "--min-split-size=1M",
        "--piece-length=1M",
        "--continue=true",
        "--file-allocation=none",
        "--disk-cache=64M",
        "--enable-http-keep-alive=true",
        "--enable-http-pipelining=true",
        "--max-tries=10",
        "--retry-wait=1",
        "--lowest-speed-limit=0",
        "--max-concurrent-downloads=5",
        "--check-integrity=false",
        "--auto-file-renaming=false",
        "--allow-overwrite=true",
        "--console-log-level=warn",
        "--summary-interval=1",
        "--user-agent=Mozilla/5.0 (Linux; Android 12) AppleWebKit/537.36"
    )

    private fun normalArgs(connections: Int): List<String> = listOf(
        "--split=$connections",
        "--max-connection-per-server=16",
        "--min-split-size=1M",
        "--continue=true",
        "--file-allocation=none",
        "--max-tries=5",
        "--retry-wait=3",
        "--console-log-level=warn",
        "--summary-interval=1",
        "--user-agent=Mozilla/5.0 (Linux; Android 12) AppleWebKit/537.36"
    )

    fun ensureInit(context: Context) {
        if (initialized) return
        val bin = File(context.filesDir, "aria2c")
        if (!bin.exists()) {
            runCatching {
                context.assets.open("aria2c").use { input ->
                    bin.outputStream().use { out -> input.copyTo(out) }
                }
                bin.setExecutable(true)
            }
        }
        initialized = true
    }

    fun startDownload(
        context: Context,
        url: String,
        savePath: String,
        direct: Boolean = false,
        connections: Int = 16,
        onProgress: (Long, Long) -> Unit
    ) {
        ensureInit(context)
        val bin = File(context.filesDir, "aria2c")
        if (!bin.exists()) { onProgress(0L, 0L); return }

        val args = mutableListOf(bin.absolutePath, "--dir=$savePath")
        args += if (direct) directArgs() else normalArgs(connections)
        args += url

        ProcessBuilder(args)
            .redirectErrorStream(true)
            .start()
            .also { process ->
                CoroutineScope(Dispatchers.IO).launch {
                    process.inputStream.bufferedReader().forEachLine { line ->
                        parseLine(line, onProgress)
                    }
                }
            }
    }

    private fun parseLine(line: String, onProgress: (Long, Long) -> Unit) {
        val regex = Regex("""SIZE:([\d.]+)(\w+)/([\d.]+)(\w+)""")
        regex.find(line)?.let { m ->
            onProgress(
                parseSize(m.groupValues[1], m.groupValues[2]),
                parseSize(m.groupValues[3], m.groupValues[4])
            )
        }
    }

    private fun parseSize(value: String, unit: String): Long {
        val v = value.toDoubleOrNull() ?: return 0
        return when (unit.uppercase()) {
            "KIB", "KB" -> (v * 1024).toLong()
            "MIB", "MB" -> (v * 1024 * 1024).toLong()
            "GIB", "GB" -> (v * 1024 * 1024 * 1024).toLong()
            else -> v.toLong()
        }
    }
}
