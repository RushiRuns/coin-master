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
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupWithNavController
import com.rushi.coinmaster.BuildConfig
import com.rushi.coinmaster.MainActivity
import com.rushi.coinmaster.R
import com.rushi.coinmaster.databinding.FragmentSettingsBinding
import com.rushi.coinmaster.util.CurrencyFormatter
import com.rushi.coinmaster.util.LocaleHelper
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import androidx.activity.result.contract.ActivityResultContracts
import android.net.Uri
import android.content.Intent
import android.widget.Toast
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: SettingsViewModel by viewModels()

    // Flag to prevent triggering a locale change when we're just syncing the UI
    private var isInitializing = true

    private val exportLauncher = registerForActivityResult(
        ActivityResultContracts.CreateDocument("application/octet-stream")
    ) { uri ->
        uri?.let { exportData(it) }
    }

    private val importLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let { confirmImport(it) }
    }

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

        // Setup toolbar with hamburger icon for sidebar navigation
        binding.toolbar.setupWithNavController(
            findNavController(),
            AppBarConfiguration(
                setOf(
                    R.id.homeFragment, R.id.transactionsFragment, R.id.budgetFragment,
                    R.id.nav_categories, R.id.nav_expense_type, R.id.nav_income,
                    R.id.nav_savings, R.id.nav_accounts, R.id.nav_notes,
                    R.id.nav_transfers, R.id.nav_goals, R.id.nav_settings
                ),
                (requireActivity() as MainActivity).drawerLayout
            )
        )

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

        // Observe saved theme and sync radio buttons
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.currentTheme.collect { theme ->
                    isInitializing = true
                    syncThemeRadioButtons(theme)
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

        // Listen for theme changes by the user
        binding.radioGroupTheme.setOnCheckedChangeListener { _, checkedId ->
            if (isInitializing) return@setOnCheckedChangeListener

            val newTheme = when (checkedId) {
                R.id.radio_theme_light -> "light"
                R.id.radio_theme_dark  -> "dark"
                else                   -> "system"
            }

            if (newTheme == viewModel.currentTheme.value) return@setOnCheckedChangeListener

            viewModel.setTheme(newTheme)
        }

        // Listen for clear all data button click
        binding.btnClearData.setOnClickListener {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.title_clear_data_dialog)
                .setMessage(R.string.msg_clear_data_dialog)
                .setNegativeButton(R.string.btn_cancel, null)
                .setPositiveButton(R.string.btn_clear) { dialog, _ ->
                    viewModel.clearAllData()
                    dialog.dismiss()
                }
                .show()
        }

        // Listen for export data button click
        binding.btnExportData.setOnClickListener {
            val dateStr = java.text.SimpleDateFormat("yyyyMMdd_HHmmss", java.util.Locale.getDefault()).format(java.util.Date())
            exportLauncher.launch("coinmaster_backup_$dateStr.db")
        }

        // Listen for import data button click
        binding.btnImportData.setOnClickListener {
            importLauncher.launch(arrayOf("*/*"))
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

    private fun syncThemeRadioButtons(theme: String) {
        val radioId = when (theme) {
            "light" -> R.id.radio_theme_light
            "dark"  -> R.id.radio_theme_dark
            else    -> R.id.radio_theme_system
        }
        binding.radioGroupTheme.check(radioId)
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

    private fun exportData(uri: Uri) {
        val contentResolver = requireContext().contentResolver
        val outputStream = try {
            contentResolver.openOutputStream(uri)
        } catch (e: Exception) {
            Toast.makeText(requireContext(), getString(R.string.toast_export_error, e.message ?: "Unknown error"), Toast.LENGTH_LONG).show()
            return
        }

        if (outputStream == null) {
            Toast.makeText(requireContext(), getString(R.string.toast_export_error, "Output stream unavailable"), Toast.LENGTH_LONG).show()
            return
        }

        setBackupButtonsEnabled(false)

        viewModel.exportDatabase(
            outputStream,
            onSuccess = {
                setBackupButtonsEnabled(true)
                Toast.makeText(requireContext(), getString(R.string.toast_export_success), Toast.LENGTH_SHORT).show()
            },
            onError = { error ->
                setBackupButtonsEnabled(true)
                Toast.makeText(requireContext(), getString(R.string.toast_export_error, error.message ?: "Unknown error"), Toast.LENGTH_LONG).show()
            }
        )
    }

    private fun confirmImport(uri: Uri) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.title_import_warning_dialog)
            .setMessage(R.string.msg_import_warning_dialog)
            .setNegativeButton(R.string.btn_cancel, null)
            .setPositiveButton(R.string.btn_restore) { dialog, _ ->
                performImport(uri)
                dialog.dismiss()
            }
            .show()
    }

    private fun performImport(uri: Uri) {
        val contentResolver = requireContext().contentResolver
        val inputStream = try {
            contentResolver.openInputStream(uri)
        } catch (e: Exception) {
            Toast.makeText(requireContext(), getString(R.string.toast_import_error, e.message ?: "Unknown error"), Toast.LENGTH_LONG).show()
            return
        }

        if (inputStream == null) {
            Toast.makeText(requireContext(), getString(R.string.toast_import_error, "Input stream unavailable"), Toast.LENGTH_LONG).show()
            return
        }

        setBackupButtonsEnabled(false)

        viewModel.importDatabase(
            inputStream,
            onSuccess = {
                Toast.makeText(requireContext(), getString(R.string.toast_import_success), Toast.LENGTH_SHORT).show()
                restartApp()
            },
            onError = { error ->
                setBackupButtonsEnabled(true)
                if (error.message?.contains("required CoinMaster database tables") == true || error.message?.contains("invalid") == true) {
                    Toast.makeText(requireContext(), getString(R.string.toast_invalid_backup), Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(requireContext(), getString(R.string.toast_import_error, error.message ?: "Unknown error"), Toast.LENGTH_LONG).show()
                }
            }
        )
    }

    private fun setBackupButtonsEnabled(enabled: Boolean) {
        if (_binding != null) {
            binding.btnExportData.isEnabled = enabled
            binding.btnImportData.isEnabled = enabled
        }
    }

    private fun restartApp() {
        val context = requireContext()
        val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)?.apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        }
        context.startActivity(intent)
        Runtime.getRuntime().exit(0)
    }
}
