package com.rushi.coinmaster.ui.budget

import android.app.AlertDialog
import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.rushi.coinmaster.R
import com.rushi.coinmaster.data.local.entity.BudgetPeriodEntity
import com.rushi.coinmaster.data.local.model.BucketType
import com.rushi.coinmaster.data.local.model.EnvelopeWithAllocation
import com.rushi.coinmaster.databinding.FragmentBudgetBinding
import com.rushi.coinmaster.databinding.ItemEnvelopeBinding
import com.rushi.coinmaster.domain.usecase.ComputeBucketSplitUseCase
import com.rushi.coinmaster.util.CurrencyFormatter
import com.rushi.coinmaster.util.LocaleHelper
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.text.DateFormatSymbols
import javax.inject.Inject

@AndroidEntryPoint
class BudgetFragment : Fragment() {

    private var _binding: FragmentBudgetBinding? = null
    private val binding get() = _binding!!

    private val viewModel: BudgetViewModel by activityViewModels()

    @Inject
    lateinit var computeBucketSplitUseCase: ComputeBucketSplitUseCase

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBudgetBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Month/Period Navigation
        binding.btnPrevMonth.setOnClickListener {
            viewModel.selectPreviousPeriod()
        }

        binding.btnNextMonth.setOnClickListener {
            viewModel.selectNextPeriod()
        }

        // Adjust Setup Button
        binding.btnSetupMonth.setOnClickListener {
            val periodId = viewModel.selectedPeriodId.value ?: 0
            val action = BudgetFragmentDirections.actionBudgetFragmentToMonthSetupFragment(periodId)
            findNavController().navigate(action)
        }

        // Toolbar Menu Action (Manage Envelopes pool)
        binding.toolbar.inflateMenu(R.menu.menu_budget)
        binding.toolbar.setOnMenuItemClickListener { menuItem ->
            if (menuItem.itemId == R.id.action_manage_envelopes) {
                val action = BudgetFragmentDirections.actionBudgetFragmentToManageEnvelopesFragment()
                findNavController().navigate(action)
                true
            } else {
                false
            }
        }

        // Activate Budget Button
        binding.btnActivateBudget.setOnClickListener {
            viewModel.activateBudgetPeriod()
        }

        // Add Envelopes Buttons
        binding.btnAddNeedsEnvelope.setOnClickListener {
            showEnvelopeSelectionDialog(BucketType.NEEDS)
        }

        binding.btnAddWantsEnvelope.setOnClickListener {
            showEnvelopeSelectionDialog(BucketType.WANTS)
        }

        binding.btnAddSavingsEnvelope.setOnClickListener {
            showEnvelopeSelectionDialog(BucketType.SAVINGS)
        }

        binding.btnManageGoals.setOnClickListener {
            val action = BudgetFragmentDirections.actionBudgetFragmentToGoalsFragment()
            findNavController().navigate(action)
        }

        binding.fabAddTransaction.setOnClickListener {
            val action = BudgetFragmentDirections.actionBudgetFragmentToAddTransactionFragment()
            findNavController().navigate(action)
        }

