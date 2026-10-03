package com.dlmaster.ui.download
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.dlmaster.R
import com.dlmaster.download.DownloadRepository
class DownloadFragment : Fragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        inflater.inflate(R.layout.fragment_download, container, false)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val rv = view.findViewById<RecyclerView>(R.id.rv_downloads)
        rv.layoutManager = LinearLayoutManager(requireContext())
        rv.setHasFixedSize(true)
        rv.itemAnimator = null
        rv.adapter = DownloadAdapter(DownloadRepository.tasks)
    }
}
