package com.rushi.coinmaster.ui.accounts

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.rushi.coinmaster.R
import com.rushi.coinmaster.data.local.model.AccountType
import com.rushi.coinmaster.databinding.FragmentAddEditAccountBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class AddEditAccountFragment : Fragment() {

    private var _binding: FragmentAddEditAccountBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AccountsViewModel by viewModels()
    private val args: AddEditAccountFragmentArgs by navArgs()

    private val accountTypesList = listOf(
        Pair("Bank Account", AccountType.BANK_ACCOUNT),
        Pair("Cash", AccountType.CASH),
        Pair("Credit Card", AccountType.CREDIT_CARD),
        Pair("Investments", AccountType.INVESTMENTS)
    )

    private val colorsList = listOf(
        Pair("Green", "#0F9D58"),
        Pair("Blue", "#4285F4"),
        Pair("Red", "#DB4437"),
        Pair("Yellow", "#F4B400")
    )

    private val iconsList = listOf(
        Pair("Bank", "ic_bank"),
        Pair("Wallet", "ic_cash"),
        Pair("Card", "ic_card"),
        Pair("SIP/Invest", "ic_invest")
    )

    private var selectedType = AccountType.BANK_ACCOUNT
    private var selectedColorHex = "#4285F4"
    private var selectedIconName = "ic_bank"

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAddEditAccountBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val accountId = args.accountId
        val isEditMode = accountId > 0L

        setupExposedDropdowns()

        if (isEditMode) {
            binding.tvTitle.text = "Edit Account"
            // Hide opening balance fields in Edit Mode to enforce audit integrity
            binding.tvBalanceLabel.visibility = View.GONE
            binding.tilBalance.visibility = View.GONE
            
            // Populate form values from database
            viewLifecycleOwner.lifecycleScope.launch {
                val account = viewModel.getAccountById(accountId)
                if (account != null) {
                    binding.etAccountName.setText(account.name)
                    
                    selectedType = account.type
                    val typeName = accountTypesList.find { it.second == account.type }?.first ?: "Bank Account"
                    binding.actvAccountType.setText(typeName, false)

                    selectedColorHex = account.colorHex
                    val colorName = colorsList.find { it.second == account.colorHex }?.first ?: "Blue"
                    binding.actvColor.setText(colorName, false)

                    selectedIconName = account.iconName
                    val iconName = iconsList.find { it.second == account.iconName }?.first ?: "Bank"
                    binding.actvIcon.setText(iconName, false)
                }
            }
        } else {
            binding.tvTitle.text = "Add Account"
            binding.tvBalanceLabel.visibility = View.VISIBLE
            binding.tilBalance.visibility = View.VISIBLE
        }

        binding.btnSave.setOnClickListener {
            if (validateForm(isEditMode)) {
                val name = binding.etAccountName.text.toString().trim()
                val balanceStr = if (isEditMode) "0" else binding.etBalance.text.toString().trim()

                viewModel.saveAccount(
                    id = accountId,
                    name = name,
                    type = selectedType,
                    balanceStr = balanceStr,
                    colorHex = selectedColorHex,
                    iconName = selectedIconName
                )
                findNavController().popBackStack()
            }
        }
    }

    private fun setupExposedDropdowns() {
        // 1. Account Types
        val typeNames = accountTypesList.map { it.first }.toTypedArray()
        val typeAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, typeNames)
        binding.actvAccountType.setAdapter(typeAdapter)
        binding.actvAccountType.setOnItemClickListener { _, _, position, _ ->
            selectedType = accountTypesList[position].second
        }

        // 2. Colors
        val colorNames = colorsList.map { it.first }.toTypedArray()
        val colorAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, colorNames)
        binding.actvColor.setAdapter(colorAdapter)
        binding.actvColor.setOnItemClickListener { _, _, position, _ ->
            selectedColorHex = colorsList[position].second
        }

        // 3. Icons
        val iconNames = iconsList.map { it.first }.toTypedArray()
        val iconAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, iconNames)
        binding.actvIcon.setAdapter(iconAdapter)
        binding.actvIcon.setOnItemClickListener { _, _, position, _ ->
            selectedIconName = iconsList[position].second
        }
    }

    private fun validateForm(isEditMode: Boolean): Boolean {
        var isValid = true
        if (binding.etAccountName.text.toString().trim().isEmpty()) {
            binding.tilAccountName.error = "Name cannot be empty"
            isValid = false
        } else {
            binding.tilAccountName.error = null
        }

        if (!isEditMode) {
            val balance = binding.etBalance.text.toString().trim().toDoubleOrNull()
            if (balance == null || balance < 0.0) {
                binding.tilBalance.error = "Please enter a valid positive opening balance"
                isValid = false
            } else {
                binding.tilBalance.error = null
            }
        }

        return isValid
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
