package com.dlmaster.ui.download
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.dlmaster.R
import com.dlmaster.download.DownloadRepository
import com.dlmaster.download.DownloadTask
class DownloadFragment : Fragment() {
    private var adapter: DownloadAdapter? = null
    private var allTasks: List<DownloadTask> = emptyList()
    override fun onCreateView(i: LayoutInflater, c: ViewGroup?, s: Bundle?): View =
        i.inflate(R.layout.fragment_download, c, false)
    override fun onViewCreated(v: View, s: Bundle?) {
        val rv = v.findViewById<RecyclerView>(R.id.rv_downloads)
        val empty = v.findViewById<TextView>(R.id.tv_empty)
        val etSearch = v.findViewById<EditText>(R.id.et_search)
        rv.layoutManager = LinearLayoutManager(requireContext())
        rv.setHasFixedSize(true)
        rv.setItemViewCacheSize(12)
        rv.recycledViewPool.setMaxRecycledViews(0, 20)
        adapter = DownloadAdapter(DownloadRepository.tasks)
        rv.adapter = adapter
        DownloadRepository.live.observe(viewLifecycleOwner) { refreshFilter(etSearch.text.toString()) }
        etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
            override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) { refreshFilter(s?.toString() ?: "") }
            override fun afterTextChanged(s: Editable?) {}
        })
        refreshFilter("")
    }
    private fun refreshFilter(q: String) {
        val tasks = DownloadRepository.tasks
        if (q.isBlank()) {
            adapter?.notifyDataSetChanged()
        } else {
            val filtered = tasks.filter { it.fileName.contains(q, true) || it.url.contains(q, true) }
            adapter?.setFiltered(filtered)
        }
        view?.findViewById<TextView>(R.id.tv_empty)?.visibility =
            if (tasks.isEmpty()) View.VISIBLE else View.GONE
    }
}
