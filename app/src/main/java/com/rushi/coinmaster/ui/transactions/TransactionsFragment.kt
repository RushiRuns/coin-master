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

        setupRecyclerView()
        setupTabLayout()
        setupFilters()
        setupDatePicker()
        setupFAB()

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
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
        }
    }

    private fun setupRecyclerView() {
        adapter = RecentTransactionsAdapter()
        binding.rvTransactions.layoutManager = LinearLayoutManager(requireContext())
        binding.rvTransactions.adapter = adapter
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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
