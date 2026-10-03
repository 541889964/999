package com.dlmaster.ui.settings
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import android.widget.Switch
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.dlmaster.R
import com.dlmaster.util.MusicPlayer
import com.dlmaster.util.Prefs
class SettingsFragment : Fragment() {
    private val labels = arrayOf("3 秒","5 秒","10 秒","20 秒","30 秒","60 秒")
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, s: Bundle?): View =
        inflater.inflate(R.layout.fragment_settings, container, false)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val swBg = view.findViewById<Switch>(R.id.sw_bg)
        val swMusic = view.findViewById<Switch>(R.id.sw_music)
        val swTrans = view.findViewById<Switch>(R.id.sw_trans)
        val sb = view.findViewById<SeekBar>(R.id.sb_freq)
        val tvFreq = view.findViewById<TextView>(R.id.tv_freq)
        swBg.isChecked = Prefs.bgEnabled(requireContext())
        swMusic.isChecked = Prefs.musicEnabled(requireContext())
        swTrans.isChecked = Prefs.translucent(requireContext())
        sb.progress = Prefs.bgFreqIndex(requireContext())
        tvFreq.text = labels[sb.progress]
        sb.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(s: SeekBar?, p: Int, fromUser: Boolean) {
                tvFreq.text = labels[p]; Prefs.setBgFreqIndex(requireContext(), p)
            }
            override fun onStartTrackingTouch(s: SeekBar?) {}
            override fun onStopTrackingTouch(s: SeekBar?) {}
        })
        swBg.setOnCheckedChangeListener { _, c -> Prefs.setBgEnabled(requireContext(), c) }
        swMusic.setOnCheckedChangeListener { _, c ->
            Prefs.setMusicEnabled(requireContext(), c)
            if (c) MusicPlayer.start(requireContext().applicationContext) else MusicPlayer.stop()
        }
        swTrans.setOnCheckedChangeListener { _, c -> Prefs.setTranslucent(requireContext(), c) }
    }
}
