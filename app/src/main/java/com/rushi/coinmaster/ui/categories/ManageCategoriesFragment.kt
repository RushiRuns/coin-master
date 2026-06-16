package com.rushi.coinmaster.ui.categories

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.rushi.coinmaster.R
import com.rushi.coinmaster.data.repository.ExpenseCategoryRepository
import com.rushi.coinmaster.databinding.FragmentManageCategoriesBinding
import com.rushi.coinmaster.databinding.ItemExpenseCategoryBinding
import com.rushi.coinmaster.domain.model.ExpenseCategory
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class ManageCategoriesFragment : Fragment() {

    private var _binding: FragmentManageCategoriesBinding? = null
    private val binding get() = _binding!!

    @Inject
    lateinit var expenseCategoryRepository: ExpenseCategoryRepository

    private val colors = listOf(
        "Red" to "#E57373",
        "Green" to "#81C784",
        "Blue" to "#64B5F6",
        "Yellow" to "#FFD54F",
        "Purple" to "#BA68C8",
        "Teal" to "#4DB6AC",
        "Cyan" to "#4DD0E1",
        "Orange" to "#FF8A65"
    )

    private val icons = listOf(
        "Rent" to "ic_rent",
        "Groceries" to "ic_groceries",
        "Utilities" to "ic_utilities",
        "Dining" to "ic_dining",
        "Entertainment" to "ic_entertainment",
        "Shopping" to "ic_shopping",
        "Savings" to "ic_savings",
        "Emergency" to "ic_emergency"
    )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentManageCategoriesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupSpinners()

        binding.btnAddCategory.setOnClickListener {
            addCategoryFromInput()
        }

        viewLifecycleOwner.lifecycleScope.launch {
            expenseCategoryRepository.getExpenseCategoriesFlow().collectLatest { list ->
                renderCategoriesList(list)
            }
        }
    }

    private fun setupSpinners() {
        val colorNames = colors.map { it.first }
        val colorAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, colorNames)
        colorAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spColor.adapter = colorAdapter

        val iconNames = icons.map { it.first }
        val iconAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, iconNames)
        iconAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spIcon.adapter = iconAdapter
    }

    private fun addCategoryFromInput() {
        val name = binding.etCategoryName.text?.toString()?.trim() ?: ""
        if (name.isEmpty()) {
            binding.tilCategoryName.error = "Name cannot be empty"
            return
        }
        binding.tilCategoryName.error = null

        val colorHex = colors[binding.spColor.selectedItemPosition].second
        val iconName = icons[binding.spIcon.selectedItemPosition].second

        val newCategory = ExpenseCategory(
            name = name,
            colorHex = colorHex,
            iconName = iconName
        )

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                expenseCategoryRepository.insertExpenseCategory(newCategory)
                Toast.makeText(requireContext(), "Category added successfully!", Toast.LENGTH_SHORT).show()
                binding.etCategoryName.text = null
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Failed to add category: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun renderCategoriesList(list: List<ExpenseCategory>) {
        binding.llCategories.removeAllViews()
        
        list.forEach { category ->
            val itemBinding = ItemExpenseCategoryBinding.inflate(
                layoutInflater,
                binding.llCategories,
                false
            )
            itemBinding.tvCategoryName.text = category.name
            
            // Set Color Indicator
            try {
                val shape = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    setColor(Color.parseColor(category.colorHex))
                }
                itemBinding.viewColorIndicator.background = shape
            } catch (e: Exception) {
                // Default fallback
            }

            // Set Icon
            val resId = requireContext().resources.getIdentifier(
                category.iconName,
                "drawable",
                requireContext().packageName
            )
            if (resId != 0) {
                itemBinding.ivIcon.setImageResource(resId)
            }

            itemBinding.btnDelete.setOnClickListener {
                viewLifecycleOwner.lifecycleScope.launch {
                    try {
                        expenseCategoryRepository.softDeleteExpenseCategory(category.id)
                        Toast.makeText(requireContext(), "Category deleted", Toast.LENGTH_SHORT).show()
                    } catch (e: Exception) {
                        Toast.makeText(requireContext(), "Error deleting category", Toast.LENGTH_SHORT).show()
                    }
                }
            }

            binding.llCategories.addView(itemBinding.root)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