        // Copy Previous Period Action
        binding.btnCopyPrevious.setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle("Copy Previous Budget")
                .setMessage("Are you sure you want to copy allocations from the previous budget period? This will overwrite any current allocations.")
                .setPositiveButton("Copy") { _, _ ->
                    viewModel.copyAllocationsFromPreviousPeriod()
                }
                .setNegativeButton("Cancel", null)
                .show()
        }

        // Observe ViewModel flows
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.budgetPeriodState.collect { period ->
                        updatePeriodLabel(period)
                        updateBudgetUi(period)
                    }
                }

                launch {
                    viewModel.showCopyPreviousState.collect { show ->
                        binding.cardCopyPrevious.visibility = if (show) View.VISIBLE else View.GONE
                    }
                }

                launch {
                    viewModel.envelopesState.collect { envelopes ->
                        renderEnvelopes(envelopes)
                    }
                }

                launch {
                    viewModel.uiEvent.collect { event ->
                        when (event) {
                            is BudgetUiEvent.SuccessActivation -> {
                                Toast.makeText(requireContext(), getString(R.string.text_budget_success), Toast.LENGTH_SHORT).show()
                            }
                            is BudgetUiEvent.Error -> {
                                Toast.makeText(requireContext(), event.message, Toast.LENGTH_LONG).show()
                            }
                            else -> {}
                        }
                    }
                }
            }
        }
    }

    private fun updatePeriodLabel(period: BudgetPeriodEntity?) {
        if (period != null) {
            val sdf = java.text.SimpleDateFormat("dd MMM yyyy", java.util.Locale.getDefault())
            val startStr = sdf.format(period.startDate)
            val endStr = sdf.format(period.endDate)
            binding.tvMonthYear.text = "$startStr - $endStr"
        } else {
            binding.tvMonthYear.text = "No Budget Period"
        }
    }

    private fun updateBudgetUi(period: BudgetPeriodEntity?) {
        val languageCode = LocaleHelper.getLanguage(requireContext())

        if (period == null) {
            binding.tvDeclaredIncome.text = getString(R.string.placeholder_declared_income)
            binding.tvUnallocatedStatus.text = getString(R.string.text_setup_required)
            binding.tvUnallocatedStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_secondary))
            binding.tvStatusHelper.text = getString(R.string.text_setup_helper)
            binding.btnActivateBudget.isEnabled = false
            binding.btnActivateBudget.text = getString(R.string.btn_activate_budget)

            binding.tvNeedsRatio.text = "₹0 / ₹0"
            binding.tvWantsRatio.text = "₹0 / ₹0"
            binding.tvSavingsRatio.text = "₹0 / ₹0"

            binding.progressNeeds.progress = 0
            binding.progressWants.progress = 0
            binding.progressSavings.progress = 0
            return
        }

        binding.tvDeclaredIncome.text = "Declared Income: ${CurrencyFormatter.format(period.incomePaise, languageCode)}"

        // Observe unallocated state reactively
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.unallocatedState.collect { validation ->
                    if (period.isActive) {
                        binding.tvUnallocatedStatus.text = getString(R.string.text_budget_balanced)
                        binding.tvUnallocatedStatus.setTextColor(resources.getColor(android.R.color.holo_green_dark, null))
                        binding.tvStatusHelper.text = getString(R.string.text_budget_active)
                        binding.btnActivateBudget.isEnabled = false
                        binding.btnActivateBudget.text = getString(R.string.btn_activated)
                    } else {
                        binding.btnActivateBudget.text = getString(R.string.btn_activate_budget)
                        when {
                            validation.isValid -> {
                                binding.tvUnallocatedStatus.text = getString(R.string.text_all_allocated)
                                binding.tvUnallocatedStatus.setTextColor(resources.getColor(android.R.color.holo_green_dark, null))
                                binding.tvStatusHelper.text = getString(R.string.text_budget_ready)
                                binding.btnActivateBudget.isEnabled = true
                            }
                            validation.differencePaise > 0L -> {
                                binding.tvUnallocatedStatus.text = getString(R.string.text_left_to_budget, CurrencyFormatter.format(validation.differencePaise, languageCode))
                                binding.tvUnallocatedStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.primary))
                                binding.tvStatusHelper.text = getString(R.string.text_zbb_rule)
                                binding.btnActivateBudget.isEnabled = false
                            }
                            else -> {
                                binding.tvUnallocatedStatus.text = getString(R.string.text_over_allocated, CurrencyFormatter.format(Math.abs(validation.differencePaise), languageCode))
                                binding.tvUnallocatedStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.error))
                                binding.tvStatusHelper.text = getString(R.string.text_over_allocated_info)
                                binding.btnActivateBudget.isEnabled = false
                            }
                        }
                    }
                }
            }
        }

        // Update bucket split projections
        val split = computeBucketSplitUseCase(period.incomePaise, period.needsPercent, period.wantsPercent, period.savingsPercent)
        val envelopes = viewModel.envelopesState.value
        val needsAllocated = envelopes.filter { it.bucketType == BucketType.NEEDS }.sumOf { it.allocatedAmountPaise }
        val wantsAllocated = envelopes.filter { it.bucketType == BucketType.WANTS }.sumOf { it.allocatedAmountPaise }
        val savingsAllocated = envelopes.filter { it.bucketType == BucketType.SAVINGS }.sumOf { it.allocatedAmountPaise }

        binding.tvNeedsRatio.text = "${CurrencyFormatter.format(needsAllocated, languageCode)} / ${CurrencyFormatter.format(split.needsPaise, languageCode)}"
        binding.tvWantsRatio.text = "${CurrencyFormatter.format(wantsAllocated, languageCode)} / ${CurrencyFormatter.format(split.wantsPaise, languageCode)}"
        binding.tvSavingsRatio.text = "${CurrencyFormatter.format(savingsAllocated, languageCode)} / ${CurrencyFormatter.format(split.savingsPaise, languageCode)}"

        binding.progressNeeds.max = 100
        binding.progressNeeds.progress = if (split.needsPaise > 0) ((needsAllocated * 100) / split.needsPaise).toInt() else 0

        binding.progressWants.max = 100
        binding.progressWants.progress = if (split.wantsPaise > 0) ((wantsAllocated * 100) / split.wantsPaise).toInt() else 0

        binding.progressSavings.max = 100
        binding.progressSavings.progress = if (split.savingsPaise > 0) ((savingsAllocated * 100) / split.savingsPaise).toInt() else 0
    }

    private fun renderEnvelopes(envelopes: List<EnvelopeWithAllocation>) {
        val languageCode = LocaleHelper.getLanguage(requireContext())
        binding.containerNeedsEnvelopes.removeAllViews()
        binding.containerWantsEnvelopes.removeAllViews()
        binding.containerSavingsEnvelopes.removeAllViews()

        for (envelope in envelopes) {
            val itemBinding = ItemEnvelopeBinding.inflate(layoutInflater, null, false)

            itemBinding.tvEnvelopeName.text = envelope.categoryName
            itemBinding.viewEnvelopeColor.setBackgroundColor(Color.parseColor(envelope.colorHex))
            itemBinding.ivEnvelopeIcon.setImageResource(getIconDrawableResId(envelope.iconName))
            itemBinding.tvEnvelopeSpent.text = getString(R.string.text_spent_prefix, CurrencyFormatter.format(envelope.spentAmountPaise, languageCode))
            itemBinding.tvAllocatedAmount.text = CurrencyFormatter.format(envelope.allocatedAmountPaise, languageCode)

            val tvAllocated = itemBinding.tvAllocatedAmount
            val etAllocated = itemBinding.etAllocatedAmount

            // Click to Edit Allocation (Inline)
            itemBinding.layoutAllocationClick.setOnClickListener {
                if (viewModel.budgetPeriodState.value?.isActive == true) {
                    Toast.makeText(requireContext(), getString(R.string.text_cannot_modify_active), Toast.LENGTH_SHORT).show()
                } else {
                    if (tvAllocated.visibility == View.VISIBLE) {
                        tvAllocated.visibility = View.GONE
                        etAllocated.visibility = View.VISIBLE
                        
                        val rupeesVal = String.format("%.2f", envelope.allocatedAmountPaise / 100.0)
                        etAllocated.setText(rupeesVal)
                        etAllocated.requestFocus()
                        etAllocated.setSelection(etAllocated.text.length)
                        showKeyboard(etAllocated)
                    }
                }
            }

            etAllocated.setOnEditorActionListener { _, actionId, _ ->
                if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_DONE) {
                    val amountStr = etAllocated.text.toString()
                    viewModel.saveAllocation(envelope.categoryId, amountStr)
                    etAllocated.clearFocus()
                    true
                } else {
                    false
                }
            }

            etAllocated.setOnFocusChangeListener { _, hasFocus ->
                if (!hasFocus) {
                    val amountStr = etAllocated.text.toString()
                    val originalStr = String.format("%.2f", envelope.allocatedAmountPaise / 100.0)
                    if (amountStr != originalStr && amountStr.isNotBlank()) {
                        viewModel.saveAllocation(envelope.categoryId, amountStr)
                    }
                    tvAllocated.visibility = View.VISIBLE
                    etAllocated.visibility = View.GONE
                    hideKeyboard(etAllocated)
                }
            }

            // Click to Edit Envelope Metadata
            itemBinding.btnEditEnvelope.setOnClickListener {
                val action = BudgetFragmentDirections.actionBudgetFragmentToAddEditEnvelopeFragment(
                    categoryId = envelope.categoryId,
                    bucketTypeOrdinal = envelope.bucketType.ordinal
                )
                findNavController().navigate(action)
            }

            when (envelope.bucketType) {
                BucketType.NEEDS -> binding.containerNeedsEnvelopes.addView(itemBinding.root)
                BucketType.WANTS -> binding.containerWantsEnvelopes.addView(itemBinding.root)
                BucketType.SAVINGS -> binding.containerSavingsEnvelopes.addView(itemBinding.root)
            }
        }

        // Re-trigger progress bar and split logic updates since envelopes have re-rendered
        viewModel.budgetPeriodState.value?.let { updateBudgetUi(it) }
    }

    private fun showKeyboard(view: View) {
        val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
        imm.showSoftInput(view, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT)
    }

    private fun hideKeyboard(view: View) {
        val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
        imm.hideSoftInputFromWindow(view.windowToken, 0)
    }

    private fun getIconDrawableResId(iconName: String): Int {
        return when (iconName) {
            "ic_rent" -> android.R.drawable.ic_menu_today
            "ic_groceries" -> android.R.drawable.ic_menu_gallery
            "ic_utilities" -> android.R.drawable.ic_menu_info_details
            "ic_dining" -> android.R.drawable.ic_menu_compass
            "ic_entertainment" -> android.R.drawable.ic_menu_slideshow
            "ic_shopping" -> android.R.drawable.ic_menu_view
            "ic_savings" -> android.R.drawable.ic_menu_save
            "ic_emergency" -> android.R.drawable.ic_menu_help
            else -> android.R.drawable.ic_menu_help
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun showEnvelopeSelectionDialog(bucketType: BucketType) {
        val unassignedEnvelopes = viewModel.allCategoriesState.value.filter {
            it.bucketType == null && !it.isDeleted
        }

        if (unassignedEnvelopes.isEmpty()) {
            AlertDialog.Builder(requireContext())
                .setTitle("No Unassigned Envelopes")
                .setMessage("All envelopes have been allocated to a bucket. Please create new envelopes in the pool first.")
                .setPositiveButton("Manage Envelopes") { _, _ ->
                    val action = BudgetFragmentDirections.actionBudgetFragmentToManageEnvelopesFragment()
                    findNavController().navigate(action)
                }
                .setNegativeButton("Cancel", null)
                .show()
        } else {
            val names = unassignedEnvelopes.map { it.name }.toTypedArray()
            AlertDialog.Builder(requireContext())
                .setTitle("Select Envelope for $bucketType")
                .setItems(names) { _, which ->
                    val selectedCategory = unassignedEnvelopes[which]
                    viewModel.assignCategoryToBucket(selectedCategory.id, bucketType)
                }
                .setNeutralButton("Manage Envelopes") { _, _ ->
                    val action = BudgetFragmentDirections.actionBudgetFragmentToManageEnvelopesFragment()
                    findNavController().navigate(action)
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }
}
