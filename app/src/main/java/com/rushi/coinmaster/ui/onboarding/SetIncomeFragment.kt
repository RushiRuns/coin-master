package com.rushi.coinmaster.ui.onboarding

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.rushi.coinmaster.R
import com.rushi.coinmaster.databinding.FragmentSetIncomeBinding

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

        // Setup Income Input
        binding.etIncome.setText(viewModel.monthlyIncomeStr)
        binding.etIncome.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                viewModel.monthlyIncomeStr = s?.toString() ?: ""
                binding.tilIncome.error = null
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    fun validateAndShowError(): Boolean {
        val income = viewModel.monthlyIncomeStr.toDoubleOrNull()
        return if (income == null || income <= 0.0) {
            binding.tilIncome.error = getString(R.string.ob_error_income_invalid)
            false
        } else {
            binding.tilIncome.error = null
            true
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
