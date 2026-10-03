package com.dlmaster.ui.settings
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Switch
import androidx.fragment.app.Fragment
import com.dlmaster.R
import com.dlmaster.util.Prefs
class SettingsFragment : Fragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        inflater.inflate(R.layout.fragment_settings, container, false)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val sw = view.findViewById<Switch>(R.id.sw_bg)
        sw.isChecked = Prefs.bgEnabled(requireContext())
        sw.setOnCheckedChangeListener { _, c -> Prefs.setBgEnabled(requireContext(), c) }
    }
}
