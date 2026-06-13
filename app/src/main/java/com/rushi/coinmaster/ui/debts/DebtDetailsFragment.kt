package com.rushi.coinmaster.ui.debts

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.rushi.coinmaster.R
import com.rushi.coinmaster.data.local.entity.AccountEntity
import com.rushi.coinmaster.data.local.model.DebtType
import com.rushi.coinmaster.databinding.FragmentDebtDetailsBinding
import com.rushi.coinmaster.ui.home.RecentTransactionsAdapter
import com.rushi.coinmaster.util.CurrencyFormatter
import com.rushi.coinmaster.util.DateFormatter
import com.rushi.coinmaster.util.LocaleHelper
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class DebtDetailsFragment : Fragment() {

    private var _binding: FragmentDebtDetailsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: DebtDetailsViewModel by viewModels()
    private val args: DebtDetailsFragmentArgs by navArgs()

    private lateinit var transactionsAdapter: RecentTransactionsAdapter
    private var accountsList: List<AccountEntity> = emptyList()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDebtDetailsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.loadDebt(args.debtId)

        setupToolbar()
        setupRecyclerView()

        binding.btnRecordRepayment.setOnClickListener {
            showRepaymentDialog()
        }

        // Observe ViewModel Flows
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                // Observe Debt State
                launch {
                    viewModel.debtState.collect { debt ->
                        if (debt != null) {
                            val languageCode = LocaleHelper.getLanguage(requireContext())
                            binding.tvPersonName.text = debt.personName
                            binding.tvRemainingAmount.text = CurrencyFormatter.format(debt.remainingPaise, languageCode)
                            binding.tvTotalAmount.text = CurrencyFormatter.format(debt.amountPaise, languageCode)

                            if (debt.type == DebtType.LENT) {
                                binding.tvDebtType.text = "LENT (They owe you)"
                                binding.tvDebtType.setTextColor(ContextCompat.getColor(requireContext(), R.color.color_positive))
                            } else {
                                binding.tvDebtType.text = "BORROWED (You owe them)"
                                binding.tvDebtType.setTextColor(ContextCompat.getColor(requireContext(), R.color.color_negative))
                            }

                            if (debt.dueDate != null) {
                                binding.tvDueDate.visibility = View.VISIBLE
                                binding.tvDueDate.text = "Due Date: ${DateFormatter.formatDate(debt.dueDate, languageCode)}"
                            } else {
                                binding.tvDueDate.visibility = View.GONE
                            }

                            if (!debt.note.isNullOrBlank()) {
                                binding.tvNote.visibility = View.VISIBLE
                                binding.tvNote.text = "Note: ${debt.note}"
                            } else {
                                binding.tvNote.visibility = View.GONE
                            }

                            if (debt.isSettled) {
                                binding.btnRecordRepayment.visibility = View.GONE
                            } else {
                                binding.btnRecordRepayment.visibility = View.VISIBLE
                            }
                        }
                    }
                }

                // Observe Accounts List
                launch {
                    viewModel.accountsState.collect { accounts ->
                        accountsList = accounts
                    }
                }

                // Observe History Transactions List
                launch {
                    viewModel.transactionsState.collect { transactions ->
                        if (transactions.isNotEmpty()) {
                            binding.rvDebtTransactions.visibility = View.VISIBLE
                            binding.tvNoHistory.visibility = View.GONE
                            transactionsAdapter.submitList(transactions)
                        } else {
                            binding.rvDebtTransactions.visibility = View.GONE
                            binding.tvNoHistory.visibility = View.VISIBLE
                        }
                    }
                }

                // Observe Events
                launch {
                    viewModel.eventFlow.collect { event ->
                        when (event) {
                            is DebtDetailsEvent.Success -> {
                                Toast.makeText(requireContext(), "Payment recorded successfully", Toast.LENGTH_SHORT).show()
                            }
                            is DebtDetailsEvent.DebtDeleted -> {
                                Toast.makeText(requireContext(), "Debt deleted successfully", Toast.LENGTH_SHORT).show()
                                findNavController().popBackStack()
                            }
                            is DebtDetailsEvent.Error -> {
                                Toast.makeText(requireContext(), event.message, Toast.LENGTH_LONG).show()
                            }
                        }
                    }
                }
            }
        }
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }
        binding.toolbar.inflateMenu(R.menu.menu_debt_details)
        binding.toolbar.setOnMenuItemClickListener { menuItem ->
            if (menuItem.itemId == R.id.action_delete) {
                showDeleteConfirmationDialog()
                true
            } else {
                false
            }
        }
    }

    private fun setupRecyclerView() {
        transactionsAdapter = RecentTransactionsAdapter()
        binding.rvDebtTransactions.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = transactionsAdapter
            isNestedScrollingEnabled = false // NestedScrollView handles scrolling
        }
    }

    private fun showDeleteConfirmationDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Delete Loan?")
            .setMessage("Are you sure you want to delete this record? This will unlink it from its transactions, but will not delete the associated transactions.")
            .setPositiveButton("Delete") { _, _ ->
                viewModel.deleteDebt()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showRepaymentDialog() {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_record_repayment, null)
        val etAmount = dialogView.findViewById<TextInputEditText>(R.id.et_amount)
        val actvAccount = dialogView.findViewById<AutoCompleteTextView>(R.id.actv_account)
        val etNote = dialogView.findViewById<TextInputEditText>(R.id.et_note)

        // Populate accounts autocomplete dropdown inside dialog
        val accountNames = accountsList.map { it.name }
        val accountAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, accountNames)
        actvAccount.setAdapter(accountAdapter)
        if (accountNames.isNotEmpty()) {
            actvAccount.setText(accountNames[0], false)
        }

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Record Repayment")
            .setView(dialogView)
            .setPositiveButton("Save") { _, _ ->
                val amountStr = etAmount.text.toString()
                val selectedAccount = actvAccount.text.toString()
                val accountId = accountsList.find { it.name == selectedAccount }?.id ?: 0L
                val note = etNote.text.toString()

                viewModel.recordRepayment(
                    amountStr = amountStr,
                    accountId = accountId,
                    note = note
                )
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
