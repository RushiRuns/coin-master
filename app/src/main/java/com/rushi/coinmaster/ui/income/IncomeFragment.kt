package com.rushi.coinmaster.ui.income

import android.app.AlertDialog
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
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupWithNavController
import com.rushi.coinmaster.MainActivity
import com.rushi.coinmaster.R
import com.rushi.coinmaster.data.local.entity.AccountEntity
import com.rushi.coinmaster.databinding.FragmentIncomeBinding
import com.rushi.coinmaster.databinding.ItemOnboardingIncomeStreamBinding
import com.rushi.coinmaster.domain.model.IncomeStream
import com.rushi.coinmaster.util.CurrencyFormatter
import com.rushi.coinmaster.util.LocaleHelper
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class IncomeFragment : Fragment() {

    private var _binding: FragmentIncomeBinding? = null
    private val binding get() = _binding!!

    private val viewModel: IncomeViewModel by viewModels()
    private var accountsList: List<AccountEntity> = emptyList()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentIncomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Setup toolbar with navigation drawer support (hamburger icon)
        val navController = findNavController()
        val appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.homeFragment, R.id.transactionsFragment, R.id.budgetFragment,
                R.id.nav_categories, R.id.nav_expense_type, R.id.nav_income,
                R.id.nav_savings, R.id.nav_accounts, R.id.nav_notes,
                R.id.nav_transfers, R.id.nav_goals, R.id.nav_settings
            ),
            (requireActivity() as MainActivity).drawerLayout
        )
        binding.toolbar.setupWithNavController(navController, appBarConfiguration)

        binding.btnAddStream.setOnClickListener {
            addIncomeStreamFromInput()
        }

        // Observe accounts, income streams, and UI events
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.accounts.collectLatest { accounts ->
                        accountsList = accounts
                        val accountNames = accounts.map { it.name }
                        val sourceAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, accountNames)
                        binding.actvStreamAccount.setAdapter(sourceAdapter)

                        if (accountNames.isNotEmpty() && binding.actvStreamAccount.text.isEmpty()) {
                            binding.actvStreamAccount.setText(accountNames[0], false)
                        }

                        // Re-render streams list since accounts are now populated (needed to resolve names)
                        renderIncomeStreams(viewModel.incomeStreams.value)
                    }
                }
                launch {
                    viewModel.incomeStreams.collectLatest { streams ->
                        renderIncomeStreams(streams)
                    }
                }
                launch {
                    viewModel.totalIncomePaise.collectLatest { totalPaise ->
                        val langCode = LocaleHelper.getLanguage(requireContext())
                        binding.tvTotalIncome.text = CurrencyFormatter.format(totalPaise, langCode)
                    }
                }
                launch {
                    viewModel.uiEvent.collect { event ->
                        when (event) {
                            is IncomeUiEvent.ShowToast -> {
                                Toast.makeText(requireContext(), event.message, Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }
            }
        }
    }

    private fun addIncomeStreamFromInput() {
        val name = binding.etStreamName.text?.toString()?.trim() ?: ""
        val amountStr = binding.etStreamAmount.text?.toString()?.trim() ?: ""
        val selectedAccountName = binding.actvStreamAccount.text?.toString() ?: ""

        if (name.isEmpty()) {
            binding.tilStreamName.error = "Name cannot be empty"
            return
        }
        binding.tilStreamName.error = null

        val amount = amountStr.toDoubleOrNull()
        if (amount == null || amount <= 0.0) {
            binding.tilStreamAmount.error = "Enter a valid amount greater than 0"
            return
        }
        binding.tilStreamAmount.error = null

        val selectedAccount = accountsList.find { it.name == selectedAccountName }
        if (selectedAccount == null) {
            binding.tilStreamAccount.error = "Please select a valid account"
            return
        }
        binding.tilStreamAccount.error = null

        viewModel.addIncomeStream(name, amount, selectedAccount.id)

        binding.etStreamName.text = null
        binding.etStreamAmount.text = null
        // Do not reset account text to keep the selection helper active
    }

    private fun renderIncomeStreams(streams: List<IncomeStream>) {
        val langCode = LocaleHelper.getLanguage(requireContext())
        binding.containerIncomeStreams.removeAllViews()

        val count = streams.size
        binding.tvStreamCount.text = "$count income stream${if (count == 1) "" else "s"}"

        if (streams.isEmpty()) {
            binding.tvEmpty.visibility = View.VISIBLE
        } else {
            binding.tvEmpty.visibility = View.GONE
            for (stream in streams) {
                val itemBinding = ItemOnboardingIncomeStreamBinding.inflate(
                    layoutInflater,
                    binding.containerIncomeStreams,
                    false
                )
                itemBinding.tvStreamName.text = stream.name
                itemBinding.tvStreamAmount.text = CurrencyFormatter.format(stream.amountPaise, langCode) + " / month"

                val linkedAccount = accountsList.find { it.id == stream.accountId }
                if (linkedAccount != null) {
                    itemBinding.tvStreamAccount.text = "Deposits to: ${linkedAccount.name}"
                    itemBinding.tvStreamAccount.visibility = View.VISIBLE
                    itemBinding.btnDeposit.visibility = View.VISIBLE
                    itemBinding.btnDeposit.setOnClickListener {
                        viewModel.depositIncomeStream(stream)
                    }
                } else {
                    itemBinding.tvStreamAccount.text = "Deposits to: Unknown Account"
                    itemBinding.tvStreamAccount.visibility = View.VISIBLE
                    itemBinding.btnDeposit.visibility = View.GONE
                }

                itemBinding.btnDelete.setOnClickListener {
                    AlertDialog.Builder(requireContext())
                        .setTitle("Delete Income Stream")
                        .setMessage("Remove \"${stream.name}\" from your income sources?")
                        .setPositiveButton("Delete") { _, _ ->
                            viewModel.deleteIncomeStream(stream.id)
                        }
                        .setNegativeButton("Cancel", null)
                        .show()
                }

                binding.containerIncomeStreams.addView(itemBinding.root)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
