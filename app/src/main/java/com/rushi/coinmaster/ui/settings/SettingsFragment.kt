package com.rushi.coinmaster.ui.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.rushi.coinmaster.BuildConfig
import com.rushi.coinmaster.R
import com.rushi.coinmaster.databinding.FragmentSettingsBinding
import com.rushi.coinmaster.util.CurrencyFormatter
import com.rushi.coinmaster.util.LocaleHelper
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: SettingsViewModel by viewModels()

    // Flag to prevent triggering a locale change when we're just syncing the UI
    private var isInitializing = true

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Set app version
        binding.tvAppVersion.text = getString(R.string.label_app_version) + " " + BuildConfig.VERSION_NAME

        // Observe saved language and sync radio buttons without triggering a change
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.currentLanguage.collect { langCode ->
                    isInitializing = true
                    syncRadioButtons(langCode)
                    updateCurrencyPreview(langCode)
                    isInitializing = false
                }
            }
        }

        // Listen for radio button changes by the user
        binding.radioGroupLanguage.setOnCheckedChangeListener { _, checkedId ->
            if (isInitializing) return@setOnCheckedChangeListener

            val newLang = when (checkedId) {
                R.id.radio_hindi   -> "hi"
                R.id.radio_marathi -> "mr"
                else               -> "en"
            }

            val currentLang = LocaleHelper.getLanguage(requireContext())
            if (newLang == currentLang) return@setOnCheckedChangeListener

            // 1. Persist the selection
            viewModel.setLanguage(newLang)

            // 2. Apply the locale to the current Context (also persists to SharedPrefs)
            LocaleHelper.setLocale(requireContext(), newLang)

            // 3. Show the note so the user knows a refresh is happening
            binding.tvLanguageNote.visibility = View.VISIBLE

            // 4. Recreate the Activity — the cheapest way to apply locale changes
            //    fully (re-inflates all views, re-reads resources).
            requireActivity().recreate()
        }
    }

    private fun syncRadioButtons(langCode: String) {
        val radioId = when (langCode) {
            "hi" -> R.id.radio_hindi
            "mr" -> R.id.radio_marathi
            else -> R.id.radio_english
        }
        binding.radioGroupLanguage.check(radioId)
    }

    private fun updateCurrencyPreview(langCode: String) {
        // Show how ₹12,34,567 looks in the active locale — proves Lakh/Crore grouping
        val sampleAmount = 1_23_45_678_00L // ₹1,23,45,678 (twelve crore rupees)
        val preview = CurrencyFormatter.format(sampleAmount, langCode, showPaise = false)
        binding.tvCurrencyPreview.text = preview
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
