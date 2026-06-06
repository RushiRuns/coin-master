package com.rushi.coinmaster.ui.onboarding

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.rushi.coinmaster.R
import com.rushi.coinmaster.databinding.FragmentNameCurrencyBinding

class NameCurrencyFragment : Fragment() {

    private var _binding: FragmentNameCurrencyBinding? = null
    private val binding get() = _binding!!

    private val viewModel: OnboardingViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentNameCurrencyBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Setup Name Input
        binding.etName.setText(viewModel.userName)
        binding.etName.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                viewModel.userName = s?.toString() ?: ""
                binding.tilName.error = null
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        // Setup Currency Dropdown
        val currencies = arrayOf("INR", "USD", "EUR", "GBP")
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, currencies)
        binding.actvCurrency.setAdapter(adapter)
        binding.actvCurrency.setText(viewModel.preferredCurrency, false)
        binding.actvCurrency.setOnItemClickListener { _, _, position, _ ->
            viewModel.preferredCurrency = currencies[position]
        }
    }

    fun validateAndShowError(): Boolean {
        return if (viewModel.validateStep1()) {
            binding.tilName.error = null
            true
        } else {
            binding.tilName.error = getString(R.string.ob_error_name_empty)
            false
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
