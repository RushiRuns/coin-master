package com.rushi.coinmaster.ui.budget

import android.app.DatePickerDialog
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
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
import androidx.navigation.fragment.navArgs
import com.rushi.coinmaster.R
import com.rushi.coinmaster.databinding.FragmentMonthSetupBinding
import com.rushi.coinmaster.domain.usecase.ComputeBucketSplitUseCase
import com.rushi.coinmaster.util.CurrencyFormatter
import com.rushi.coinmaster.util.LocaleHelper
import com.rushi.coinmaster.util.MoneyMath
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import javax.inject.Inject

@AndroidEntryPoint
class BudgetPeriodSetupFragment : Fragment() {

    private var _binding: FragmentMonthSetupBinding? = null
    private val binding get() = _binding!!

    private val viewModel: BudgetViewModel by activityViewModels()
    private val args: BudgetPeriodSetupFragmentArgs by navArgs()

    @Inject
    lateinit var computeBucketSplitUseCase: ComputeBucketSplitUseCase

    private var activePeriodId: Int = 0
    private var startDateVal: Long = 0L
    private var endDateVal: Long = 0L

    private val dateFormatter = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMonthSetupBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.toolbar.title = "Budget Setup"
        binding.toolbar.setNavigationOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }

        activePeriodId = args.budgetPeriodId

        // Observe values from existing budget period if available
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                val existing = viewModel.budgetPeriodState.value
                if (existing != null && existing.id == activePeriodId) {
                    startDateVal = existing.startDate
                    endDateVal = existing.endDate
                    binding.etMonthlyIncome.setText(String.format("%.2f", existing.incomePaise / 100.0))
                    binding.etNeedsPercent.setText(existing.needsPercent.toString())
                    binding.etWantsPercent.setText(existing.wantsPercent.toString())
                    binding.etSavingsPercent.setText(existing.savingsPercent.toString())
                    updateDateViews()
                } else {
                    // Seed default dates starting today and ending 1 month later if not initialized
                    if (startDateVal == 0L) {
                        val calendar = Calendar.getInstance()
                        calendar.set(Calendar.HOUR_OF_DAY, 0)
                        calendar.set(Calendar.MINUTE, 0)
                        calendar.set(Calendar.SECOND, 0)
                        calendar.set(Calendar.MILLISECOND, 0)
                        startDateVal = calendar.timeInMillis

                        calendar.add(Calendar.MONTH, 1)
                        calendar.add(Calendar.DAY_OF_YEAR, -1)
                        calendar.set(Calendar.HOUR_OF_DAY, 23)
                        calendar.set(Calendar.MINUTE, 59)
                        calendar.set(Calendar.SECOND, 59)
                        calendar.set(Calendar.MILLISECOND, 999)
                        endDateVal = calendar.timeInMillis
                        updateDateViews()
                    }
                }
                updateSplitCalculations()
            }
        }

        // Date selection click listeners
        binding.btnSelectStartDate.setOnClickListener {
            showDatePicker(true)
        }

        binding.btnSelectEndDate.setOnClickListener {
            showDatePicker(false)
        }

        // Setup Text Change Listeners for real-time split calculations
        val textWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                updateSplitCalculations()
            }
        }
        binding.etMonthlyIncome.addTextChangedListener(textWatcher)
        binding.etNeedsPercent.addTextChangedListener(textWatcher)
        binding.etWantsPercent.addTextChangedListener(textWatcher)
        binding.etSavingsPercent.addTextChangedListener(textWatcher)

        // Save Setup Action
        binding.btnSaveSetup.setOnClickListener {
            saveBudgetSetup()
        }

        // Listen for save success/error events
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiEvent.collect { event ->
                    when (event) {
                        is BudgetUiEvent.SuccessSave -> {
                            Toast.makeText(requireContext(), "Budget setup saved successfully!", Toast.LENGTH_SHORT).show()
                            viewModel.selectPeriod(viewModel.selectedPeriodId.value ?: activePeriodId)
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

    private fun updateDateViews() {
        binding.tvStartDate.text = dateFormatter.format(startDateVal)
        binding.tvEndDate.text = dateFormatter.format(endDateVal)
    }

    private fun showDatePicker(isStartDate: Boolean) {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = if (isStartDate) startDateVal else endDateVal

        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        DatePickerDialog(requireContext(), { _, selectedYear, selectedMonth, selectedDay ->
            val resultCal = Calendar.getInstance()
            resultCal.set(Calendar.YEAR, selectedYear)
            resultCal.set(Calendar.MONTH, selectedMonth)
            resultCal.set(Calendar.DAY_OF_MONTH, selectedDay)

            if (isStartDate) {
                resultCal.set(Calendar.HOUR_OF_DAY, 0)
                resultCal.set(Calendar.MINUTE, 0)
                resultCal.set(Calendar.SECOND, 0)
                resultCal.set(Calendar.MILLISECOND, 0)
                startDateVal = resultCal.timeInMillis
            } else {
                resultCal.set(Calendar.HOUR_OF_DAY, 23)
                resultCal.set(Calendar.MINUTE, 59)
                resultCal.set(Calendar.SECOND, 59)
                resultCal.set(Calendar.MILLISECOND, 999)
                endDateVal = resultCal.timeInMillis
            }
            updateDateViews()
        }, year, month, day).show()
    }

    private fun updateSplitCalculations() {
        val incomeStr = binding.etMonthlyIncome.text.toString()
        val needsStr = binding.etNeedsPercent.text.toString()
        val wantsStr = binding.etWantsPercent.text.toString()
        val savingsStr = binding.etSavingsPercent.text.toString()

        val incomePaise = try {
            MoneyMath.rupeesToPaise(incomeStr)
        } catch (e: Exception) {
            0L
        }

        val needsP = needsStr.toIntOrNull() ?: 0
        val wantsP = wantsStr.toIntOrNull() ?: 0
        val savingsP = savingsStr.toIntOrNull() ?: 0

        val total = needsP + wantsP + savingsP
        binding.tvPercentageSum.text = getString(R.string.label_total_percent_format, total)
        if (total == 100) {
            binding.tvPercentageSum.setTextColor(resources.getColor(android.R.color.holo_green_dark, null))
        } else {
            binding.tvPercentageSum.setTextColor(resources.getColor(android.R.color.holo_red_dark, null))
        }

        val split = computeBucketSplitUseCase(incomePaise, needsP, wantsP, savingsP)
        val languageCode = LocaleHelper.getLanguage(requireContext())
        binding.tvNeedsTargetVal.text = CurrencyFormatter.format(split.needsPaise, languageCode)
        binding.tvWantsTargetVal.text = CurrencyFormatter.format(split.wantsPaise, languageCode)
        binding.tvSavingsTargetVal.text = CurrencyFormatter.format(split.savingsPaise, languageCode)
    }

    private fun saveBudgetSetup() {
        val incomeStr = binding.etMonthlyIncome.text.toString()
        val needsStr = binding.etNeedsPercent.text.toString()
        val wantsStr = binding.etWantsPercent.text.toString()
        val savingsStr = binding.etSavingsPercent.text.toString()

        if (startDateVal > endDateVal) {
            Toast.makeText(requireContext(), "Start date must be before or equal to End date.", Toast.LENGTH_SHORT).show()
            return
        }

        if (incomeStr.isBlank()) {
            Toast.makeText(requireContext(), getString(R.string.error_income_empty), Toast.LENGTH_SHORT).show()
            return
        }

        val needsP = needsStr.toIntOrNull() ?: 0
        val wantsP = wantsStr.toIntOrNull() ?: 0
        val savingsP = savingsStr.toIntOrNull() ?: 0

        if (needsP + wantsP + savingsP != 100) {
            Toast.makeText(requireContext(), getString(R.string.error_percentages_total), Toast.LENGTH_LONG).show()
            return
        }

        viewModel.setupBudgetPeriod(
            id = activePeriodId,
            startDate = startDateVal,
            endDate = endDateVal,
            incomeStr = incomeStr,
            needsPercent = needsP,
            wantsPercent = wantsP,
            savingsPercent = savingsP
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
