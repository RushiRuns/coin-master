package com.rushi.coinmaster.ui.expensetype

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupWithNavController
import com.rushi.coinmaster.MainActivity
import com.rushi.coinmaster.R
import com.rushi.coinmaster.data.local.entity.CategoryEntity
import com.rushi.coinmaster.data.local.model.ExpenseType
import com.rushi.coinmaster.databinding.FragmentExpenseTypeBinding
import com.rushi.coinmaster.databinding.ItemExpenseTypeSelectBinding
import com.rushi.coinmaster.ui.budget.BudgetViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ExpenseTypeFragment : Fragment() {

    private var _binding: FragmentExpenseTypeBinding? = null
    private val binding get() = _binding!!

    private val viewModel: BudgetViewModel by activityViewModels()

    private val selectedIds = mutableSetOf<Long>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentExpenseTypeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.toolbar.setupWithNavController(
            findNavController(),
            AppBarConfiguration(
                setOf(
                    R.id.homeFragment, R.id.transactionsFragment, R.id.budgetFragment,
                    R.id.nav_categories, R.id.nav_expense_type, R.id.nav_income,
                    R.id.nav_savings, R.id.nav_accounts, R.id.nav_notes,
                    R.id.nav_transfers, R.id.nav_goals, R.id.nav_settings
                ),
                (requireActivity() as MainActivity).drawerLayout
            )
        )

        binding.btnAssignFixed.setOnClickListener {
            if (selectedIds.isNotEmpty()) {
                viewModel.updateExpenseTypeForCategories(selectedIds.toList(), ExpenseType.FIXED)
                Toast.makeText(
                    requireContext(),
                    "Assigned ${selectedIds.size} envelope(s) as Fixed",
                    Toast.LENGTH_SHORT
                ).show()
                selectedIds.clear()
                updateSelectionUi()
            }
        }

        binding.btnAssignVariable.setOnClickListener {
            if (selectedIds.isNotEmpty()) {
                viewModel.updateExpenseTypeForCategories(selectedIds.toList(), ExpenseType.VARIABLE)
                Toast.makeText(
                    requireContext(),
                    "Assigned ${selectedIds.size} envelope(s) as Variable",
                    Toast.LENGTH_SHORT
                ).show()
                selectedIds.clear()
                updateSelectionUi()
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.allCategoriesState.collectLatest { categories ->
                    renderCategories(categories)
                }
            }
        }
    }

    private fun renderCategories(categories: List<CategoryEntity>) {
        binding.containerUnclassifiedEnvelopes.removeAllViews()
        binding.containerFixedEnvelopes.removeAllViews()
        binding.containerVariableEnvelopes.removeAllViews()

        val activeCategories = categories.filter { !it.isDeleted }
        val unclassifiedCategories = activeCategories.filter { it.expenseType == null }
        val fixedCategories = activeCategories.filter { it.expenseType == ExpenseType.FIXED }
        val variableCategories = activeCategories.filter { it.expenseType == ExpenseType.VARIABLE }

        // Render Unclassified/Unassigned list
        if (unclassifiedCategories.isEmpty()) {
            binding.tvEmptyUnclassified.visibility = View.VISIBLE
        } else {
            binding.tvEmptyUnclassified.visibility = View.GONE
            for (category in unclassifiedCategories) {
                val itemBinding = createItemView(category)
                binding.containerUnclassifiedEnvelopes.addView(itemBinding.root)
            }
        }

        // Render Fixed list
        if (fixedCategories.isEmpty()) {
            binding.tvEmptyFixed.visibility = View.VISIBLE
        } else {
            binding.tvEmptyFixed.visibility = View.GONE
            for (category in fixedCategories) {
                val itemBinding = createItemView(category)
                binding.containerFixedEnvelopes.addView(itemBinding.root)
            }
        }

        // Render Variable list
        if (variableCategories.isEmpty()) {
            binding.tvEmptyVariable.visibility = View.VISIBLE
        } else {
            binding.tvEmptyVariable.visibility = View.GONE
            for (category in variableCategories) {
                val itemBinding = createItemView(category)
                binding.containerVariableEnvelopes.addView(itemBinding.root)
            }
        }

        // Verify selections still exist in active categories (e.g. not deleted or modified)
        val activeIds = activeCategories.map { it.id }.toSet()
        val toRemove = selectedIds.filter { it !in activeIds }
        if (toRemove.isNotEmpty()) {
            selectedIds.removeAll(toRemove.toSet())
            updateSelectionUi()
        }
    }

    private fun createItemView(category: CategoryEntity): ItemExpenseTypeSelectBinding {
        val itemBinding = ItemExpenseTypeSelectBinding.inflate(layoutInflater, null, false)
        itemBinding.tvEnvelopeName.text = category.name
        itemBinding.tvEnvelopeBucket.text = category.bucketType?.name ?: "Unassigned"
        
        try {
            itemBinding.viewEnvelopeColor.setBackgroundColor(
                Color.parseColor(category.colorHex)
            )
        } catch (e: Exception) {
            itemBinding.viewEnvelopeColor.setBackgroundColor(Color.GRAY)
        }
        
        itemBinding.ivEnvelopeIcon.setImageResource(getIconDrawableResId(category.iconName))

        // Set checkbox initial state
        itemBinding.cbSelect.isChecked = selectedIds.contains(category.id)

        // Single tap on item selects checkbox too for convenience (3-tap constraint improvement)
        itemBinding.root.setOnClickListener {
            itemBinding.cbSelect.performClick()
        }

        itemBinding.cbSelect.setOnClickListener {
            val isChecked = itemBinding.cbSelect.isChecked
            if (isChecked) {
                selectedIds.add(category.id)
            } else {
                selectedIds.remove(category.id)
            }
            updateSelectionUi()
        }

        return itemBinding
    }

    private fun updateSelectionUi() {
        if (selectedIds.isEmpty()) {
            binding.cardActions.visibility = View.GONE
        } else {
            binding.cardActions.visibility = View.VISIBLE
            binding.tvSelectedCount.text = "${selectedIds.size} envelope(s) selected"
        }
    }

    private fun getIconDrawableResId(iconName: String): Int {
        return com.rushi.coinmaster.util.IconHelper.getIconDrawableResId(requireContext(), iconName)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
