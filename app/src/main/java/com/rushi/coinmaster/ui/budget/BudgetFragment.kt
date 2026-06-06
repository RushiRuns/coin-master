package com.rushi.coinmaster.ui.budget

import android.app.AlertDialog
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
import com.rushi.coinmaster.data.local.entity.BudgetMonthEntity
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

        // Month Navigation
        binding.btnPrevMonth.setOnClickListener {
            viewModel.selectPreviousMonth()
        }

        binding.btnNextMonth.setOnClickListener {
            viewModel.selectNextMonth()
        }

        // Adjust Setup Button
        binding.btnSetupMonth.setOnClickListener {
            val action = BudgetFragmentDirections.actionBudgetFragmentToMonthSetupFragment(viewModel.selectedMonthId.value)
            findNavController().navigate(action)
        }

        // Activate Budget Button
        binding.btnActivateBudget.setOnClickListener {
            viewModel.activateBudgetMonth()
        }

        // Add Envelopes Buttons
        binding.btnAddNeedsEnvelope.setOnClickListener {
            val action = BudgetFragmentDirections.actionBudgetFragmentToAddEditEnvelopeFragment(
                categoryId = 0L,
                bucketTypeOrdinal = BucketType.NEEDS.ordinal
            )
            findNavController().navigate(action)
        }

        binding.btnAddWantsEnvelope.setOnClickListener {
            val action = BudgetFragmentDirections.actionBudgetFragmentToAddEditEnvelopeFragment(
                categoryId = 0L,
                bucketTypeOrdinal = BucketType.WANTS.ordinal
            )
            findNavController().navigate(action)
        }

        binding.btnAddSavingsEnvelope.setOnClickListener {
            val action = BudgetFragmentDirections.actionBudgetFragmentToAddEditEnvelopeFragment(
                categoryId = 0L,
                bucketTypeOrdinal = BucketType.SAVINGS.ordinal
            )
            findNavController().navigate(action)
        }

        // Observe ViewModel flows
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.selectedMonthId.collect { monthId ->
                        updateMonthLabel(monthId)
                    }
                }

                launch {
                    viewModel.budgetMonthState.collect { month ->
                        updateBudgetUi(month)
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
                                Toast.makeText(requireContext(), "Budget activated successfully!", Toast.LENGTH_SHORT).show()
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

    private fun updateMonthLabel(monthId: Int) {
        val year = monthId / 100
        val month = monthId % 100
        val monthName = DateFormatSymbols().months[month - 1]
        binding.tvMonthYear.text = "$monthName $year"
    }

    private fun updateBudgetUi(month: BudgetMonthEntity?) {
        val languageCode = LocaleHelper.getLanguage(requireContext())

        if (month == null) {
            binding.tvDeclaredIncome.text = "Declared Income: ₹0.00"
            binding.tvUnallocatedStatus.text = "Setup Required"
            binding.tvUnallocatedStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_secondary))
            binding.tvStatusHelper.text = "Please set up your budget details first."
            binding.btnActivateBudget.isEnabled = false
            binding.btnActivateBudget.text = "Activate Budget"

            binding.tvNeedsRatio.text = "₹0 / ₹0"
            binding.tvWantsRatio.text = "₹0 / ₹0"
            binding.tvSavingsRatio.text = "₹0 / ₹0"

            binding.progressNeeds.progress = 0
            binding.progressWants.progress = 0
            binding.progressSavings.progress = 0
            return
        }

        binding.tvDeclaredIncome.text = "Declared Income: ${CurrencyFormatter.format(month.incomePaise, languageCode)}"

        // Observe unallocated state reactively
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.unallocatedState.collect { validation ->
                    if (month.isActive) {
                        binding.tvUnallocatedStatus.text = "Budget Balanced"
                        binding.tvUnallocatedStatus.setTextColor(resources.getColor(android.R.color.holo_green_dark, null))
                        binding.tvStatusHelper.text = "Budget is active."
                        binding.btnActivateBudget.isEnabled = false
                        binding.btnActivateBudget.text = "Activated"
                    } else {
                        binding.btnActivateBudget.text = "Activate Budget"
                        when {
                            validation.isValid -> {
                                binding.tvUnallocatedStatus.text = "All Rupee(s) Allocated!"
                                binding.tvUnallocatedStatus.setTextColor(resources.getColor(android.R.color.holo_green_dark, null))
                                binding.tvStatusHelper.text = "Your budget is balanced and ready."
                                binding.btnActivateBudget.isEnabled = true
                            }
                            validation.differencePaise > 0L -> {
                                binding.tvUnallocatedStatus.text = "${CurrencyFormatter.format(validation.differencePaise, languageCode)} Left to Budget"
                                binding.tvUnallocatedStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.primary))
                                binding.tvStatusHelper.text = "ZBB requires unallocated balance to be zero."
                                binding.btnActivateBudget.isEnabled = false
                            }
                            else -> {
                                binding.tvUnallocatedStatus.text = "${CurrencyFormatter.format(Math.abs(validation.differencePaise), languageCode)} Over-allocated"
                                binding.tvUnallocatedStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.error))
                                binding.tvStatusHelper.text = "You have allocated more than your income."
                                binding.btnActivateBudget.isEnabled = false
                            }
                        }
                    }
                }
            }
        }

        // Update bucket split projections
        val split = computeBucketSplitUseCase(month.incomePaise, month.needsPercent, month.wantsPercent, month.savingsPercent)
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
            itemBinding.tvEnvelopeSpent.text = "Spent: " + CurrencyFormatter.format(envelope.spentAmountPaise, languageCode)
            itemBinding.tvAllocatedAmount.text = CurrencyFormatter.format(envelope.allocatedAmountPaise, languageCode)

            // Click to Edit Allocation
            itemBinding.layoutAllocationClick.setOnClickListener {
                if (viewModel.budgetMonthState.value?.isActive == true) {
                    Toast.makeText(requireContext(), "Cannot modify allocations for an active budget.", Toast.LENGTH_SHORT).show()
                } else {
                    showAllocationDialog(envelope)
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
        viewModel.budgetMonthState.value?.let { updateBudgetUi(it) }
    }

    private fun showAllocationDialog(envelope: EnvelopeWithAllocation) {
        val input = EditText(requireContext()).apply {
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
            setText(String.format("%.2f", envelope.allocatedAmountPaise / 100.0))
            setSelection(text.length)
        }

        val container = FrameLayout(requireContext())
        val margin = (24 * resources.displayMetrics.density).toInt()
        val params = FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply {
            leftMargin = margin
            rightMargin = margin
            topMargin = (8 * resources.displayMetrics.density).toInt()
            bottomMargin = (8 * resources.displayMetrics.density).toInt()
        }
        input.layoutParams = params
        container.addView(input)

        AlertDialog.Builder(requireContext())
            .setTitle("Allocate Budget")
            .setMessage("Enter allocation for ${envelope.categoryName}:")
            .setView(container)
            .setPositiveButton("Save") { _, _ ->
                val amountStr = input.text.toString()
                viewModel.saveAllocation(envelope.categoryId, amountStr)
            }
            .setNegativeButton("Cancel", null)
            .show()
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
}
