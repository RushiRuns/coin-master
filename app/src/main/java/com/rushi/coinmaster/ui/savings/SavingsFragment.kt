package com.rushi.coinmaster.ui.savings

import android.app.AlertDialog
import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
import com.rushi.coinmaster.databinding.FragmentSavingsBinding
import com.rushi.coinmaster.databinding.ItemEnvelopeBinding
import com.rushi.coinmaster.domain.usecase.ComputeBucketSplitUseCase
import com.rushi.coinmaster.ui.budget.BudgetViewModel
import com.rushi.coinmaster.util.CurrencyFormatter
import com.rushi.coinmaster.util.LocaleHelper
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class SavingsFragment : Fragment() {

    private var _binding: FragmentSavingsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: BudgetViewModel by activityViewModels()

    @Inject
    lateinit var computeBucketSplitUseCase: ComputeBucketSplitUseCase

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSavingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.toolbar.setNavigationOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }

        binding.btnAddSavingsEnvelope.setOnClickListener {
            showEnvelopeSelectionDialog()
        }

        binding.btnManageGoals.setOnClickListener {
            findNavController().navigate(R.id.nav_goals)
        }

        // Observe ViewModel flows
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.budgetPeriodState.collect { period ->
                        updateSavingsUi(period)
                    }
                }

                launch {
                    combine(
                        viewModel.envelopesState,
                        viewModel.budgetPeriodState
                    ) { envelopes, period ->
                        Pair(envelopes, period)
                    }.collect { (envelopes, period) ->
                        renderSavingsList(envelopes)
                        updateSavingsUi(period)
                    }
                }
            }
        }
    }

    private fun updateSavingsUi(period: BudgetPeriodEntity?) {
        val languageCode = LocaleHelper.getLanguage(requireContext())

        if (period == null) {
            binding.tvTotalSavingsTarget.text = "Target: ₹0.00"
            binding.tvRemainingSavings.text = "₹0.00 Left to Allocate"
            binding.tvSavingsRatio.text = "₹0 / ₹0"
            binding.progressSavings.progress = 0
            return
        }

        val split = computeBucketSplitUseCase(
            period.incomePaise,
            period.needsPercent,
            period.wantsPercent,
            period.savingsPercent
        )
        val envelopes = viewModel.envelopesState.value
        val savingsAllocated = envelopes.filter { it.bucketType == BucketType.SAVINGS }.sumOf { it.allocatedAmountPaise }
        val remainingSavings = split.savingsPaise - savingsAllocated

        binding.tvTotalSavingsTarget.text = "Target: ${CurrencyFormatter.format(split.savingsPaise, languageCode)} (${period.savingsPercent}% of Income)"
        
        if (remainingSavings >= 0L) {
            binding.tvRemainingSavings.text = "${CurrencyFormatter.format(remainingSavings, languageCode)} Left to Allocate"
            binding.tvRemainingSavings.setTextColor(ContextCompat.getColor(requireContext(), R.color.secondary))
        } else {
            binding.tvRemainingSavings.text = "${CurrencyFormatter.format(Math.abs(remainingSavings), languageCode)} Over-allocated"
            binding.tvRemainingSavings.setTextColor(ContextCompat.getColor(requireContext(), R.color.error))
        }

        binding.tvSavingsRatio.text = "${CurrencyFormatter.format(savingsAllocated, languageCode)} / ${CurrencyFormatter.format(split.savingsPaise, languageCode)}"

        binding.progressSavings.max = 100
        binding.progressSavings.progress = if (split.savingsPaise > 0) {
            ((savingsAllocated * 100) / split.savingsPaise).toInt()
        } else {
            0
        }
    }

    private fun renderSavingsList(envelopes: List<EnvelopeWithAllocation>) {
        val languageCode = LocaleHelper.getLanguage(requireContext())
        binding.containerSavingsEnvelopes.removeAllViews()

        val savingsEnvelopes = envelopes.filter { it.bucketType == BucketType.SAVINGS }

        if (savingsEnvelopes.isEmpty()) {
            binding.tvEmptySavings.visibility = View.VISIBLE
        } else {
            binding.tvEmptySavings.visibility = View.GONE
            for (envelope in savingsEnvelopes) {
                val itemBinding = ItemEnvelopeBinding.inflate(layoutInflater, binding.containerSavingsEnvelopes, false)

                itemBinding.tvEnvelopeName.text = envelope.categoryName
                
                try {
                    itemBinding.viewEnvelopeColor.setBackgroundColor(Color.parseColor(envelope.colorHex))
                } catch (e: Exception) {
                    itemBinding.viewEnvelopeColor.setBackgroundColor(Color.GRAY)
                }

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
                    val bundle = Bundle().apply {
                        putLong("categoryId", envelope.categoryId)
                        putInt("bucketTypeOrdinal", envelope.bucketType.ordinal)
                    }
                    findNavController().navigate(R.id.addEditEnvelopeFragment, bundle)
                }

                binding.containerSavingsEnvelopes.addView(itemBinding.root)
            }
        }
    }

    private fun showKeyboard(view: View) {
        val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
        imm.showSoftInput(view, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT)
    }

    private fun hideKeyboard(view: View) {
        val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
        imm.hideSoftInputFromWindow(view.windowToken, 0)
    }

    private fun showEnvelopeSelectionDialog() {
        val unassignedEnvelopes = viewModel.allCategoriesState.value.filter {
            it.bucketType == null && !it.isDeleted
        }

        if (unassignedEnvelopes.isEmpty()) {
            AlertDialog.Builder(requireContext())
                .setTitle("No Unassigned Envelopes")
                .setMessage("All envelopes have been allocated to a bucket. Please create new envelopes in the pool first.")
                .setPositiveButton("Manage Envelopes") { _, _ ->
                    findNavController().navigate(R.id.manageEnvelopesFragment)
                }
                .setNegativeButton("Cancel", null)
                .show()
        } else {
            val names = unassignedEnvelopes.map { it.name }.toTypedArray()
            AlertDialog.Builder(requireContext())
                .setTitle("Select Envelope for Savings")
                .setItems(names) { _, which ->
                    val selectedCategory = unassignedEnvelopes[which]
                    viewModel.assignCategoryToBucket(selectedCategory.id, BucketType.SAVINGS)
                }
                .setNeutralButton("Manage Envelopes") { _, _ ->
                    findNavController().navigate(R.id.manageEnvelopesFragment)
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
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
