package com.rushi.coinmaster.ui.transactions

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.tabs.TabLayout
import com.rushi.coinmaster.R
import com.rushi.coinmaster.databinding.FragmentTransactionsBinding
import com.rushi.coinmaster.ui.home.RecentTransactionsAdapter
import com.rushi.coinmaster.util.DateFormatter
import com.rushi.coinmaster.util.CurrencyFormatter
import com.rushi.coinmaster.util.LocaleHelper
import com.rushi.coinmaster.MainActivity
import androidx.navigation.ui.setupWithNavController
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.util.Calendar

@AndroidEntryPoint
class TransactionsFragment : Fragment() {

    private var _binding: FragmentTransactionsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: TransactionsListViewModel by viewModels()
    private lateinit var adapter: RecentTransactionsAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTransactionsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val navController = findNavController()
        val appBarConfiguration = androidx.navigation.ui.AppBarConfiguration(
            setOf(R.id.homeFragment, R.id.transactionsFragment, R.id.budgetFragment),
            (requireActivity() as MainActivity).drawerLayout
        )
        binding.toolbar.setupWithNavController(navController, appBarConfiguration)
        setupSearch()

        setupRecyclerView()
        setupTabLayout()
        setupFilters()
        setupDatePicker()
        setupFAB()

        // Read navigation arguments for deep-link filtering
        val envelopeIds = arguments?.getLongArray("envelopeIds")
        val startMillis = arguments?.getLong("startMillis", -1L) ?: -1L
        val endMillis = arguments?.getLong("endMillis", -1L) ?: -1L
        val filterLabel = arguments?.getString("filterLabel") ?: ""

        if (envelopeIds != null && startMillis != -1L && endMillis != -1L) {
            viewModel.applyDeepLinkFilter(
                TransactionsListViewModel.DeepLinkFilter(
                    categoryIds = envelopeIds.toList(),
                    startMillis = startMillis,
                    endMillis = endMillis,
                    displayLabel = filterLabel
                )
            )
        }

        binding.btnClearFilter.setOnClickListener {
            viewModel.clearDeepLinkFilter()
            arguments?.clear()
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.uiState.collect { uiState ->
                        val languageCode = LocaleHelper.getLanguage(requireContext())

                        // Format Date Selector button text
                        binding.btnSelectDate.text = getRelativeDateString(uiState.selectedDateMillis)

                        adapter.submitList(uiState.transactions)

                        if (uiState.transactions.isEmpty()) {
                            binding.tvEmptyState.visibility = View.VISIBLE
                            binding.rvTransactions.visibility = View.GONE
                        } else {
                            binding.tvEmptyState.visibility = View.GONE
                            binding.rvTransactions.visibility = View.VISIBLE
                        }
                    }
                }

                launch {
                    viewModel.spendingSummary.collect { summary ->
                        val languageCode = LocaleHelper.getLanguage(requireContext())
                        binding.tvTotalToday.text = CurrencyFormatter.format(summary.todayPaise, languageCode)
                        binding.tvTotalWeek.text = CurrencyFormatter.format(summary.weeklyPaise, languageCode)
                        binding.tvTotalMonth.text = CurrencyFormatter.format(summary.monthlyPaise, languageCode)
                    }
                }

