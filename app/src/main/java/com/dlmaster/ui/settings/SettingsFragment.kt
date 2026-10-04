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

        // 主题色
        val themeRow = v.findViewById<LinearLayout>(R.id.theme_row)
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
                    if (th.key == current.key) setStroke((3 * resources.displayMetrics.density).toInt(), 0xFFFFFFFF.toInt())
                    else setStroke((1 * resources.displayMetrics.density).toInt(), 0x40FFFFFF)
                }
                setOnClickListener {
                    ThemeManager.setCurrent(ctx, th.key)
                    try { (activity as? MainActivity)?.recreate() } catch (_: Throwable) {}
                }
            }
            themeRow.addView(dot)
        }

        // 布局模式
        val btnList = v.findViewById<com.google.android.material.button.MaterialButton>(R.id.btn_layout_list)
        val btnGrid = v.findViewById<com.google.android.material.button.MaterialButton>(R.id.btn_layout_grid)
        val btnCompact = v.findViewById<com.google.android.material.button.MaterialButton>(R.id.btn_layout_compact)

        fun updateLayoutBtn() {
            val m = ThemeManager.layoutMode(requireContext())
            fun setBtn(b: com.google.android.material.button.MaterialButton, active: Boolean) {
                try {
                    if (active) {
                        b.setTextColor(0xFFFFFFFF.toInt())
                        b.setBackgroundColor(ThemeManager.current(requireContext()).primary)
                    } else {
                        b.setTextColor(0xFFCCFFFFFF.toInt())
                        b.setBackgroundColor(0x00000000)
                    }
                } catch (_: Throwable) {}
            }
            setBtn(btnList, m == "list"); setBtn(btnGrid, m == "grid"); setBtn(btnCompact, m == "compact")
        }
        updateLayoutBtn()
        btnList.setOnClickListener { ThemeManager.setLayoutMode(requireContext(), "list"); updateLayoutBtn() }
        btnGrid.setOnClickListener { ThemeManager.setLayoutMode(requireContext(), "grid"); updateLayoutBtn() }
        btnCompact.setOnClickListener { ThemeManager.setLayoutMode(requireContext(), "compact"); updateLayoutBtn() }

        // 帧率
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

        // 开关
        val swBg = v.findViewById<Switch>(R.id.sw_bg)
        val swMusic = v.findViewById<Switch>(R.id.sw_music)
        val swClip = v.findViewById<Switch>(R.id.sw_clip)
        val swIsland = v.findViewById<Switch>(R.id.sw_island)

        swBg.isChecked = Prefs.bgEnabled(requireContext())
        swMusic.isChecked = Prefs.musicEnabled(requireContext())
        swClip.isChecked = Prefs.clipboardEnabled(requireContext())
        swIsland.isChecked = try { com.dlmaster.feature.Settings.islandEnabled() } catch (_: Throwable) { false }

        swBg.setOnCheckedChangeListener { _, c ->
            Prefs.setBgEnabled(requireContext(), c)
            try { (activity as? MainActivity)?.restartBgSchedule() } catch (_: Throwable) {}
        }
        swMusic.setOnCheckedChangeListener { _, c ->
            Prefs.setMusicEnabled(requireContext(), c)
            if (c) MusicPlayer.start(requireContext().applicationContext) else MusicPlayer.stop()
        }
        swClip.setOnCheckedChangeListener { _, c -> Prefs.setClipboardEnabled(requireContext(), c) }
        swIsland.setOnCheckedChangeListener { _, c ->
            try {
                com.dlmaster.feature.Settings.setIslandEnabled(c)
                val act = activity as? MainActivity
                if (c) act?.startIsland() else act?.stopIsland()
            } catch (_: Throwable) {}
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
