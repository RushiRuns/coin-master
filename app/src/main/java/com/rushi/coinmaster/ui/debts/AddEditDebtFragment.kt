package com.rushi.coinmaster.ui.debts

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
import com.rushi.coinmaster.R
import com.rushi.coinmaster.data.local.entity.AccountEntity
import com.rushi.coinmaster.data.local.model.DebtType
import com.rushi.coinmaster.databinding.FragmentAddEditDebtBinding
import com.rushi.coinmaster.util.DateFormatter
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.util.Calendar

@AndroidEntryPoint
class AddEditDebtFragment : Fragment() {

    private var _binding: FragmentAddEditDebtBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AddEditDebtViewModel by viewModels()

    private var selectedDueDateMillis: Long? = null
    private var accountsList: List<AccountEntity> = emptyList()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAddEditDebtBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.toolbar.setNavigationOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }

        setupDatePicker()

        // Observe Accounts and Events
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.accountsState.collect { accounts ->
                        accountsList = accounts
                        val accountNames = accounts.map { it.name }
                        val accountAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, accountNames)
                        binding.actvAccount.setAdapter(accountAdapter)

                        if (accountNames.isNotEmpty() && binding.actvAccount.text.isEmpty()) {
                            binding.actvAccount.setText(accountNames[0], false)
                        }
                    }
                }

                launch {
                    viewModel.uiEvent.collect { event ->
                        when (event) {
                            is DebtUiEvent.Success -> {
                                Toast.makeText(requireContext(), "Debt saved successfully", Toast.LENGTH_SHORT).show()
                                findNavController().popBackStack()
                            }
                            is DebtUiEvent.Error -> {
                                Toast.makeText(requireContext(), event.message, Toast.LENGTH_LONG).show()
                            }
                        }
                    }
                }
            }
        }

        binding.btnSave.setOnClickListener {
            saveDebt()
        }
    }

    private fun setupDatePicker() {
        binding.etDueDate.setOnClickListener {
            showDatePicker()
        }
    }

    private fun showDatePicker() {
        val calendar = Calendar.getInstance()
        selectedDueDateMillis?.let { calendar.timeInMillis = it }
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        android.app.DatePickerDialog(
            requireContext(),
            { _, y, m, d ->
                val selectedCalendar = Calendar.getInstance().apply {
                    set(Calendar.YEAR, y)
                    set(Calendar.MONTH, m)
                    set(Calendar.DAY_OF_MONTH, d)
                }
                selectedDueDateMillis = selectedCalendar.timeInMillis
                binding.etDueDate.setText(DateFormatter.formatDate(selectedCalendar.timeInMillis))
            },
            year,
            month,
            day
        ).show()
    }

    private fun saveDebt() {
        val personName = binding.etPersonName.text.toString()
        
        val type = if (binding.toggleDebtType.checkedButtonId == R.id.btn_lent) {
            DebtType.LENT
        } else {
            DebtType.BORROWED
        }

        val amountStr = binding.etAmount.text.toString()

        val selectedAccountName = binding.actvAccount.text.toString()
        val accountId = accountsList.find { it.name == selectedAccountName }?.id ?: 0L

        val note = binding.etNote.text.toString()

        viewModel.saveDebt(
            personName = personName,
            type = type,
            amountStr = amountStr,
            accountId = accountId,
            dueDate = selectedDueDateMillis,
            note = note
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
