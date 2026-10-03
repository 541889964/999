package com.dlmaster.ui.download
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.dlmaster.R
import com.dlmaster.download.DownloadRepository
class DownloadFragment : Fragment() {
    private lateinit var adapter: DownloadAdapter
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        inflater.inflate(R.layout.fragment_download, container, false)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val rv = view.findViewById<RecyclerView>(R.id.rv_downloads)
        val empty = view.findViewById<TextView>(R.id.tv_empty)
        rv.layoutManager = LinearLayoutManager(requireContext())
        rv.setHasFixedSize(true)
        rv.itemAnimator = null
        adapter = DownloadAdapter(DownloadRepository.tasks)
        rv.adapter = adapter
        empty.visibility = if (DownloadRepository.tasks.isEmpty()) View.VISIBLE else View.GONE
    }
    override fun onResume() {
        super.onResume()
        try { adapter.notifyDataSetChanged() } catch (_: Throwable) {}
    }
}
