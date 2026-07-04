package com.rushi.coinmaster.ui.analysis

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.rushi.coinmaster.data.local.model.BucketType
import com.rushi.coinmaster.databinding.FragmentEnvelopeAnalysisBottomSheetBinding
import com.rushi.coinmaster.ui.budget.BudgetViewModel
import com.rushi.coinmaster.util.CurrencyFormatter
import com.rushi.coinmaster.util.DateFormatter
import com.rushi.coinmaster.util.LocaleHelper
import kotlinx.coroutines.launch

class EnvelopeAnalysisBottomSheet : BottomSheetDialogFragment() {

    private var _binding: FragmentEnvelopeAnalysisBottomSheetBinding? = null
    private val binding get() = _binding!!

    private val viewModel: BudgetViewModel by activityViewModels()
    private lateinit var adapter: EnvelopeAnalysisAdapter

    companion object {
        private const val ARG_CATEGORY_ID = "category_id"
        private const val ARG_BUCKET_TYPE_ORDINAL = "bucket_type_ordinal"

        fun newInstance(categoryId: Long, bucketTypeOrdinal: Int): EnvelopeAnalysisBottomSheet {
            val fragment = EnvelopeAnalysisBottomSheet()
            val args = Bundle().apply {
                putLong(ARG_CATEGORY_ID, categoryId)
                putInt(ARG_BUCKET_TYPE_ORDINAL, bucketTypeOrdinal)
            }
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentEnvelopeAnalysisBottomSheetBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val categoryId = arguments?.getLong(ARG_CATEGORY_ID) ?: -1L
        val bucketTypeOrdinal = arguments?.getInt(ARG_BUCKET_TYPE_ORDINAL) ?: -1
        val bucketType = if (bucketTypeOrdinal != -1) BucketType.values()[bucketTypeOrdinal] else null

        setupRecyclerView()

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.groupedCategoriesState.collect { groups ->
                    val group = groups.find {
                        it.id == categoryId && (bucketType == null || it.bucketType == bucketType)
                    }

                    if (group != null) {
                        val context = requireContext()
                        val languageCode = LocaleHelper.getLanguage(context)

                        binding.tvDialogTitle.text = "${group.name} Envelopes"
                        binding.tvDialogSubtitle.text = "Total Spent: ${CurrencyFormatter.format(group.spentAmountPaise, languageCode)} / Allocated: ${CurrencyFormatter.format(group.allocatedAmountPaise, languageCode)}"

                        adapter.submitList(group.envelopes)
                    } else {
                        dismiss()
                    }
                }
            }
        }
    }

    private fun setupRecyclerView() {
        adapter = EnvelopeAnalysisAdapter { envelope ->
            val period = viewModel.budgetPeriodState.value
            if (period != null) {
                val context = requireContext()
                val languageCode = LocaleHelper.getLanguage(context)

                // Dismiss bottom sheet before navigating to ensure smooth transition
                dismiss()

                val action = BudgetAnalysisFragmentDirections.actionAnalysisFragmentToTransactionsFragment(
                    envelopeIds = longArrayOf(envelope.categoryId),
                    startMillis = period.startDate,
                    endMillis = period.endDate,
                    filterLabel = "${envelope.categoryName} · ${DateFormatter.formatMonthYear(period.startDate, languageCode)}"
                )
                findNavController().navigate(action)
            }
        }

        binding.rvEnvelopes.layoutManager = LinearLayoutManager(requireContext())
        binding.rvEnvelopes.adapter = adapter
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
