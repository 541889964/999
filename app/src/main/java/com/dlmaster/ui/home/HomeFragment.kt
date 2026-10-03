package com.dlmaster.ui.home
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.fragment.app.Fragment
import com.dlmaster.R
import com.dlmaster.util.CommandDownloader
import com.google.android.material.button.MaterialButton
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
            Snackbar.make(view, "直链下载已启动", Snackbar.LENGTH_SHORT).show()
            et.setText("")
        }
    }
}
