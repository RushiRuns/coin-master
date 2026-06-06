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
import com.rushi.coinmaster.data.local.model.AccountType
import com.rushi.coinmaster.databinding.FragmentAddFirstAccountBinding

class AddFirstAccountFragment : Fragment() {

    private var _binding: FragmentAddFirstAccountBinding? = null
    private val binding get() = _binding!!

    private val viewModel: OnboardingViewModel by activityViewModels()

    // Mapping displayed types to Enum
    private val accountTypesList = listOf(
        Pair("Bank Account", AccountType.BANK_ACCOUNT),
        Pair("Cash", AccountType.CASH),
        Pair("Credit Card", AccountType.CREDIT_CARD),
        Pair("Investments", AccountType.INVESTMENTS)
    )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAddFirstAccountBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Setup Account Name
        binding.etAccountName.setText(viewModel.accountName)
        binding.etAccountName.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                viewModel.accountName = s?.toString() ?: ""
                binding.tilAccountName.error = null
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        // Setup Account Type dropdown selection
        val displayNames = accountTypesList.map { it.first }.toTypedArray()
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, displayNames)
        binding.actvAccountType.setAdapter(adapter)

        val initialTypeString = accountTypesList.find { it.second == viewModel.accountType }?.first ?: "Bank Account"
        binding.actvAccountType.setText(initialTypeString, false)
        binding.actvAccountType.setOnItemClickListener { _, _, position, _ ->
            viewModel.accountType = accountTypesList[position].second
        }

        // Setup Opening Balance
        binding.etBalance.setText(viewModel.accountBalanceStr)
        binding.etBalance.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                viewModel.accountBalanceStr = s?.toString() ?: ""
                binding.tilBalance.error = null
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    fun validateAndShowError(): Boolean {
        var isValid = true

        if (viewModel.accountName.trim().isEmpty()) {
            binding.tilAccountName.error = getString(R.string.ob_error_acc_name_empty)
            isValid = false
        } else {
            binding.tilAccountName.error = null
        }

        val balance = viewModel.accountBalanceStr.toDoubleOrNull()
        if (balance == null || balance < 0.0) {
            binding.tilBalance.error = getString(R.string.ob_error_balance_invalid)
            isValid = false
        } else {
            binding.tilBalance.error = null
        }

        return isValid
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
