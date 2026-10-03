package com.dlmaster.ui.download
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.dlmaster.R
import com.dlmaster.download.DownloadRepository
import com.dlmaster.util.RvOptimizer
import com.dlmaster.view.ShimmerView

class DownloadFragment : Fragment() {
    private var adapter: DownloadAdapter? = null
    override fun onCreateView(i: LayoutInflater, c: ViewGroup?, s: Bundle?): View =
        i.inflate(R.layout.fragment_download, c, false)
    override fun onViewCreated(v: View, s: Bundle?) {
        val rv = v.findViewById<RecyclerView>(R.id.rv_downloads)
        val etSearch = v.findViewById<EditText>(R.id.et_search)
        val shimmer = v.findViewById<ShimmerView>(R.id.shimmer)

        RvOptimizer.config(rv, requireContext())
        rv.layoutManager = LinearLayoutManager(requireContext())
        adapter = DownloadAdapter(DownloadRepository.tasks)
        rv.adapter = adapter

        // 空数据时短暂显示 shimmer(视觉上"在加载")
        if (DownloadRepository.tasks.isEmpty()) {
            shimmer.visibility = View.VISIBLE
            v.postDelayed({ shimmer.visibility = View.GONE }, 400)
        }

        DownloadRepository.live.observe(viewLifecycleOwner) { refreshFilter(etSearch.text.toString()) }
        etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
            override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {
                refreshFilter(s?.toString() ?: "")
            }
            override fun afterTextChanged(s: Editable?) {}
        })
        refreshFilter("")
    }
    private fun refreshFilter(q: String) {
        val tasks = DownloadRepository.tasks
        if (q.isBlank()) adapter?.setFiltered(null)
        else adapter?.setFiltered(tasks.filter {
            it.fileName.contains(q, true) || it.url.contains(q, true)
        })
        view?.findViewById<LinearLayout>(R.id.layout_empty)?.visibility =
            if (tasks.isEmpty()) View.VISIBLE else View.GONE
    }
}
