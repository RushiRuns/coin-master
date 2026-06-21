package com.rushi.coinmaster.ui.budget

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
import com.rushi.coinmaster.R
import com.rushi.coinmaster.data.local.entity.CategoryEntity
import com.rushi.coinmaster.databinding.FragmentManageEnvelopesBinding
import com.rushi.coinmaster.databinding.ItemManageEnvelopeBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import com.rushi.coinmaster.data.local.model.BucketType
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import android.widget.ArrayAdapter
import android.widget.CheckBox
import android.widget.AutoCompleteTextView
import com.google.android.material.textfield.TextInputLayout

@AndroidEntryPoint
class ManageEnvelopesFragment : Fragment() {

    private var _binding: FragmentManageEnvelopesBinding? = null
    private val binding get() = _binding!!

    private val viewModel: BudgetViewModel by activityViewModels()

    private var isSelectionMode = false
    private val selectedEnvelopeIds = mutableSetOf<Long>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentManageEnvelopesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.toolbar.setNavigationOnClickListener {
            if (isSelectionMode) {
                exitSelectionMode()
            } else {
                requireActivity().onBackPressedDispatcher.onBackPressed()
            }
        }

        binding.toolbar.inflateMenu(R.menu.menu_manage_envelopes)
        val colorOnSurface = getColorOnSurface()
        binding.toolbar.menu.findItem(R.id.action_bulk_edit)?.icon?.setTint(colorOnSurface)
        binding.toolbar.menu.findItem(R.id.action_cancel_selection)?.icon?.setTint(colorOnSurface)
        binding.toolbar.navigationIcon?.setTint(colorOnSurface)

