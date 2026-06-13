package com.rushi.coinmaster.ui.transactions

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.rushi.coinmaster.R
import com.rushi.coinmaster.data.local.entity.AccountEntity
import com.rushi.coinmaster.data.local.entity.CategoryEntity
import com.rushi.coinmaster.data.local.model.TransactionType
import com.rushi.coinmaster.databinding.FragmentAddTransactionBinding
import com.rushi.coinmaster.util.DateFormatter
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.util.Calendar

@AndroidEntryPoint
class AddTransactionFragment : Fragment() {

    private var _binding: FragmentAddTransactionBinding? = null
    private val binding get() = _binding!!

    private val viewModel: TransactionViewModel by viewModels()

    private var selectedDateMillis: Long = System.currentTimeMillis()
    private var accountsList: List<AccountEntity> = emptyList()
    private var categoriesList: List<CategoryEntity> = emptyList()

    private lateinit var types: List<String>

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAddTransactionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.toolbar.setNavigationOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }

        setupTypeDropdown()
        setupDatePicker()

        // Observe Accounts and Categories
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.accountsState.collect { accounts ->
                        accountsList = accounts
                        val accountNames = accounts.map { it.name }
                        val sourceAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, accountNames)
                        binding.actvAccount.setAdapter(sourceAdapter)
                        binding.actvTransferToAccount.setAdapter(sourceAdapter)

                        if (accountNames.isNotEmpty() && binding.actvAccount.text.isEmpty()) {
                            binding.actvAccount.setText(accountNames[0], false)
                        }
                    }
                }

                launch {
                    viewModel.categoriesState.collect { categories ->
                        categoriesList = categories
                        val categoryNames = categories.map { it.name }
                        val categoryAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, categoryNames)
                        binding.actvCategory.setAdapter(categoryAdapter)

                        if (categoryNames.isNotEmpty() && binding.actvCategory.text.isEmpty()) {
                            binding.actvCategory.setText(categoryNames[0], false)
                        }
                    }
                }

                launch {
                    viewModel.uiEvent.collect { event ->
                        when (event) {
                            is TransactionUiEvent.Success -> {
                                Toast.makeText(requireContext(), getString(R.string.text_transaction_success), Toast.LENGTH_SHORT).show()
                                findNavController().popBackStack()
                            }
                            is TransactionUiEvent.Error -> {
                                Toast.makeText(requireContext(), event.message, Toast.LENGTH_LONG).show()
                            }
                        }
                    }
                }
            }
        }

        binding.btnSave.setOnClickListener {
            saveTransaction()
        }

        binding.btnCancel.setOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }
    }

    private fun setupTypeDropdown() {
        types = listOf(
            getString(R.string.type_expense),
            getString(R.string.type_income),
            getString(R.string.type_transfer)
        )
        val typeAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, types)
        binding.actvType.setAdapter(typeAdapter)
        binding.actvType.setText(getString(R.string.type_expense), false)
        updateDropdownVisibility(getString(R.string.type_expense))

        binding.actvType.setOnItemClickListener { _, _, position, _ ->
            val selected = types[position]
            updateDropdownVisibility(selected)
        }
    }

    private fun updateDropdownVisibility(typeStr: String) {
        when (typeStr) {
            getString(R.string.type_expense) -> {
                binding.tilCategory.visibility = View.VISIBLE
                binding.tilTransferToAccount.visibility = View.GONE
            }
            getString(R.string.type_income) -> {
                binding.tilCategory.visibility = View.GONE
                binding.tilTransferToAccount.visibility = View.GONE
            }
            getString(R.string.type_transfer) -> {
                binding.tilCategory.visibility = View.GONE
                binding.tilTransferToAccount.visibility = View.VISIBLE
            }
        }
    }

    private fun setupDatePicker() {
        binding.etDate.setText(DateFormatter.formatDate(selectedDateMillis))
        binding.etDate.setOnClickListener {
            showDatePicker()
        }
    }

    private fun showDatePicker() {
        val calendar = Calendar.getInstance().apply { timeInMillis = selectedDateMillis }
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        android.app.DatePickerDialog(
            requireContext(),
            { _, y, m, d ->
                val selectedCalendar = Calendar.getInstance().apply {
                    set(Calendar.YEAR, y)
                    set(Calendar.MONTH, m)
                    set(Calendar.DAY_OF_MONTH, d)
                }
                selectedDateMillis = selectedCalendar.timeInMillis
                binding.etDate.setText(DateFormatter.formatDate(selectedDateMillis))
            },
            year,
            month,
            day
        ).show()
    }

    private fun saveTransaction() {
        val amountStr = binding.etAmount.text.toString()
        val typeStr = binding.actvType.text.toString()
        val type = when (typeStr) {
            getString(R.string.type_expense) -> TransactionType.EXPENSE
            getString(R.string.type_income) -> TransactionType.INCOME
            getString(R.string.type_transfer) -> TransactionType.TRANSFER
            else -> TransactionType.EXPENSE
        }

        val selectedSourceName = binding.actvAccount.text.toString()
        val sourceAccountId = accountsList.find { it.name == selectedSourceName }?.id ?: 0L

        val selectedDestName = binding.actvTransferToAccount.text.toString()
        val destAccountId = if (type == TransactionType.TRANSFER) {
            accountsList.find { it.name == selectedDestName }?.id
        } else null

        val selectedCategoryName = binding.actvCategory.text.toString()
        val categoryId = if (type == TransactionType.EXPENSE) {
            categoriesList.find { it.name == selectedCategoryName }?.id
        } else null

        val note = binding.etNote.text.toString()

        viewModel.saveTransaction(
            amountStr = amountStr,
            type = type,
            accountId = sourceAccountId,
            transferToAccountId = destAccountId,
            categoryId = categoryId,
            date = selectedDateMillis,
            note = note
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
