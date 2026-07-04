package com.rushi.coinmaster.ui.analysis

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.rushi.coinmaster.MainActivity
import com.rushi.coinmaster.R
import com.rushi.coinmaster.data.local.entity.BudgetPeriodEntity
import com.rushi.coinmaster.databinding.FragmentBudgetAnalysisBinding
import com.rushi.coinmaster.ui.budget.BudgetViewModel
import com.rushi.coinmaster.util.CurrencyFormatter
import com.rushi.coinmaster.util.LocaleHelper
import androidx.navigation.ui.setupWithNavController
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

class BudgetAnalysisFragment : Fragment() {

    private var _binding: FragmentBudgetAnalysisBinding? = null
    private val binding get() = _binding!!

    private val viewModel: BudgetViewModel by activityViewModels()
    private lateinit var adapter: CategoryAnalysisAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBudgetAnalysisBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val navController = findNavController()
        val appBarConfiguration = androidx.navigation.ui.AppBarConfiguration(
            setOf(R.id.homeFragment, R.id.transactionsFragment, R.id.budgetFragment, R.id.budgetAnalysisFragment),
            (requireActivity() as MainActivity).drawerLayout
        )
        binding.toolbar.setupWithNavController(navController, appBarConfiguration)

        setupRecyclerView()

        binding.btnPrevMonth.setOnClickListener {
            viewModel.selectPreviousPeriod()
        }

        binding.btnNextMonth.setOnClickListener {
            viewModel.selectNextPeriod()
        }

        binding.fabAddTransaction.setOnClickListener {
            val action = BudgetAnalysisFragmentDirections.actionAnalysisFragmentToAddTransactionFragment()
            findNavController().navigate(action)
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.budgetPeriodState.collect { period ->
                        updatePeriodLabel(period)
                    }
                }

                launch {
                    combine(
                        viewModel.groupedCategoriesState,
                        viewModel.envelopesState
                    ) { groupedCategories, envelopes ->
                        Pair(groupedCategories, envelopes)
                    }.collect { (groupedCategories, envelopes) ->
                        renderAnalysisData(groupedCategories, envelopes)
                    }
                }
            }
        }
    }

    private fun setupRecyclerView() {
        adapter = CategoryAnalysisAdapter(
            onCategoryClick = { group ->
                val dialog = EnvelopeAnalysisBottomSheet.newInstance(group.id, group.bucketType.ordinal)
                dialog.show(childFragmentManager, "envelope_analysis")
            },
            onViewTransactionsClick = { group ->
                val period = viewModel.budgetPeriodState.value
                if (period != null) {
                    val sdf = java.text.SimpleDateFormat("dd MMM yyyy", java.util.Locale.getDefault())
                    val rangeLabel = "${sdf.format(period.startDate)} - ${sdf.format(period.endDate)}"

                    val envelopeIds = group.envelopes.map { it.categoryId }.toLongArray()

                    val action = BudgetAnalysisFragmentDirections.actionAnalysisFragmentToTransactionsFragment(
                        envelopeIds = envelopeIds,
                        startMillis = period.startDate,
                        endMillis = period.endDate,
                        filterLabel = "${group.name} · $rangeLabel"
                    )
                    findNavController().navigate(action)
                }
            }
        )

        binding.rvCategories.layoutManager = LinearLayoutManager(requireContext())
        binding.rvCategories.adapter = adapter
    }

    private fun updatePeriodLabel(period: BudgetPeriodEntity?) {
        if (period != null) {
            val sdf = java.text.SimpleDateFormat("dd MMM yyyy", java.util.Locale.getDefault())
            val startStr = sdf.format(period.startDate)
            val endStr = sdf.format(period.endDate)
            val label = "$startStr - $endStr"
            binding.tvMonthYear.text = label
            binding.tvTotalPeriodLabel.text = label
            binding.toolbar.title = "Analysis"
            binding.cardTotalBudget.visibility = View.VISIBLE
            binding.layoutMonthSwitcher.visibility = View.VISIBLE
        } else {
            binding.tvMonthYear.text = "No Budget Period"
            binding.tvTotalPeriodLabel.text = ""
            binding.toolbar.title = "Analysis"
            binding.cardTotalBudget.visibility = View.GONE
            binding.layoutMonthSwitcher.visibility = View.GONE
        }
    }

    private fun renderAnalysisData(
        groupedCategories: List<com.rushi.coinmaster.data.local.model.GroupedCategory>,
        envelopes: List<com.rushi.coinmaster.data.local.model.EnvelopeWithAllocation>
    ) {
        val languageCode = LocaleHelper.getLanguage(requireContext())

        val totalSpent = envelopes.sumOf { it.spentAmountPaise }
        val totalAllocated = envelopes.sumOf { it.allocatedAmountPaise }

        binding.tvTotalAmounts.text = "${CurrencyFormatter.format(totalSpent, languageCode)} / ${CurrencyFormatter.format(totalAllocated, languageCode)}"
        binding.semiCircleProgressView.setProgress(totalSpent, totalAllocated)

        adapter.submitList(groupedCategories)

        if (groupedCategories.isEmpty()) {
            binding.tvEmptyState.visibility = View.VISIBLE
            binding.rvCategories.visibility = View.GONE
        } else {
            binding.tvEmptyState.visibility = View.GONE
            binding.rvCategories.visibility = View.VISIBLE
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
