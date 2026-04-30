package com.xmm.videodownloader

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.fragment.app.Fragment
import com.xmm.videodownloader.databinding.FragmentSettingsBinding

class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupLanguageSpinner()
        setupColorGrid()
    }

    private fun setupLanguageSpinner() {
        val adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            LanguageManager.languages.map { it.displayName }
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerLanguage.adapter = adapter
        binding.spinnerLanguage.setSelection(LanguageManager.getSelectedIndex(requireContext()))

        binding.spinnerLanguage.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, pos: Int, id: Long) {
                val selected = LanguageManager.languages[pos]
                if (selected.code != LanguageManager.getSelectedCode(requireContext())) {
                    (activity as? MainActivity)?.switchLanguage(selected.code)
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun setupColorGrid() {
        val colorViews = listOf(
            binding.colorBlue,
            binding.colorPurple,
            binding.colorGreen,
            binding.colorOrange,
            binding.colorRed,
            binding.colorTeal,
            binding.colorPink,
            binding.colorIndigo
        )

        val selectedIndex = ThemeManager.getSelectedIndex(requireContext())

        ThemeManager.themeColors.forEachIndexed { index, themeColor ->
            if (index < colorViews.size) {
                val colorInt = android.graphics.Color.parseColor(themeColor.colorHex)
                colorViews[index].setBackgroundColor(colorInt)

                if (index == selectedIndex) {
                    colorViews[index].alpha = 1.0f
                    binding.tvSelectedColor.text = themeColor.name
                } else {
                    colorViews[index].alpha = 0.6f
                }

                colorViews[index].setOnClickListener {
                    (activity as? MainActivity)?.switchTheme(index)
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
