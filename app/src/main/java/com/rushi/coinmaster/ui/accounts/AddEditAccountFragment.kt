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

    private lateinit var accountTypesList: List<Pair<String, AccountType>>
    private lateinit var colorsList: List<Pair<String, String>>

    private var selectedType = AccountType.BANK_ACCOUNT
    private var selectedColorHex = "#4285F4"

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

        // Dynamically initialize localized dropdown lists
        accountTypesList = listOf(
            Pair(getString(R.string.acc_type_bank), AccountType.BANK_ACCOUNT),
            Pair(getString(R.string.acc_type_cash), AccountType.CASH),
            Pair(getString(R.string.acc_type_card), AccountType.CREDIT_CARD),
            Pair(getString(R.string.acc_type_invest), AccountType.INVESTMENTS)
        )

        binding.toolbar.setNavigationOnClickListener {
            findNavController().popBackStack()
        }

        colorsList = listOf(
            Pair(getString(R.string.color_green), "#0F9D58"),
            Pair(getString(R.string.color_blue), "#4285F4"),
            Pair(getString(R.string.color_red), "#DB4437"),
            Pair(getString(R.string.color_yellow), "#F4B400")
        )

        val accountId = args.accountId
        val isEditMode = accountId > 0L

        setupExposedDropdowns()

        if (isEditMode) {
            binding.tvTitle.text = getString(R.string.title_edit_account)
            // Hide opening balance fields in Edit Mode to enforce audit integrity
            binding.tvBalanceLabel.visibility = View.GONE
            binding.tilBalance.visibility = View.GONE
            
            // Populate form values from database
            viewLifecycleOwner.lifecycleScope.launch {
                val account = viewModel.getAccountById(accountId)
                if (account != null) {
                    binding.etAccountName.setText(account.name)
                    
                    selectedType = account.type
                    val typeName = accountTypesList.find { it.second == account.type }?.first ?: getString(R.string.acc_type_bank)
                    binding.actvAccountType.setText(typeName, false)
 
                    selectedColorHex = account.colorHex
                    val colorName = colorsList.find { it.second == account.colorHex }?.first ?: getString(R.string.color_blue)
                    binding.actvColor.setText(colorName, false)
                }
            }
        } else {
            binding.tvTitle.text = getString(R.string.title_add_account)
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
                    iconName = "ic_bank"
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

    }

    private fun validateForm(isEditMode: Boolean): Boolean {
        var isValid = true
        if (binding.etAccountName.text.toString().trim().isEmpty()) {
            binding.tilAccountName.error = getString(R.string.ob_error_name_empty)
            isValid = false
        } else {
            binding.tilAccountName.error = null
        }

        if (!isEditMode) {
            val balance = binding.etBalance.text.toString().trim().toDoubleOrNull()
            if (balance == null || balance < 0.0) {
                binding.tilBalance.error = getString(R.string.ob_error_balance_invalid)
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
