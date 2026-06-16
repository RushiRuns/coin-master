package com.rushi.coinmaster.ui.onboarding

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.rushi.coinmaster.R
import com.rushi.coinmaster.databinding.FragmentSetIncomeBinding
import com.rushi.coinmaster.databinding.ItemOnboardingIncomeStreamBinding
import com.rushi.coinmaster.util.CurrencyFormatter
import com.rushi.coinmaster.util.LocaleHelper

class SetIncomeFragment : Fragment() {

    private var _binding: FragmentSetIncomeBinding? = null
    private val binding get() = _binding!!

    private val viewModel: OnboardingViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSetIncomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Bind clicks
        binding.btnAddStream.setOnClickListener {
            addIncomeStreamFromInput()
        }

        // Render initially
        renderIncomeStreams()
    }

    private fun addIncomeStreamFromInput() {
        val name = binding.etStreamName.text?.toString()?.trim() ?: ""
        val amountStr = binding.etIncome.text?.toString()?.trim() ?: ""
        
        var hasError = false
        if (name.isEmpty()) {
            binding.tilStreamName.error = "Name cannot be empty"
            hasError = true
        } else {
            binding.tilStreamName.error = null
        }

        val amount = amountStr.toDoubleOrNull()
        if (amount == null || amount <= 0.0) {
            binding.tilIncome.error = getString(R.string.ob_error_income_invalid)
            hasError = true
        } else {
            binding.tilIncome.error = null
        }

        if (hasError) return

        viewModel.addIncomeStream(name, amount!!)
        
        // Reset fields
        binding.etStreamName.text = null
        binding.etIncome.text = null
        binding.tilStreamName.error = null
        binding.tilIncome.error = null

        renderIncomeStreams()
    }

    private fun renderIncomeStreams() {
        binding.llIncomeStreams.removeAllViews()
        val languageCode = LocaleHelper.getLanguage(requireContext())

        viewModel.incomeStreams.forEachIndexed { index, stream ->
            val itemBinding = ItemOnboardingIncomeStreamBinding.inflate(
                layoutInflater,
                binding.llIncomeStreams,
                false
            )
            itemBinding.tvStreamName.text = stream.name
            itemBinding.tvStreamAmount.text = CurrencyFormatter.format(stream.amountPaise, languageCode)
            itemBinding.btnDelete.setOnClickListener {
                viewModel.removeIncomeStream(index)
                renderIncomeStreams()
            }
            binding.llIncomeStreams.addView(itemBinding.root)
        }

        // Update Total
        val totalPaise = viewModel.incomeStreams.sumOf { it.amountPaise }
        binding.tvTotalIncome.text = CurrencyFormatter.format(totalPaise, languageCode)
    }

    fun validateAndShowError(): Boolean {
        return if (viewModel.validateStep3()) {
            true
        } else {
            Toast.makeText(
                requireContext(),
                "Please declare at least one valid income stream.",
                Toast.LENGTH_LONG
            ).show()
            false
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
