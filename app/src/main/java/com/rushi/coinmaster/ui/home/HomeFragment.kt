package com.rushi.coinmaster.ui.home

import android.content.Context
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
import com.rushi.coinmaster.data.local.model.BucketType
import com.rushi.coinmaster.data.local.model.EnvelopeWithAllocation
import com.rushi.coinmaster.data.local.model.ExpenseType
import com.rushi.coinmaster.databinding.FragmentHomeBinding
import com.rushi.coinmaster.util.CurrencyFormatter
import com.rushi.coinmaster.util.LocaleHelper
import com.rushi.coinmaster.MainActivity
import androidx.navigation.ui.setupWithNavController
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val viewModel: HomeViewModel by viewModels()

    private lateinit var accountsAdapter: AccountsHorizontalAdapter

    /** Guards the one-shot entry animation for the pie charts. */
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

        val navController = findNavController()
        val appBarConfiguration = androidx.navigation.ui.AppBarConfiguration(
            setOf(R.id.homeFragment, R.id.transactionsFragment, R.id.budgetFragment),
            (requireActivity() as MainActivity).drawerLayout
        )
        binding.toolbar.setupWithNavController(navController, appBarConfiguration)

        setupRecyclerView()
        setupPieCharts()
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

                    // 4. Update Pie Charts Data
                    updateChartsData(uiState.envelopes)

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
                }
            }
        }
    }

    private fun setupRecyclerView() {
        accountsAdapter = AccountsHorizontalAdapter { accountId ->
            val action = HomeFragmentDirections.actionHomeFragmentToAddEditAccountFragment(accountId)
            findNavController().navigate(action)
        }
        binding.rvAccountsHorizontal.apply {
            layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
            adapter = accountsAdapter
        }
    }

    private fun setupPieCharts() {
        setupPieChart(binding.pieChart, true)
        setupPieChart(binding.pieChartNeedsWants, false)
        setupPieChart(binding.pieChartExpenseType, false)
    }

    private fun setupPieChart(chart: com.github.mikephil.charting.charts.PieChart, enableSelectionListener: Boolean) {
        chart.apply {
            description.isEnabled = false
            legend.isEnabled = false
            isDrawHoleEnabled = true
            setHoleColor(Color.TRANSPARENT)
            setDrawEntryLabels(false)
            animateY(800)

            if (enableSelectionListener) {
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
    }

    private fun updateChartsData(envelopes: List<EnvelopeWithAllocation>) {
        val spentEnvelopes = envelopes.filter { it.spentAmountPaise > 0L }

        // 1. Needs vs Wants Chart
        val needsSpent = spentEnvelopes.filter { it.bucketType == BucketType.NEEDS }.sumOf { it.spentAmountPaise }
        val wantsSpent = spentEnvelopes.filter { it.bucketType == BucketType.WANTS }.sumOf { it.spentAmountPaise }
        
        if (needsSpent > 0L || wantsSpent > 0L) {
            binding.cardBreakdownNeedsWants.visibility = View.VISIBLE
            val entries = mutableListOf<PieEntry>()
            val colors = mutableListOf<Int>()
            if (needsSpent > 0L) {
                entries.add(PieEntry(needsSpent.toFloat() / 100f, "Needs"))
                colors.add(Color.parseColor("#1E88E5"))
            }
            if (wantsSpent > 0L) {
                entries.add(PieEntry(wantsSpent.toFloat() / 100f, "Wants"))
                colors.add(Color.parseColor("#E53935"))
            }
            updateChartData(binding.pieChartNeedsWants, entries, colors)
        } else {
            binding.cardBreakdownNeedsWants.visibility = View.GONE
        }

        // 2. Expense Type Breakdown (Fixed vs Variable) Chart
        val fixedSpent = spentEnvelopes.filter { it.expenseType == ExpenseType.FIXED }.sumOf { it.spentAmountPaise }
        val variableSpent = spentEnvelopes.filter { it.expenseType == ExpenseType.VARIABLE }.sumOf { it.spentAmountPaise }

        if (fixedSpent > 0L || variableSpent > 0L) {
            binding.cardBreakdownExpenseType.visibility = View.VISIBLE
            val entries = mutableListOf<PieEntry>()
            val colors = mutableListOf<Int>()
            if (fixedSpent > 0L) {
                entries.add(PieEntry(fixedSpent.toFloat() / 100f, "Fixed"))
                colors.add(Color.parseColor("#43A047"))
            }
            if (variableSpent > 0L) {
                entries.add(PieEntry(variableSpent.toFloat() / 100f, "Variable"))
                colors.add(Color.parseColor("#FB8C00"))
            }
            updateChartData(binding.pieChartExpenseType, entries, colors)
        } else {
            binding.cardBreakdownExpenseType.visibility = View.GONE
        }

        // 3. Category Breakdown Chart
        if (spentEnvelopes.isNotEmpty()) {
            binding.pieChart.visibility = View.VISIBLE
            binding.cardCategoryDetail.visibility = View.VISIBLE
            updateCategoryChart(spentEnvelopes)
        } else {
            binding.pieChart.visibility = View.GONE
            binding.cardCategoryDetail.visibility = View.GONE
        }

        // Disable animation flag after first load
        if (isFirstChartLoad) {
            isFirstChartLoad = false
        }
    }

    private fun updateCategoryChart(envelopes: List<EnvelopeWithAllocation>) {
        val entries = envelopes.map { env ->
            PieEntry(env.spentAmountPaise.toFloat() / 100f, env.categoryName, env)
        }
        val colors = envelopes.map { env ->
            try {
                Color.parseColor(env.colorHex)
            } catch (e: Exception) {
                Color.GRAY
            }
        }
        updateChartData(binding.pieChart, entries, colors)
    }

    private fun updateChartData(chart: com.github.mikephil.charting.charts.PieChart, entries: List<PieEntry>, colorsList: List<Int>) {
        val dataSet = PieDataSet(entries, "").apply {
            colors = colorsList
            valueTextSize = 12f
            setDrawValues(true)
            valueTextColor = Color.WHITE
        }
        chart.data = PieData(dataSet)
        if (isFirstChartLoad) {
            chart.animateY(800)
        } else {
            chart.invalidate()
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
