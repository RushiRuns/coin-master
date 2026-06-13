package com.rushi.coinmaster.ui.home

import android.graphics.Color
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
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.listener.OnChartValueSelectedListener
import com.rushi.coinmaster.R
import com.rushi.coinmaster.data.local.model.EnvelopeWithAllocation
import com.rushi.coinmaster.databinding.FragmentHomeBinding
import com.rushi.coinmaster.util.CurrencyFormatter
import com.rushi.coinmaster.util.LocaleHelper
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val viewModel: HomeViewModel by viewModels()

    private lateinit var accountsAdapter: AccountsHorizontalAdapter
    private lateinit var transactionsAdapter: RecentTransactionsAdapter

    /** Guards the one-shot entry animation for the pie chart. */
    private var isFirstChartLoad = true

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerViews()
        setupPieChart()
        setupFAB()

        binding.cardDebts.setOnClickListener {
            val action = HomeFragmentDirections.actionHomeFragmentToDebtsFragment()
            findNavController().navigate(action)
        }

        // Observe ViewModel State
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { uiState ->
                    val languageCode = LocaleHelper.getLanguage(requireContext())

                    // 1. Net Worth
                    binding.tvNetWorth.text = CurrencyFormatter.format(uiState.netWorth, languageCode)

                    // Debts & Loans Summary
                    binding.tvOwedToYou.text = CurrencyFormatter.format(uiState.owedToYouPaise, languageCode)
                    binding.tvYouOwe.text = CurrencyFormatter.format(uiState.youOwePaise, languageCode)

                    // 2. Accounts list
                    accountsAdapter.submitList(uiState.accounts)

                    // 3. Budget Health
                    if (uiState.totalBudgetedPaise > 0L) {
                        val progressPercent = (uiState.totalSpentPaise * 100 / uiState.totalBudgetedPaise).toInt()
                        binding.pbBudgetHealth.progress = progressPercent.coerceAtMost(100)
                        
                        val spentStr = CurrencyFormatter.format(uiState.totalSpentPaise, languageCode)
                        val budgetedStr = CurrencyFormatter.format(uiState.totalBudgetedPaise, languageCode)
                        binding.tvBudgetHealthSummary.text = getString(
                            R.string.text_budget_health_summary,
                            spentStr,
                            budgetedStr
                        )
                    } else {
                        binding.pbBudgetHealth.progress = 0
                        binding.tvBudgetHealthSummary.text = getString(R.string.text_budget_health_no_budget)
                    }

                    // 4. Pie Chart Category Breakdown
                    val spentEnvelopes = uiState.envelopes.filter { it.spentAmountPaise > 0L }
                    if (spentEnvelopes.isNotEmpty()) {
                        binding.pieChart.visibility = View.VISIBLE
                        binding.cardCategoryDetail.visibility = View.VISIBLE
                        updatePieChartData(spentEnvelopes)
                    } else {
                        binding.pieChart.visibility = View.GONE
                        binding.cardCategoryDetail.visibility = View.GONE
                    }

                    // 5. Category selection details
                    val selectedDetail = uiState.selectedCategoryDetail
                    if (selectedDetail != null) {
                        val spentStr = CurrencyFormatter.format(selectedDetail.spentAmountPaise, languageCode)
                        val budgetedStr = CurrencyFormatter.format(selectedDetail.allocatedAmountPaise, languageCode)
                        binding.tvCategoryDetail.text = getString(
                            R.string.text_chart_details,
                            selectedDetail.categoryName,
                            spentStr,
                            budgetedStr
                        )
                        binding.tvCategoryDetail.setTypeface(null, android.graphics.Typeface.NORMAL)
                    } else {
                        binding.tvCategoryDetail.setText(R.string.text_chart_placeholder)
                        binding.tvCategoryDetail.setTypeface(null, android.graphics.Typeface.ITALIC)
                    }

                    // 6. Recent Transactions
                    if (uiState.recentTransactions.isNotEmpty()) {
                        binding.rvRecentTransactions.visibility = View.VISIBLE
                        binding.tvNoTransactions.visibility = View.GONE
                        transactionsAdapter.submitList(uiState.recentTransactions)
                    } else {
                        binding.rvRecentTransactions.visibility = View.GONE
                        binding.tvNoTransactions.visibility = View.VISIBLE
                    }
                }
            }
        }
    }

    private fun setupRecyclerViews() {
        // Accounts Horizontal RecyclerView
        accountsAdapter = AccountsHorizontalAdapter { accountId ->
            val action = HomeFragmentDirections.actionHomeFragmentToAddEditAccountFragment(accountId)
            findNavController().navigate(action)
        }
        binding.rvAccountsHorizontal.apply {
            layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
            adapter = accountsAdapter
        }

        // Recent Transactions RecyclerView
        transactionsAdapter = RecentTransactionsAdapter()
        binding.rvRecentTransactions.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = transactionsAdapter
            isNestedScrollingEnabled = false // Let NestedScrollView handle parent scroll
        }
    }

    private fun setupPieChart() {
        binding.pieChart.apply {
            description.isEnabled = false
            legend.isEnabled = false // Custom category detail card replaces standard legend
            isDrawHoleEnabled = true
            setHoleColor(Color.TRANSPARENT)
            setDrawEntryLabels(false) // Disable labels on slices for a cleaner dashboard look
            animateY(800)

            setOnChartValueSelectedListener(object : OnChartValueSelectedListener {
                override fun onValueSelected(e: Entry?, h: Highlight?) {
                    val envelope = e?.data as? EnvelopeWithAllocation
                    viewModel.selectCategory(envelope?.categoryId)
                }

                override fun onNothingSelected() {
                    viewModel.selectCategory(null)
                }
            })
        }
    }

    private fun updatePieChartData(envelopes: List<EnvelopeWithAllocation>) {
        val entries = envelopes.map { env ->
            PieEntry(env.spentAmountPaise.toFloat() / 100f, env.categoryName, env)
        }

        val dataSet = PieDataSet(entries, "Expense Categories").apply {
            colors = envelopes.map { env ->
                try {
                    Color.parseColor(env.colorHex)
                } catch (e: Exception) {
                    Color.GRAY
                }
            }
            valueTextSize = 0f // Hide text value directly on the pie slice
            setDrawValues(false)
        }

        binding.pieChart.data = PieData(dataSet)
        // Only animate on the very first load — subsequent reactive updates
        // should not re-trigger the spin animation (T058 performance fix).
        if (isFirstChartLoad) {
            isFirstChartLoad = false
            binding.pieChart.animateY(800)
        } else {
            binding.pieChart.invalidate()
        }
    }

    private fun setupFAB() {
        binding.fabAddTransaction.setOnClickListener {
            val action = HomeFragmentDirections.actionHomeFragmentToAddTransactionFragment()
            findNavController().navigate(action)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
