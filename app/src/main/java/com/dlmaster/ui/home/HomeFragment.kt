package com.dlmaster.ui.home
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.dlmaster.MainActivity
import com.dlmaster.R
import com.dlmaster.util.CommandDownloader
import com.dlmaster.util.MusicPlayer
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.snackbar.Snackbar

class HomeFragment : Fragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        inflater.inflate(R.layout.fragment_home, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val et = view.findViewById<EditText>(R.id.et_url)
        view.findViewById<MaterialButton>(R.id.btn_smart).setOnClickListener {
            val t = et.text.toString().trim()
            if (t.isEmpty()) { Snackbar.make(view, "请输入链接", Snackbar.LENGTH_SHORT).show(); return@setOnClickListener }
            CommandDownloader.smartDownload(t, requireContext())
            Snackbar.make(view, "已加入队列", Snackbar.LENGTH_SHORT).show()
            et.setText("")
        }
        view.findViewById<MaterialButton>(R.id.btn_direct).setOnClickListener {
            val t = et.text.toString().trim()
            if (t.isEmpty()) { Snackbar.make(view, "请输入直链", Snackbar.LENGTH_SHORT).show(); return@setOnClickListener }
            CommandDownloader.directDownload(t, requireContext())
            Snackbar.make(view, "16 线程直链下载已启动", Snackbar.LENGTH_SHORT).show()
            et.setText("")
        }

        // 音乐控制
        val tvMusic = view.findViewById<TextView>(R.id.tv_music_name)
        val btnToggle = view.findViewById<ImageButton>(R.id.btn_music_toggle)
        val btnNext = view.findViewById<ImageButton>(R.id.btn_music_next)

        fun refreshMusicUi() {
            val name = MusicPlayer.currentTrackName()
            tvMusic.text = name ?: getString(R.string.music_idle)
            btnToggle.setImageResource(
                if (MusicPlayer.isPlaying()) android.R.drawable.ic_media_pause
                else android.R.drawable.ic_media_play
            )
        }

        btnToggle.setOnClickListener {
            if (MusicPlayer.isPlaying()) MusicPlayer.pause() else MusicPlayer.resume()
            refreshMusicUi()
        }
        btnNext.setOnClickListener {
            MusicPlayer.next(requireContext())
            refreshMusicUi()
        }
        refreshMusicUi()

        // 快捷卡片
        view.findViewById<MaterialCardView>(R.id.card_browse).setOnClickListener {
            (activity as? MainActivity)?.findViewById<com.google.android.material.bottomnavigation.BottomNavigationView>(R.id.bottom_nav)
                ?.selectedItemId = R.id.nav_browser
        }
        view.findViewById<MaterialCardView>(R.id.card_downloads).setOnClickListener {
            (activity as? MainActivity)?.findViewById<com.google.android.material.bottomnavigation.BottomNavigationView>(R.id.bottom_nav)
                ?.selectedItemId = R.id.nav_download
        }
        view.findViewById<MaterialCardView>(R.id.card_rescan).setOnClickListener {
            MusicPlayer.rescan(requireContext())
            refreshMusicUi()
            Snackbar.make(view, "音乐已重新扫描", Snackbar.LENGTH_SHORT).show()
        }
    }
}
