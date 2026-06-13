package com.rushi.coinmaster.ui.goals

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.rushi.coinmaster.R
import com.rushi.coinmaster.databinding.FragmentAddEditGoalBinding
import com.rushi.coinmaster.util.CurrencyFormatter
import com.rushi.coinmaster.util.LocaleHelper
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.util.Calendar

@AndroidEntryPoint
class AddEditGoalFragment : Fragment() {

    private var _binding: FragmentAddEditGoalBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AddEditGoalViewModel by viewModels()
    private val args: AddEditGoalFragmentArgs by navArgs()

    private var selectedTargetDateMillis: Long = 0L

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAddEditGoalBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.toolbar.setNavigationOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }

        val isEditing = args.goalId > 0L

        // Update header text based on mode
        binding.tvTitle.text = if (isEditing)
            getString(R.string.title_edit_goal)
        else
            getString(R.string.title_add_goal)

        if (isEditing) {
            viewModel.loadGoal(args.goalId)
        }

        // Target Date picker
        binding.etTargetDate.setOnClickListener { showMonthYearPicker() }

        // Auto-create category checkbox toggle
        binding.cbAutoCreateCategory.setOnCheckedChangeListener { _, isChecked ->
            binding.layoutSelectCategory.visibility = if (isChecked) View.GONE else View.VISIBLE
        }

        // Cancel button
        binding.btnCancel.setOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }

        // Save button
        binding.btnSave.setOnClickListener {
            submitGoal()
        }

        // Observe the savings categories for the spinner
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.savingsCategories.collect { categories ->
                        val names = categories.map { it.name }
                        val adapter = ArrayAdapter(
                            requireContext(),
                            android.R.layout.simple_spinner_item,
                            names
                        ).apply {
                            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                        }
                        binding.spinnerCategory.adapter = adapter
                    }
                }

                launch {
                    viewModel.goalState.collect { fund ->
                        if (fund != null) {
                            binding.etGoalName.setText(fund.name)
                            binding.etTargetAmount.setText(String.format("%.2f", fund.targetAmountPaise / 100.0))
                            selectedTargetDateMillis = fund.targetDate
                            binding.etTargetDate.setText(
                                java.text.SimpleDateFormat("MMMM yyyy", java.util.Locale.getDefault())
                                    .format(java.util.Date(fund.targetDate))
                            )
                            // For editing, try to match the existing category
                            binding.cbAutoCreateCategory.isChecked = false
                            binding.layoutSelectCategory.visibility = View.VISIBLE
                            val catList = viewModel.savingsCategories.value
                            val idx = catList.indexOfFirst { it.id == fund.categoryId }
                            if (idx >= 0) binding.spinnerCategory.setSelection(idx)
                        }
                    }
                }

                launch {
                    viewModel.eventFlow.collect { event ->
                        when (event) {
                            is AddEditGoalEvent.Success -> {
                                Toast.makeText(
                                    requireContext(),
                                    getString(R.string.text_goal_saved),
                                    Toast.LENGTH_SHORT
                                ).show()
                                requireActivity().onBackPressedDispatcher.onBackPressed()
                            }
                            is AddEditGoalEvent.Error -> {
                                Toast.makeText(requireContext(), event.message, Toast.LENGTH_LONG).show()
                            }
                        }
                    }
                }
            }
        }
    }

    private fun showMonthYearPicker() {
        val cal = Calendar.getInstance()
        if (selectedTargetDateMillis > 0L) {
            cal.timeInMillis = selectedTargetDateMillis
        }

        DatePickerDialog(
            requireContext(),
            { _, year, month, _ ->
                val pickedCal = Calendar.getInstance()
                pickedCal.set(year, month, 1, 0, 0, 0)
                pickedCal.set(Calendar.MILLISECOND, 0)
                selectedTargetDateMillis = pickedCal.timeInMillis

                val fmt = java.text.SimpleDateFormat("MMMM yyyy", java.util.Locale.getDefault())
                binding.etTargetDate.setText(fmt.format(java.util.Date(selectedTargetDateMillis)))
            },
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun submitGoal() {
        val name = binding.etGoalName.text?.toString() ?: ""
        val amountStr = binding.etTargetAmount.text?.toString() ?: ""
        val autoCreate = binding.cbAutoCreateCategory.isChecked

        val amountPaise = try {
            (amountStr.toDouble() * 100).toLong()
        } catch (e: NumberFormatException) {
            Toast.makeText(requireContext(), getString(R.string.error_invalid_amount), Toast.LENGTH_SHORT).show()
            return
        }

        if (selectedTargetDateMillis == 0L) {
            Toast.makeText(requireContext(), getString(R.string.error_target_date_required), Toast.LENGTH_SHORT).show()
            return
        }

        val selectedCategoryId: Long? = if (!autoCreate) {
            val idx = binding.spinnerCategory.selectedItemPosition
            val cats = viewModel.savingsCategories.value
            if (idx >= 0 && idx < cats.size) cats[idx].id else null
        } else null

        val langCode = LocaleHelper.getLanguage(requireContext())

        viewModel.saveGoal(
            id = args.goalId,
            name = name,
            targetAmountPaise = amountPaise,
            targetDate = selectedTargetDateMillis,
            autoCreateCategory = autoCreate,
            selectedCategoryId = selectedCategoryId
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
