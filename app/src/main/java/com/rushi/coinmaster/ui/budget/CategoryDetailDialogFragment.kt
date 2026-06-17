package com.rushi.coinmaster.ui.budget

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.rushi.coinmaster.R
import com.rushi.coinmaster.data.local.model.BucketType
import com.rushi.coinmaster.databinding.DialogCategoryDetailBinding
import com.rushi.coinmaster.databinding.ItemEnvelopeBinding
import com.rushi.coinmaster.util.CurrencyFormatter
import com.rushi.coinmaster.util.LocaleHelper
import kotlinx.coroutines.launch

class CategoryDetailDialogFragment : DialogFragment() {

    private var _binding: DialogCategoryDetailBinding? = null
    private val binding get() = _binding!!

    private val viewModel: BudgetViewModel by activityViewModels()

    companion object {
        private const val ARG_CATEGORY_ID = "category_id"
        private const val ARG_BUCKET_TYPE_ORDINAL = "bucket_type_ordinal"

        fun newInstance(categoryId: Long, bucketTypeOrdinal: Int): CategoryDetailDialogFragment {
            val fragment = CategoryDetailDialogFragment()
            val args = Bundle().apply {
                putLong(ARG_CATEGORY_ID, categoryId)
                putInt(ARG_BUCKET_TYPE_ORDINAL, bucketTypeOrdinal)
            }
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NO_TITLE, android.R.style.Theme_Material_Light_Dialog_NoActionBar_MinWidth)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogCategoryDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val categoryId = arguments?.getLong(ARG_CATEGORY_ID) ?: -1L
        val bucketTypeOrdinal = arguments?.getInt(ARG_BUCKET_TYPE_ORDINAL) ?: -1
        val bucketType = if (bucketTypeOrdinal != -1) BucketType.values()[bucketTypeOrdinal] else null

        binding.btnClose.setOnClickListener {
            dismiss()
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.groupedCategoriesState.collect { groupedList ->
                    val matchedGroup = groupedList.find { 
                        it.id == categoryId && (bucketType == null || it.bucketType == bucketType)
                    }

                    if (matchedGroup != null) {
                        binding.tvDialogTitle.text = matchedGroup.name
                        val languageCode = LocaleHelper.getLanguage(requireContext())
                        val totalAllocated = CurrencyFormatter.format(matchedGroup.allocatedAmountPaise, languageCode)
                        val totalSpent = CurrencyFormatter.format(matchedGroup.spentAmountPaise, languageCode)
                        binding.tvDialogSubtitle.text = "Total Allocated: $totalAllocated / Spent: $totalSpent"
                        
                        renderEnvelopes(matchedGroup.envelopes)
                    } else {
                        binding.tvDialogTitle.text = "Category Details"
                        binding.tvDialogSubtitle.text = "No envelopes linked to this category."
                        binding.llEnvelopesContainer.removeAllViews()
                    }
                }
            }
        }
    }

    private fun renderEnvelopes(envelopes: List<com.rushi.coinmaster.data.local.model.EnvelopeWithAllocation>) {
        val languageCode = LocaleHelper.getLanguage(requireContext())
        binding.llEnvelopesContainer.removeAllViews()

        for (envelope in envelopes) {
            val itemBinding = ItemEnvelopeBinding.inflate(layoutInflater, binding.llEnvelopesContainer, false)

            itemBinding.tvEnvelopeName.text = envelope.categoryName
            itemBinding.viewEnvelopeColor.setBackgroundColor(Color.parseColor(envelope.colorHex))
            itemBinding.ivEnvelopeIcon.setImageResource(getIconDrawableResId(envelope.iconName))
            itemBinding.tvEnvelopeSpent.text = getString(R.string.text_spent_prefix, CurrencyFormatter.format(envelope.spentAmountPaise, languageCode))
            itemBinding.tvAllocatedAmount.text = CurrencyFormatter.format(envelope.allocatedAmountPaise, languageCode)

            val tvAllocated = itemBinding.tvAllocatedAmount
            val etAllocated = itemBinding.etAllocatedAmount

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
                if (actionId == EditorInfo.IME_ACTION_DONE) {
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

            itemBinding.btnEditEnvelope.setOnClickListener {
                dismiss()
                val navController = findNavController()
                val action = BudgetFragmentDirections.actionBudgetFragmentToAddEditEnvelopeFragment(
                    categoryId = envelope.categoryId,
                    bucketTypeOrdinal = envelope.bucketType.ordinal
                )
                navController.navigate(action)
            }

            binding.llEnvelopesContainer.addView(itemBinding.root)
        }
    }

    private fun showKeyboard(view: View) {
        val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT)
    }

    private fun hideKeyboard(view: View) {
        val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(view.windowToken, 0)
    }

    private fun getIconDrawableResId(iconName: String): Int {
        return com.rushi.coinmaster.util.IconHelper.getIconDrawableResId(requireContext(), iconName)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
