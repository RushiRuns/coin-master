package com.rushi.coinmaster.ui.transactions

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Filter
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
import com.rushi.coinmaster.data.local.entity.TransferRecipientEntity
import com.rushi.coinmaster.data.local.model.TransactionType
import com.rushi.coinmaster.databinding.FragmentAddTransactionBinding
import androidx.navigation.fragment.navArgs
import com.rushi.coinmaster.util.DateFormatter
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.util.Calendar

@AndroidEntryPoint
class AddTransactionFragment : Fragment() {

    private var _binding: FragmentAddTransactionBinding? = null
    private val binding get() = _binding!!

    private val viewModel: TransactionViewModel by viewModels()
    private val args: AddTransactionFragmentArgs by navArgs()

    private var selectedDateMillis: Long = System.currentTimeMillis()
    private var accountsList: List<AccountEntity> = emptyList()
    private var categoriesList: List<CategoryEntity> = emptyList()
    private var recipientsList: List<TransferRecipientEntity> = emptyList()

    private var editingTransaction: com.rushi.coinmaster.data.local.entity.TransactionEntity? = null
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

        binding.actvCategory.threshold = 1

        setupTypeDropdown()
        setupDatePicker()

        // If editing a transaction, trigger load
        if (args.transactionId != 0L) {
            viewModel.loadTransaction(args.transactionId)
        }

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