                launch {
                    viewModel.deepLinkFilter.collect { deepLink ->
                        if (deepLink != null) {
                            binding.cardActiveFilter.visibility = View.VISIBLE
                            binding.tvActiveFilterLabel.text = "Filtered: ${deepLink.displayLabel}"
                            binding.cgFilters.visibility = View.GONE
                            binding.layoutDateSelector.visibility = View.GONE
                        } else {
                            binding.cardActiveFilter.visibility = View.GONE
                            if (viewModel.activeTab.value == 0) {
                                binding.cgFilters.visibility = View.VISIBLE
                                binding.layoutDateSelector.visibility = View.GONE
                            } else {
                                binding.cgFilters.visibility = View.GONE
                                binding.layoutDateSelector.visibility = View.VISIBLE
                            }
                        }
                    }
                }
            }
        }
    }

    private fun setupRecyclerView() {
        adapter = RecentTransactionsAdapter()
        adapter.onItemLongClick = { transaction ->
            showTransactionOptions(transaction)
        }
        binding.rvTransactions.layoutManager = LinearLayoutManager(requireContext())
        binding.rvTransactions.adapter = adapter

    }

    private fun showTransactionOptions(transaction: com.rushi.coinmaster.ui.home.TransactionDisplayItem) {
        val options = arrayOf("Edit", "Delete")
        android.app.AlertDialog.Builder(requireContext())
            .setTitle("Transaction Options")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> navigateToEditTransaction(transaction.id)
                    1 -> showDeleteConfirmationDialog(transaction)
                }
            }
            .show()
    }

    private fun navigateToEditTransaction(transactionId: Long) {
        val bundle = Bundle().apply {
            putLong("transactionId", transactionId)
        }
        findNavController().navigate(R.id.addTransactionFragment, bundle)
    }

    private fun showDeleteConfirmationDialog(
        transaction: com.rushi.coinmaster.ui.home.TransactionDisplayItem
    ) {
        android.app.AlertDialog.Builder(requireContext())
            .setTitle("Delete Transaction")
            .setMessage("Are you sure you want to delete this transaction?")
            .setPositiveButton("Delete") { _, _ ->
                viewModel.deleteTransaction(transaction.id)
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private fun setupTabLayout() {
        binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                val position = tab?.position ?: 0
                viewModel.setTab(position)
                if (position == 0) {
                    binding.cgFilters.visibility = View.VISIBLE
                    binding.layoutDateSelector.visibility = View.GONE
                } else {
                    binding.cgFilters.visibility = View.GONE
                    binding.layoutDateSelector.visibility = View.VISIBLE
                }
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
    }

    private fun setupFilters() {
        binding.cgFilters.setOnCheckedStateChangeListener { _, checkedIds ->
            when (checkedIds.firstOrNull()) {
                R.id.chip_day -> viewModel.setFilter(TransactionFilter.DAY)
                R.id.chip_week -> viewModel.setFilter(TransactionFilter.WEEK)
                R.id.chip_month -> viewModel.setFilter(TransactionFilter.MONTH)
            }
        }
    }

    private fun setupDatePicker() {
        binding.btnSelectDate.setOnClickListener {
            val calendar = Calendar.getInstance()
            calendar.timeInMillis = viewModel.uiState.value.selectedDateMillis

            DatePickerDialog(
                requireContext(),
                { _, year, month, dayOfMonth ->
                    val selectedCal = Calendar.getInstance().apply {
                        set(Calendar.YEAR, year)
                        set(Calendar.MONTH, month)
                        set(Calendar.DAY_OF_MONTH, dayOfMonth)
                    }
                    viewModel.setDate(selectedCal.timeInMillis)
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            ).show()
        }
    }

    private fun setupFAB() {
        binding.fabAddTransaction.setOnClickListener {
            findNavController().navigate(R.id.addTransactionFragment)
        }
    }

    private fun getRelativeDateString(dateMillis: Long): String {
        val languageCode = LocaleHelper.getLanguage(requireContext())
        val today = Calendar.getInstance()
        val target = Calendar.getInstance().apply { timeInMillis = dateMillis }

        val isToday = today.get(Calendar.YEAR) == target.get(Calendar.YEAR) &&
                today.get(Calendar.DAY_OF_YEAR) == target.get(Calendar.DAY_OF_YEAR)

        val yesterday = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }
        val isYesterday = yesterday.get(Calendar.YEAR) == target.get(Calendar.YEAR) &&
                yesterday.get(Calendar.DAY_OF_YEAR) == target.get(Calendar.DAY_OF_YEAR)

        return when {
            isToday -> getString(R.string.text_today)
            isYesterday -> getString(R.string.text_yesterday)
            else -> DateFormatter.formatDate(dateMillis, languageCode)
        }
    }

    private fun setupSearch() {
        binding.toolbar.inflateMenu(R.menu.menu_transactions)
        val searchItem = binding.toolbar.menu.findItem(R.id.action_search)
        val searchView = searchItem?.actionView as? androidx.appcompat.widget.SearchView

        searchView?.setOnQueryTextListener(object : androidx.appcompat.widget.SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                viewModel.setSearchQuery(newText.orEmpty())
                return true
            }
        })

        searchItem?.setOnActionExpandListener(object : android.view.MenuItem.OnActionExpandListener {
            override fun onMenuItemActionExpand(item: android.view.MenuItem): Boolean {
                return true
            }

            override fun onMenuItemActionCollapse(item: android.view.MenuItem): Boolean {
                viewModel.setSearchQuery("")
                return true
            }
        })
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
