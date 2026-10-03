package com.dlmaster.ui.settings
import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.dlmaster.MainActivity
import com.dlmaster.R
import com.dlmaster.util.MusicPlayer
import com.dlmaster.util.Prefs
import java.io.File
class SettingsFragment : Fragment() {
    private val labels = arrayOf("3 秒","5 秒","10 秒","20 秒","30 秒","60 秒")
    override fun onCreateView(i: LayoutInflater, c: ViewGroup?, s: Bundle?): View =
        i.inflate(R.layout.fragment_settings, c, false)
    override fun onViewCreated(v: View, s: Bundle?) {
        val swBg = v.findViewById<Switch>(R.id.sw_bg)
        val swTrans = v.findViewById<Switch>(R.id.sw_trans)
        val swMusic = v.findViewById<Switch>(R.id.sw_music)
        val swClip = v.findViewById<Switch>(R.id.sw_clip)
        val sb = v.findViewById<SeekBar>(R.id.sb_freq)
        val tvFreq = v.findViewById<TextView>(R.id.tv_freq)
        val tvCache = v.findViewById<TextView>(R.id.tv_cache_size)
        val tvVersion = v.findViewById<TextView>(R.id.tv_version)

        swBg.isChecked = Prefs.bgEnabled(requireContext())
        swTrans.isChecked = Prefs.translucent(requireContext())
        swMusic.isChecked = Prefs.musicEnabled(requireContext())
        swClip.isChecked = Prefs.clipboardEnabled(requireContext())
        sb.progress = Prefs.bgFreqIndex(requireContext())
        tvFreq.text = labels[sb.progress]
        tvVersion.text = "v8.0"
        tvCache.text = calcCacheSize()

        sb.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(s: SeekBar?, p: Int, fromUser: Boolean) {
                tvFreq.text = labels[p]
                Prefs.setBgFreqIndex(requireContext(), p)
                if (fromUser) (activity as? MainActivity)?.restartBgSchedule()
            }
            override fun onStartTrackingTouch(s: SeekBar?) {}
            override fun onStopTrackingTouch(s: SeekBar?) {}
        })
        swBg.setOnCheckedChangeListener { _, c ->
            Prefs.setBgEnabled(requireContext(), c)
            (activity as? MainActivity)?.restartBgSchedule()
        }
        swTrans.setOnCheckedChangeListener { _, c ->
            Prefs.setTranslucent(requireContext(), c)
            Toast.makeText(requireContext(), if (c) "玻璃卡片已开" else "已关", Toast.LENGTH_SHORT).show()
        }
        swMusic.setOnCheckedChangeListener { _, c ->
            Prefs.setMusicEnabled(requireContext(), c)
            if (c) MusicPlayer.start(requireContext().applicationContext) else MusicPlayer.stop()
        }
        swClip.setOnCheckedChangeListener { _, c -> Prefs.setClipboardEnabled(requireContext(), c) }

        v.findViewById<View>(R.id.card_clear_cache).setOnClickListener {
            clearCache(); tvCache.text = calcCacheSize()
            Toast.makeText(requireContext(), "缓存已清", Toast.LENGTH_SHORT).show()
        }
        v.findViewById<View>(R.id.card_about).setOnClickListener {
            AlertDialog.Builder(requireContext()).setTitle("关于下载工具").setMessage(
                "版本 v8.0\n\n找最快的路，一直免费。\n\n把链接粘进来，剩下的交给它。"
            ).setPositiveButton("好", null).show()
        }
    }
    private fun calcCacheSize(): String {
        var size = 0L
        try {
            val dir = requireContext().cacheDir
            dir.walkTopDown().forEach { if (it.isFile) size += it.length() }
        } catch (_: Throwable) {}
        return when {
            size >= 1024L * 1024 * 1024 -> "%.2f GB".format(size / 1024.0 / 1024 / 1024)
            size >= 1024L * 1024 -> "%.2f MB".format(size / 1024.0 / 1024)
            size >= 1024L -> "%.1f KB".format(size / 1024.0)
            else -> "$size B"
        }
    }
    private fun clearCache() {
        try {
            requireContext().cacheDir.listFiles()?.forEach { it.deleteRecursively() }
            val ext = requireContext().externalCacheDir
            ext?.listFiles()?.forEach { it.deleteRecursively() }
        } catch (_: Throwable) {}
    }
}