                        val editTx = editingTransaction
                        if (editTx != null) {
                            val sourceAcc = accounts.find { it.id == editTx.accountId }
                            if (sourceAcc != null) {
                                binding.actvAccount.setText(sourceAcc.name, false)
                            }
                            if (editTx.type == TransactionType.TRANSFER) {
                                val destAcc = accounts.find { it.id == editTx.transferToAccountId }
                                if (destAcc != null) {
                                    binding.actvTransferToAccount.setText(destAcc.name, false)
                                }
                            }
                        } else if (accountNames.isNotEmpty() && binding.actvAccount.text.isEmpty()) {
                            binding.actvAccount.setText(accountNames[0], false)
                        }
                    }
                }

                launch {
                    viewModel.categoriesState.collect { categories ->
                        categoriesList = categories
                        val categoryNames = categories.map { it.name }
                        val categoryAdapter = PrefixFilterAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, categoryNames)
                        binding.actvCategory.setAdapter(categoryAdapter)

                        val editTx = editingTransaction
                        if (editTx != null && editTx.type == TransactionType.EXPENSE) {
                            val cat = categories.find { it.id == editTx.categoryId }
                            if (cat != null) {
                                binding.actvCategory.setText(cat.name, false)
                            }
                        }
                    }
                }

                launch {
                    viewModel.recipientsState.collect { recipients ->
                        recipientsList = recipients
                        val recipientNames = recipients.map { it.name }
                        val recipientAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, recipientNames)
                        binding.actvRecipient.setAdapter(recipientAdapter)

                        val editTx = editingTransaction
                        if (editTx != null && editTx.type == TransactionType.EXTERNAL_TRANSFER) {
                            val rec = recipients.find { it.id == editTx.transferRecipientId }
                            if (rec != null) {
                                binding.actvRecipient.setText(rec.name, false)
                            }
                        } else if (recipientNames.isNotEmpty() && binding.actvRecipient.text.isEmpty()) {
                            binding.actvRecipient.setText(recipientNames[0], false)
                        }
                    }
                }

                launch {
                    viewModel.transactionToEdit.collect { transaction ->
                        if (transaction != null) {
                            editingTransaction = transaction
                            
                            binding.toolbar.title = "Edit Transaction"
                            binding.btnSave.text = "Save Changes"

                            binding.etAmount.setText(String.format(java.util.Locale.US, "%.2f", transaction.amountPaise / 100.0))
                            selectedDateMillis = transaction.date
                            binding.etDate.setText(DateFormatter.formatDate(selectedDateMillis))
                            binding.etNote.setText(transaction.note ?: "")

                            val typeString = when (transaction.type) {
                                TransactionType.EXPENSE -> getString(R.string.type_expense)
                                TransactionType.INCOME -> getString(R.string.type_income)
                                TransactionType.TRANSFER -> getString(R.string.type_transfer)
                                TransactionType.EXTERNAL_TRANSFER -> getString(R.string.type_external_transfer)
                                TransactionType.BALANCE_CORRECTION -> getString(R.string.type_expense) // fallback
                            }
                            binding.actvType.setText(typeString, false)
                            updateDropdownVisibility(typeString)

                            // Trigger re-population of spinner lists to bind correct selections
                            val accs = accountsList
                            if (accs.isNotEmpty()) {
                                val sourceAcc = accs.find { it.id == transaction.accountId }
                                if (sourceAcc != null) {
                                    binding.actvAccount.setText(sourceAcc.name, false)
                                }
                                if (transaction.type == TransactionType.TRANSFER) {
                                    val destAcc = accs.find { it.id == transaction.transferToAccountId }
                                    if (destAcc != null) {
                                        binding.actvTransferToAccount.setText(destAcc.name, false)
                                    }
                                }
                            }

                            val cats = categoriesList
                            if (cats.isNotEmpty() && transaction.type == TransactionType.EXPENSE) {
                                val cat = cats.find { it.id == transaction.categoryId }
                                if (cat != null) {
                                    binding.actvCategory.setText(cat.name, false)
                                }
                            }

                            val recs = recipientsList
                            if (recs.isNotEmpty() && transaction.type == TransactionType.EXTERNAL_TRANSFER) {
                                val rec = recs.find { it.id == transaction.transferRecipientId }
                                if (rec != null) {
                                    binding.actvRecipient.setText(rec.name, false)
                                }
                            }
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
            getString(R.string.type_transfer),
            getString(R.string.type_external_transfer)
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
                binding.tilRecipient.visibility = View.GONE
            }
            getString(R.string.type_income) -> {
                binding.tilCategory.visibility = View.GONE
                binding.tilTransferToAccount.visibility = View.GONE
                binding.tilRecipient.visibility = View.GONE
            }
            getString(R.string.type_transfer) -> {
                binding.tilCategory.visibility = View.GONE
                binding.tilTransferToAccount.visibility = View.VISIBLE
                binding.tilRecipient.visibility = View.GONE
            }
            getString(R.string.type_external_transfer) -> {
                binding.tilCategory.visibility = View.GONE
                binding.tilTransferToAccount.visibility = View.GONE
                binding.tilRecipient.visibility = View.VISIBLE
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
            getString(R.string.type_external_transfer) -> TransactionType.EXTERNAL_TRANSFER
            else -> TransactionType.EXPENSE
        }

        val selectedSourceName = binding.actvAccount.text.toString()
        val sourceAccountId = accountsList.find { it.name == selectedSourceName }?.id ?: 0L

        val selectedDestName = binding.actvTransferToAccount.text.toString()
        val destAccountId = if (type == TransactionType.TRANSFER) {
            accountsList.find { it.name == selectedDestName }?.id
        } else null

        val selectedRecipientName = binding.actvRecipient.text.toString()
        val recipientId = if (type == TransactionType.EXTERNAL_TRANSFER) {
            recipientsList.find { it.name == selectedRecipientName }?.id
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
            transferRecipientId = recipientId,
            categoryId = categoryId,
            date = selectedDateMillis,
            note = note,
            editingTransactionId = args.transactionId
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

class PrefixFilterAdapter(
    context: android.content.Context,
    resource: Int,
    private val originalItems: List<String>
) : ArrayAdapter<String>(context, resource, ArrayList(originalItems)) {

    private var filteredItems: List<String> = originalItems

    override fun getCount(): Int = filteredItems.size

    override fun getItem(position: Int): String? {
        return if (position in filteredItems.indices) filteredItems[position] else null
    }

    override fun getFilter(): Filter {
        return object : Filter() {
            override fun performFiltering(constraint: CharSequence?): FilterResults {
                val results = FilterResults()
                val prefix = constraint?.toString()?.trim() ?: ""
                val filtered = if (prefix.isEmpty()) {
                    originalItems
                } else {
                    originalItems.filter {
                        it.startsWith(prefix, ignoreCase = true)
                    }
                }
                results.values = filtered
                results.count = filtered.size
                return results
            }

            @Suppress("UNCHECKED_CAST")
            override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
                filteredItems = (results?.values as? List<String>) ?: originalItems
                clear()
                addAll(filteredItems)
                notifyDataSetChanged()
            }
        }
    }
}