        binding.toolbar.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.action_bulk_edit -> {
                    showBulkEditDialog()
                    true
                }
                R.id.action_cancel_selection -> {
                    exitSelectionMode()
                    true
                }
                else -> false
            }
        }

        binding.btnAddEnvelope.setOnClickListener {
            val name = binding.etEnvelopeName.text.toString().trim()
            if (name.isBlank()) {
                Toast.makeText(requireContext(), "Envelope name cannot be empty", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            viewModel.saveCategory(
                id = 0L,
                name = name,
                bucketType = null,
                colorHex = "#9E9E9E",
                iconName = "ic_category"
            )
            binding.etEnvelopeName.setText("") // Clear field for continuous additions
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.allCategoriesState.collectLatest { categories ->
                        renderCategories(categories)
                    }
                }
                launch {
                    viewModel.uiEvent.collect { event ->
                        when (event) {
                            is BudgetUiEvent.SuccessSave -> {
                                Toast.makeText(requireContext(), "Envelope saved!", Toast.LENGTH_SHORT).show()
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

    private fun renderCategories(categories: List<CategoryEntity>) {
        binding.containerEnvelopes.removeAllViews()
        val activeCategories = categories.filter { !it.isDeleted }
        for (category in activeCategories) {
            val itemBinding = ItemManageEnvelopeBinding.inflate(layoutInflater, null, false)
            itemBinding.tvEnvelopeName.text = category.name
            itemBinding.tvEnvelopeBucket.text = category.bucketType?.name ?: "Unassigned"
            itemBinding.viewEnvelopeColor.setBackgroundColor(
                android.graphics.Color.parseColor(category.colorHex)
            )
            itemBinding.ivEnvelopeIcon.setImageResource(getIconDrawableResId(category.iconName))

            val isSelected = selectedEnvelopeIds.contains(category.id)

            if (isSelectionMode) {
                itemBinding.btnEditEnvelope.visibility = View.GONE
                itemBinding.cbSelectEnvelope.visibility = View.VISIBLE
                itemBinding.cbSelectEnvelope.isChecked = isSelected
                itemBinding.root.setBackgroundColor(
                    if (isSelected) {
                        androidx.core.content.ContextCompat.getColor(requireContext(), R.color.selected_item_background)
                    } else {
                        android.graphics.Color.TRANSPARENT
                    }
                )
            } else {
                itemBinding.btnEditEnvelope.visibility = View.VISIBLE
                itemBinding.cbSelectEnvelope.visibility = View.GONE
                itemBinding.root.setBackground(null)
            }

            itemBinding.btnEditEnvelope.setOnClickListener {
                val action = ManageEnvelopesFragmentDirections.actionManageEnvelopesFragmentToAddEditEnvelopeFragment(
                    categoryId = category.id,
                    bucketTypeOrdinal = -1
                )
                findNavController().navigate(action)
            }

            itemBinding.root.setOnLongClickListener {
                if (!isSelectionMode) {
                    enterSelectionMode(category.id)
                }
                true
            }

            itemBinding.root.setOnClickListener {
                if (isSelectionMode) {
                    toggleSelection(category.id)
                }
            }

            itemBinding.cbSelectEnvelope.setOnClickListener {
                toggleSelection(category.id)
            }

            binding.containerEnvelopes.addView(itemBinding.root)
        }
    }

    private fun enterSelectionMode(firstSelectedId: Long) {
        isSelectionMode = true
        selectedEnvelopeIds.clear()
        selectedEnvelopeIds.add(firstSelectedId)
        
        binding.cardQuickCreate.visibility = View.GONE
        
        updateSelectionToolbar()
        viewModel.allCategoriesState.value.let { renderCategories(it) }
    }

    private fun toggleSelection(id: Long) {
        if (selectedEnvelopeIds.contains(id)) {
            selectedEnvelopeIds.remove(id)
            if (selectedEnvelopeIds.isEmpty()) {
                exitSelectionMode()
                return
            }
        } else {
            selectedEnvelopeIds.add(id)
        }
        updateSelectionToolbar()
        viewModel.allCategoriesState.value.let { renderCategories(it) }
    }

    private fun exitSelectionMode() {
        isSelectionMode = false
        selectedEnvelopeIds.clear()
        
        binding.cardQuickCreate.visibility = View.VISIBLE
        
        binding.tvToolbarTitle.text = "Manage Envelopes"
        val navIcon = androidx.core.content.ContextCompat.getDrawable(requireContext(), R.drawable.ic_arrow_back)?.apply {
            setTint(getColorOnSurface())
        }
        binding.toolbar.navigationIcon = navIcon
        binding.toolbar.menu.findItem(R.id.action_bulk_edit)?.isVisible = false
        binding.toolbar.menu.findItem(R.id.action_cancel_selection)?.isVisible = false
        
        viewModel.allCategoriesState.value.let { renderCategories(it) }
    }

    private fun updateSelectionToolbar() {
        binding.tvToolbarTitle.text = "Selected: ${selectedEnvelopeIds.size}"
        binding.toolbar.navigationIcon = null
        binding.toolbar.menu.findItem(R.id.action_bulk_edit)?.isVisible = true
        binding.toolbar.menu.findItem(R.id.action_cancel_selection)?.isVisible = true
    }

    private fun showBulkEditDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_bulk_edit_envelopes, null)
        
        val cbUpdateCategory = dialogView.findViewById<CheckBox>(R.id.cb_update_category)
        val tilParentCategory = dialogView.findViewById<TextInputLayout>(R.id.til_parent_category)
        val actvParentCategory = dialogView.findViewById<AutoCompleteTextView>(R.id.actv_parent_category)
        
        cbUpdateCategory.setOnCheckedChangeListener { _, isChecked ->
            tilParentCategory.isEnabled = isChecked
        }
        
        val list = viewModel.expenseCategoriesState.value
        val categoryNames = listOf("None") + list.map { it.name }
        val categoryAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, categoryNames)
        actvParentCategory.setAdapter(categoryAdapter)
        actvParentCategory.setText("None", false)
        
        MaterialAlertDialogBuilder(requireContext())
            .setView(dialogView)
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Save") { _, _ ->
                val updateCategory = cbUpdateCategory.isChecked
                val parentId = if (updateCategory) {
                    val parentName = actvParentCategory.text.toString()
                    if (parentName == "None") null else list.find { it.name == parentName }?.id
                } else null
                
                viewModel.bulkUpdateCategories(
                    categoryIds = selectedEnvelopeIds.toList(),
                    newBucket = null,
                    updateBucket = false,
                    newParentId = parentId,
                    updateCategory = updateCategory
                )
                exitSelectionMode()
            }
            .show()
    }

    private fun getIconDrawableResId(iconName: String): Int {
        return com.rushi.coinmaster.util.IconHelper.getIconDrawableResId(requireContext(), iconName)
    }

    private fun getColorOnSurface(): Int {
        val typedValue = android.util.TypedValue()
        requireContext().theme.resolveAttribute(com.google.android.material.R.attr.colorOnSurface, typedValue, true)
        return typedValue.data
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
