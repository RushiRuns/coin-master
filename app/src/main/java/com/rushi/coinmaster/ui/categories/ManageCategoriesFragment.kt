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
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupWithNavController
import com.rushi.coinmaster.MainActivity
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
        "Rent" to "ic_home",
        "Groceries" to "ic_shopping_cart",
        "Utilities" to "ic_bolt",
        "Dining" to "ic_restaurant",
        "Entertainment" to "ic_movie",
        "Shopping" to "ic_shopping_cart",
        "Savings" to "ic_savings",
        "Emergency" to "ic_local_hospital",
        "Car" to "ic_directions_car",
        "School" to "ic_school",
        "Flight" to "ic_flight",
        "Favorite" to "ic_favorite",
        "Phone" to "ic_phone",
        "Gift" to "ic_card_giftcard",
        "Work" to "ic_work",
        "Coffee" to "ic_coffee"
    )

    private var editingCategory: ExpenseCategory? = null

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

        val navController = findNavController()
        val appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.homeFragment, R.id.transactionsFragment, R.id.budgetFragment,
                R.id.nav_categories, R.id.nav_expense_type, R.id.nav_income,
                R.id.nav_savings, R.id.nav_accounts, R.id.nav_notes,
                R.id.nav_transfers, R.id.nav_goals, R.id.nav_settings
            ),
            (requireActivity() as MainActivity).drawerLayout
        )
        binding.toolbar.setupWithNavController(navController, appBarConfiguration)

        setupSpinners()

        binding.btnAddCategory.setOnClickListener {
            addCategoryFromInput()
        }

        binding.btnCancelEdit.setOnClickListener {
            resetForm()
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

        val editCat = editingCategory
        if (editCat != null) {
            val updated = editCat.copy(
                name = name,
                colorHex = colorHex,
                iconName = iconName
            )
            viewLifecycleOwner.lifecycleScope.launch {
                try {
                    expenseCategoryRepository.updateExpenseCategory(updated)
                    Toast.makeText(requireContext(), "Category updated successfully!", Toast.LENGTH_SHORT).show()
                    resetForm()
                } catch (e: Exception) {
                    Toast.makeText(requireContext(), "Failed to update category: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        } else {
            val newCategory = ExpenseCategory(
                name = name,
                colorHex = colorHex,
                iconName = iconName
            )
            viewLifecycleOwner.lifecycleScope.launch {
                try {
                    expenseCategoryRepository.insertExpenseCategory(newCategory)
                    Toast.makeText(requireContext(), "Category added successfully!", Toast.LENGTH_SHORT).show()
                    resetForm()
                } catch (e: Exception) {
                    Toast.makeText(requireContext(), "Failed to add category: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun startEditingCategory(category: ExpenseCategory) {
        editingCategory = category
        binding.etCategoryName.setText(category.name)
        
        val colorIndex = colors.indexOfFirst { it.second.equals(category.colorHex, ignoreCase = true) }
        if (colorIndex != -1) {
            binding.spColor.setSelection(colorIndex)
        }

        val iconIndex = icons.indexOfFirst { it.second == category.iconName }
        if (iconIndex != -1) {
            binding.spIcon.setSelection(iconIndex)
        }

        binding.tvFormHeader.text = "Edit Category"
        binding.btnAddCategory.text = "Update Category"
        binding.btnCancelEdit.visibility = View.VISIBLE
    }

    private fun resetForm() {
        binding.etCategoryName.text = null
        binding.spColor.setSelection(0)
        binding.spIcon.setSelection(0)
        binding.tvFormHeader.text = "Create Category"
        binding.btnAddCategory.text = "Add Category"
        binding.btnCancelEdit.visibility = View.GONE
        editingCategory = null
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
 
            // Set Icon using IconHelper to correctly resolve standard drawable resource IDs
            itemBinding.ivIcon.setImageResource(
                com.rushi.coinmaster.util.IconHelper.getIconDrawableResId(requireContext(), category.iconName)
            )

            itemBinding.btnEdit.setOnClickListener {
                startEditingCategory(category)
            }

            itemBinding.btnDelete.setOnClickListener {
                viewLifecycleOwner.lifecycleScope.launch {
                    try {
                        expenseCategoryRepository.softDeleteExpenseCategory(category.id)
                        Toast.makeText(requireContext(), "Category deleted", Toast.LENGTH_SHORT).show()
                        if (editingCategory?.id == category.id) {
                            resetForm()
                        }
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
