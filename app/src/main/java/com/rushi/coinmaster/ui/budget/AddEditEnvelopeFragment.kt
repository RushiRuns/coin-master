package com.rushi.coinmaster.ui.budget

import android.app.AlertDialog
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.rushi.coinmaster.R
import com.rushi.coinmaster.data.local.entity.CategoryEntity
import com.rushi.coinmaster.data.local.model.BucketType
import com.rushi.coinmaster.databinding.FragmentAddEditEnvelopeBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class AddEditEnvelopeFragment : Fragment() {

    private var _binding: FragmentAddEditEnvelopeBinding? = null
    private val binding get() = _binding!!

    private val viewModel: BudgetViewModel by activityViewModels()
    private val args: AddEditEnvelopeFragmentArgs by navArgs()

    @javax.inject.Inject
    lateinit var computeBucketSplitUseCase: com.rushi.coinmaster.domain.usecase.ComputeBucketSplitUseCase

    private var selectedColor: String = "#E57373"
    private var selectedIcon: String = "ic_emergency"

    private lateinit var colorViews: List<View>
    private lateinit var iconViews: List<Pair<String, ImageView>>

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAddEditEnvelopeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.toolbar.setNavigationOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }

        setupBucketDropdown()
        setupColorSelection()
        setupIconSelection()

        val isEditMode = args.categoryId != 0L

        // Toggle Advanced Settings (Icon & Color)
        binding.btnToggleAdvanced.setOnClickListener {
            val isCollapsed = binding.layoutAdvancedIconColor.visibility == View.GONE
            binding.layoutAdvancedIconColor.visibility = if (isCollapsed) View.VISIBLE else View.GONE
            binding.btnToggleAdvanced.text = if (isCollapsed) "Hide Color & Icon Settings" else "Customize Color & Icon"
        }

        if (isEditMode) {
            binding.tvTitle.text = "Edit Envelope"
            binding.btnDeleteCategory.visibility = View.VISIBLE
            binding.tilEnvelopeAmount.visibility = View.GONE
            loadCategoryData(args.categoryId)
        } else {
            binding.tvTitle.text = "New Envelope"
            binding.btnDeleteCategory.visibility = View.GONE
            binding.tilEnvelopeAmount.visibility = View.VISIBLE
            
            // Pre-select bucket type if passed
            if (args.bucketTypeOrdinal != -1) {
                val bucketType = BucketType.values()[args.bucketTypeOrdinal]
                binding.actvBucketType.setText(bucketType.name, false)
            }
            highlightSelectedColor()
            highlightSelectedIcon()

            // Update remaining balance hint reactively
            viewLifecycleOwner.lifecycleScope.launch {
                viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                    launch {
                        viewModel.budgetMonthState.collect {
                            updateBucketRemainingHint()
                        }
                    }
                    launch {
                        viewModel.envelopesState.collect {
                            updateBucketRemainingHint()
                        }
                    }
                }
            }

            binding.actvBucketType.setOnItemClickListener { _, _, _, _ ->
                updateBucketRemainingHint()
            }
        }

        // Save Category
        binding.btnSaveCategory.setOnClickListener {
            saveCategory()
        }

        // Delete Category
        binding.btnDeleteCategory.setOnClickListener {
            showDeleteConfirmation()
        }

        // Observe ViewModel Save events
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiEvent.collect { event ->
                    when (event) {
                        is BudgetUiEvent.SuccessSave -> {
                            val msg = if (isEditMode) "Envelope updated!" else "Envelope created!"
                            Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
                            findNavController().popBackStack()
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

    private fun setupBucketDropdown() {
        val buckets = listOf("UNASSIGNED") + BucketType.values().map { it.name }
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, buckets)
        binding.actvBucketType.setAdapter(adapter)
        
        // Default select the first one
        if (buckets.isNotEmpty()) {
            binding.actvBucketType.setText(buckets[0], false)
        }
    }

    private fun setupColorSelection() {
        colorViews = listOf(
            binding.color1, binding.color2, binding.color3, binding.color4,
            binding.color5, binding.color6, binding.color7, binding.color8
        )

        val colors = listOf(
            "#E57373", "#81C784", "#64B5F6", "#FFD54F",
            "#BA68C8", "#4DB6AC", "#4DD0E1", "#FF8A65"
        )

        for (i in colorViews.indices) {
            val colorStr = colors[i]
            val colorView = colorViews[i]

            // Apply solid color circle
            val shape = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.parseColor(colorStr))
            }
            colorView.background = shape

            colorView.setOnClickListener {
                selectedColor = colorStr
                highlightSelectedColor()
            }
        }
    }

    private fun setupIconSelection() {
        iconViews = listOf(
            "ic_rent" to binding.iconRent,
            "ic_groceries" to binding.iconGroceries,
            "ic_utilities" to binding.iconUtilities,
            "ic_dining" to binding.iconDining,
            "ic_entertainment" to binding.iconEntertainment,
            "ic_shopping" to binding.iconShopping,
            "ic_savings" to binding.iconSavings,
            "ic_emergency" to binding.iconEmergency
        )

        for (pair in iconViews) {
            val iconName = pair.first
            val imageView = pair.second

            imageView.setOnClickListener {
                selectedIcon = iconName
                highlightSelectedIcon()
            }
        }
    }

    private fun highlightSelectedColor() {
        val colors = listOf(
            "#E57373", "#81C784", "#64B5F6", "#FFD54F",
            "#BA68C8", "#4DB6AC", "#4DD0E1", "#FF8A65"
        )

        for (i in colorViews.indices) {
            val colorStr = colors[i]
            val colorView = colorViews[i]

            val shape = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.parseColor(colorStr))
                if (colorStr.equals(selectedColor, ignoreCase = true)) {
                    // Draw a strong border if selected
                    setStroke(6, ContextCompat.getColor(requireContext(), R.color.text_primary))
                } else {
                    setStroke(0, Color.TRANSPARENT)
                }
            }
            colorView.background = shape
        }
    }

    private fun highlightSelectedIcon() {
        for (pair in iconViews) {
            val iconName = pair.first
            val imageView = pair.second

            if (iconName == selectedIcon) {
                imageView.setBackgroundResource(R.drawable.bg_allocation_box)
                imageView.setColorFilter(ContextCompat.getColor(requireContext(), R.color.primary))
            } else {
                imageView.background = null
                imageView.setColorFilter(ContextCompat.getColor(requireContext(), R.color.text_secondary))
            }
        }
    }

    private fun loadCategoryData(categoryId: Long) {
        viewLifecycleOwner.lifecycleScope.launch {
            val category = viewModel.getCategoryById(categoryId)
            if (category != null) {
                binding.etEnvelopeName.setText(category.name)
                binding.actvBucketType.setText(category.bucketType?.name ?: "UNASSIGNED", false)
                selectedColor = category.colorHex
                selectedIcon = category.iconName
                highlightSelectedColor()
                highlightSelectedIcon()
            }
        }
    }

    private fun saveCategory() {
        val name = binding.etEnvelopeName.text.toString()
        val bucketStr = binding.actvBucketType.text.toString()
        val bucket = try {
            if (bucketStr == "UNASSIGNED") null else BucketType.valueOf(bucketStr)
        } catch (e: Exception) {
            null
        }

        val amountStr = binding.etEnvelopeAmount.text.toString()
        val initialAllocationPaise = if (amountStr.isNotBlank()) {
            try {
                com.rushi.coinmaster.util.MoneyMath.rupeesToPaise(amountStr)
            } catch (e: Exception) {
                null
            }
        } else {
            null
        }

        viewModel.saveCategory(
            id = args.categoryId,
            name = name,
            bucketType = bucket,
            colorHex = selectedColor,
            iconName = selectedIcon,
            initialAllocationPaise = initialAllocationPaise
        )
    }

    private fun updateBucketRemainingHint() {
        val month = viewModel.budgetMonthState.value ?: return
        val envelopes = viewModel.envelopesState.value
        val bucketStr = binding.actvBucketType.text.toString()
        val selectedBucket = try {
            if (bucketStr == "UNASSIGNED") null else BucketType.valueOf(bucketStr)
        } catch (e: Exception) {
            null
        }

        if (selectedBucket == null) {
            binding.tilEnvelopeAmount.helperText = "No bucket limit (Unassigned)"
            return
        }

        val split = computeBucketSplitUseCase(month.incomePaise, month.needsPercent, month.wantsPercent, month.savingsPercent)
        val targetPaise = when (selectedBucket) {
            BucketType.NEEDS -> split.needsPaise
            BucketType.WANTS -> split.wantsPaise
            BucketType.SAVINGS -> split.savingsPaise
        }

        val allocatedPaise = envelopes.filter { it.bucketType == selectedBucket }.sumOf { it.allocatedAmountPaise }
        val remainingPaise = targetPaise - allocatedPaise

        val languageCode = com.rushi.coinmaster.util.LocaleHelper.getLanguage(requireContext())
        val remainingStr = com.rushi.coinmaster.util.CurrencyFormatter.format(remainingPaise, languageCode)
        binding.tilEnvelopeAmount.helperText = "Limit: $remainingStr remaining in $bucketStr bucket"
    }

    private fun showDeleteConfirmation() {
        AlertDialog.Builder(requireContext())
            .setTitle("Delete Envelope")
            .setMessage("Are you sure you want to delete this envelope? Historical allocations will be preserved, but you won't be able to budget into this envelope in the future.")
            .setPositiveButton("Delete") { _, _ ->
                viewModel.deleteCategory(args.categoryId)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
