package com.dlmaster.ui.settings

import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.dlmaster.MainActivity
import com.dlmaster.R
import com.dlmaster.theme.ThemeManager
import com.dlmaster.util.MusicPlayer
import com.dlmaster.util.Prefs

class SettingsFragment : Fragment() {

    override fun onCreateView(i: LayoutInflater, c: ViewGroup?, s: Bundle?): View =
        i.inflate(R.layout.fragment_settings, c, false)

    override fun onViewCreated(v: View, s: Bundle?) {
        val ctx = requireContext()

        // ============ 主题色 ============
        val themeRow = v.findViewById<LinearLayout>(R.id.theme_row)
        themeRow.removeAllViews()
        val current = ThemeManager.current(ctx)
        ThemeManager.ALL.forEach { th ->
            val dot = View(ctx).apply {
                val size = (48 * resources.displayMetrics.density).toInt()
                layoutParams = LinearLayout.LayoutParams(size, size).apply {
                    marginEnd = (14 * resources.displayMetrics.density).toInt()
                }
                background = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    setColor(th.primary)
                    if (th.key == current.key) {
                        setStroke((3 * resources.displayMetrics.density).toInt(), 0xFFFFFFFF.toInt())
                    } else {
                        setStroke((1 * resources.displayMetrics.density).toInt(), 0x40FFFFFF)
                    }
                }
                setOnClickListener {
                    ThemeManager.setCurrent(ctx, th.key)
                    try { (activity as? MainActivity)?.recreate() } catch (_: Throwable) {}
                }
            }
            themeRow.addView(dot)
        }

        // ============ 帧率 ============
        val sbFr = v.findViewById<SeekBar>(R.id.sb_framerate)
        val tvFr = v.findViewById<TextView>(R.id.tv_framerate_val)
        val frValues = intArrayOf(60, 90, 120)
        val curFr = try { com.dlmaster.feature.Settings.frameRate() } catch (_: Throwable) { 60 }
        sbFr.progress = when (curFr) { 90 -> 1; 120 -> 2; else -> 0 }
        tvFr.text = "$curFr FPS"
        sbFr.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(s: SeekBar?, p: Int, fromUser: Boolean) {
                val fr = frValues[p.coerceIn(0, 2)]
                tvFr.text = "$fr FPS"
                try { com.dlmaster.feature.Settings.setFrameRate(fr) } catch (_: Throwable) {}
                if (fromUser) try { (activity as? MainActivity)?.recreate() } catch (_: Throwable) {}
            }
            override fun onStartTrackingTouch(s: SeekBar?) {}
            override fun onStopTrackingTouch(s: SeekBar?) {}
        })

        val swAdaptive = v.findViewById<Switch>(R.id.sw_adaptive_fr)
        swAdaptive.isChecked = try { com.dlmaster.feature.Settings.adaptiveFrameRate() } catch (_: Throwable) { true }
        swAdaptive.setOnCheckedChangeListener { _, c ->
            try { com.dlmaster.feature.Settings.setAdaptiveFrameRate(c) } catch (_: Throwable) {}
        }

        // ============ 灵动岛开关组 ============
        val swIsland = v.findViewById<Switch>(R.id.sw_island)
        val swBatt = v.findViewById<Switch>(R.id.sw_island_batt)
        val swTime = v.findViewById<Switch>(R.id.sw_island_time)
        val swNet = v.findViewById<Switch>(R.id.sw_island_net)
        val swTap = v.findViewById<Switch>(R.id.sw_island_tap)

        swIsland.isChecked = try { com.dlmaster.feature.Settings.islandEnabled() } catch (_: Throwable) { false }
        swBatt.isChecked = try { com.dlmaster.feature.Settings.islandShowBattery() } catch (_: Throwable) { true }
        swTime.isChecked = try { com.dlmaster.feature.Settings.islandShowTime() } catch (_: Throwable) { true }
        swNet.isChecked = try { com.dlmaster.feature.Settings.islandShowNet() } catch (_: Throwable) { true }
        swTap.isChecked = try { com.dlmaster.feature.Settings.islandTapExpand() } catch (_: Throwable) { true }

        swIsland.setOnCheckedChangeListener { _, c ->
            try {
                com.dlmaster.feature.Settings.setIslandEnabled(c)
                val act = activity as? MainActivity
                if (c) act?.startIsland() else act?.stopIsland()
            } catch (_: Throwable) {}
        }
        swBatt.setOnCheckedChangeListener { _, c ->
            try {
                com.dlmaster.feature.Settings.setIslandShowBattery(c)
                // 重启服务使设置生效
                val act = activity as? MainActivity
                if (act != null && com.dlmaster.feature.Settings.islandEnabled()) {
                    act.stopIsland()
                    v.postDelayed({ try { act.startIsland() } catch (_: Throwable) {} }, 400)
                }
            } catch (_: Throwable) {}
        }
        swTime.setOnCheckedChangeListener { _, c ->
            try {
                com.dlmaster.feature.Settings.setIslandShowTime(c)
                val act = activity as? MainActivity
                if (act != null && com.dlmaster.feature.Settings.islandEnabled()) {
                    act.stopIsland()
                    v.postDelayed({ try { act.startIsland() } catch (_: Throwable) {} }, 400)
                }
            } catch (_: Throwable) {}
        }
        swNet.setOnCheckedChangeListener { _, c ->
            try {
                com.dlmaster.feature.Settings.setIslandShowNet(c)
                val act = activity as? MainActivity
                if (act != null && com.dlmaster.feature.Settings.islandEnabled()) {
                    act.stopIsland()
                    v.postDelayed({ try { act.startIsland() } catch (_: Throwable) {} }, 400)
                }
            } catch (_: Throwable) {}
        }
        swTap.setOnCheckedChangeListener { _, c ->
            try { com.dlmaster.feature.Settings.setIslandTapExpand(c) } catch (_: Throwable) {}
        }

        // ============ 基础开关 ============
        val swBg = v.findViewById<Switch>(R.id.sw_bg)
        val swMusic = v.findViewById<Switch>(R.id.sw_music)
        val swClip = v.findViewById<Switch>(R.id.sw_clip)

        swBg.isChecked = Prefs.bgEnabled(requireContext())
        swMusic.isChecked = Prefs.musicEnabled(requireContext())
        swClip.isChecked = Prefs.clipboardEnabled(requireContext())

        swBg.setOnCheckedChangeListener { _, c ->
            Prefs.setBgEnabled(requireContext(), c)
            try { (activity as? MainActivity)?.restartBgSchedule() } catch (_: Throwable) {}
        }
        swMusic.setOnCheckedChangeListener { _, c ->
            Prefs.setMusicEnabled(requireContext(), c)
            if (c) MusicPlayer.start(requireContext().applicationContext)
            else MusicPlayer.stop()
        }
        swClip.setOnCheckedChangeListener { _, c ->
            Prefs.setClipboardEnabled(requireContext(), c)
        }

        v.findViewById<View>(R.id.card_clear_cache)?.setOnClickListener {
            try {
                requireContext().cacheDir.listFiles()?.forEach { it.deleteRecursively() }
                requireContext().externalCacheDir?.listFiles()?.forEach { it.deleteRecursively() }
                Toast.makeText(requireContext(), "缓存已清", Toast.LENGTH_SHORT).show()
            } catch (_: Throwable) {}
        }
    }
}
