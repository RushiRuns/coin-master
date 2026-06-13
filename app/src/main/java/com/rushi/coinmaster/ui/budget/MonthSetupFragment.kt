package com.rushi.coinmaster.ui.budget

import android.app.AlertDialog
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.Spinner
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
import java.text.DateFormatSymbols
import java.util.Calendar
import javax.inject.Inject

@AndroidEntryPoint
class MonthSetupFragment : Fragment() {

    private var _binding: FragmentMonthSetupBinding? = null
    private val binding get() = _binding!!

    private val viewModel: BudgetViewModel by activityViewModels()
    private val args: MonthSetupFragmentArgs by navArgs()

    @Inject
    lateinit var computeBucketSplitUseCase: ComputeBucketSplitUseCase

    private var activeMonthId: Int = 0
    private var activeYear: Int = 2026
    private var activeMonth: Int = 6

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

        binding.toolbar.setNavigationOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }

        // Parse date from arguments
        val calendar = Calendar.getInstance()
        val currentMonthId = calendar.get(Calendar.YEAR) * 100 + (calendar.get(Calendar.MONTH) + 1)
        activeMonthId = if (args.budgetMonthId != 0) args.budgetMonthId else currentMonthId
        activeYear = activeMonthId / 100
        activeMonth = activeMonthId % 100

        updateDateLabel()

        // Prefill values from existing budget month if available
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                val existing = viewModel.budgetMonthState.value
                if (existing != null && existing.id == activeMonthId) {
                    binding.etMonthlyIncome.setText(String.format("%.2f", existing.incomePaise / 100.0))
                    binding.etNeedsPercent.setText(existing.needsPercent.toString())
                    binding.etWantsPercent.setText(existing.wantsPercent.toString())
                    binding.etSavingsPercent.setText(existing.savingsPercent.toString())
                }
                updateSplitCalculations()
            }
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

        // Change Date Action
        binding.btnChangeDate.setOnClickListener {
            showMonthYearPickerDialog()
        }

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
                            viewModel.selectMonth(activeMonthId)
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

    private fun updateDateLabel() {
        val languageCode = LocaleHelper.getLanguage(requireContext())
        val monthName = DateFormatSymbols(java.util.Locale(languageCode)).months[activeMonth - 1]
        binding.tvSelectedDateLabel.text = getString(R.string.label_month_format, monthName, activeYear)
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

    private fun showMonthYearPickerDialog() {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_month_year_picker, null)
        val spinnerMonth = dialogView.findViewById<Spinner>(R.id.spinner_month)
        val etYear = dialogView.findViewById<EditText>(R.id.et_year)

        // Populate Month Spinner
        val languageCode = LocaleHelper.getLanguage(requireContext())
        val months = DateFormatSymbols(java.util.Locale(languageCode)).months
        val spinnerAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, months)
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerMonth.adapter = spinnerAdapter
        spinnerMonth.setSelection(activeMonth - 1)

        etYear.setText(activeYear.toString())

        AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.dialog_select_budget_month))
            .setView(dialogView)
            .setPositiveButton(getString(R.string.btn_select)) { _, _ ->
                val selectedMonth = spinnerMonth.selectedItemPosition + 1
                val selectedYear = etYear.text.toString().toIntOrNull() ?: activeYear
                if (selectedYear in 2000..2100) {
                    activeMonth = selectedMonth
                    activeYear = selectedYear
                    activeMonthId = activeYear * 100 + activeMonth
                    updateDateLabel()
                    updateSplitCalculations()
                } else {
                    Toast.makeText(requireContext(), getString(R.string.error_invalid_year), Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton(getString(R.string.btn_cancel), null)
            .show()
    }

    private fun saveBudgetSetup() {
        val incomeStr = binding.etMonthlyIncome.text.toString()
        val needsStr = binding.etNeedsPercent.text.toString()
        val wantsStr = binding.etWantsPercent.text.toString()
        val savingsStr = binding.etSavingsPercent.text.toString()

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

        viewModel.setupBudgetMonth(
            id = activeMonthId,
            month = activeMonth,
            year = activeYear,
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
