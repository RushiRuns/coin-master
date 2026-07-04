package com.rushi.coinmaster.ui.onboarding

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.rushi.coinmaster.R
import com.rushi.coinmaster.databinding.FragmentWelcomeBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class WelcomeFragment : Fragment() {

    private var _binding: FragmentWelcomeBinding? = null
    private val binding get() = _binding!!

    private val viewModel: OnboardingViewModel by activityViewModels()

    private val importLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let { performImport(it) }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentWelcomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.cardStartFresh.setOnClickListener {
            findNavController().navigate(R.id.action_welcomeFragment_to_onboardingFragment)
        }

        binding.cardRestoreBackup.setOnClickListener {
            importLauncher.launch(arrayOf("*/*"))
        }
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

        setButtonsEnabled(false)

        viewModel.importDatabase(
            inputStream,
            onSuccess = {
                Toast.makeText(requireContext(), getString(R.string.toast_import_success), Toast.LENGTH_SHORT).show()
                restartApp()
            },
            onError = { error ->
                setButtonsEnabled(true)
                if (error.message?.contains("required CoinMaster database tables") == true || error.message?.contains("invalid") == true) {
                    Toast.makeText(requireContext(), getString(R.string.toast_invalid_backup), Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(requireContext(), getString(R.string.toast_import_error, error.message ?: "Unknown error"), Toast.LENGTH_LONG).show()
                }
            }
        )
    }

    private fun setButtonsEnabled(enabled: Boolean) {
        if (_binding != null) {
            binding.cardStartFresh.isEnabled = enabled
            binding.cardRestoreBackup.isEnabled = enabled
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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
