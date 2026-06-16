package com.rushi.coinmaster.ui.transfers

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupWithNavController
import com.rushi.coinmaster.MainActivity
import com.rushi.coinmaster.R
import com.rushi.coinmaster.data.local.entity.TransferRecipientEntity
import com.rushi.coinmaster.databinding.FragmentManageTransfersBinding
import com.rushi.coinmaster.databinding.ItemTransferRecipientBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ManageTransfersFragment : Fragment() {

    private var _binding: FragmentManageTransfersBinding? = null
    private val binding get() = _binding!!

    private val viewModel: TransfersViewModel by viewModels()

    private var editingRecipientId: Long = 0L

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentManageTransfersBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.toolbar.setupWithNavController(
            findNavController(),
            AppBarConfiguration(
                setOf(
                    R.id.homeFragment, R.id.transactionsFragment, R.id.budgetFragment,
                    R.id.nav_categories, R.id.nav_expense_type, R.id.nav_income,
                    R.id.nav_savings, R.id.nav_accounts, R.id.nav_notes,
                    R.id.nav_transfers, R.id.nav_goals, R.id.nav_settings
                ),
                (requireActivity() as MainActivity).drawerLayout
            )
        )

        binding.btnSaveRecipient.setOnClickListener {
            saveRecipientFromInput()
        }

        binding.btnCancel.setOnClickListener {
            resetFormState()
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.recipientsState.collectLatest { list ->
                    renderRecipientsList(list)
                }
            }
        }
    }

    private fun saveRecipientFromInput() {
        val name = binding.etRecipientName.text?.toString()?.trim() ?: ""
        val bankDetails = binding.etBankDetails.text?.toString()?.trim() ?: ""
        
        val type = if (binding.rbPerson.isChecked) "PERSON" else "BANK"

        if (name.isEmpty()) {
            binding.tilRecipientName.error = "Name cannot be empty"
            return
        }
        binding.tilRecipientName.error = null

        viewModel.saveRecipient(
            id = editingRecipientId,
            name = name,
            type = type,
            bankDetails = bankDetails.ifEmpty { null }
        )

        Toast.makeText(
            requireContext(),
            if (editingRecipientId == 0L) "Recipient added" else "Recipient updated",
            Toast.LENGTH_SHORT
        ).show()

        resetFormState()
    }

    private fun resetFormState() {
        editingRecipientId = 0L
        binding.etRecipientName.text = null
        binding.etBankDetails.text = null
        binding.rbPerson.isChecked = true
        binding.tvFormTitle.text = "Add Recipient"
        binding.btnCancel.visibility = View.GONE
        binding.tilRecipientName.error = null
    }

    private fun renderRecipientsList(list: List<TransferRecipientEntity>) {
        binding.containerRecipients.removeAllViews()

        if (list.isEmpty()) {
            binding.tvEmptyRecipients.visibility = View.VISIBLE
        } else {
            binding.tvEmptyRecipients.visibility = View.GONE
            for (recipient in list) {
                val itemBinding = ItemTransferRecipientBinding.inflate(layoutInflater, binding.containerRecipients, false)
                itemBinding.tvRecipientName.text = recipient.name
                itemBinding.tvRecipientType.text = recipient.type
                
                if (recipient.bankDetails.isNullOrEmpty()) {
                    itemBinding.tvBankDetails.visibility = View.GONE
                } else {
                    itemBinding.tvBankDetails.visibility = View.VISIBLE
                    itemBinding.tvBankDetails.text = recipient.bankDetails
                }

                itemBinding.btnEdit.setOnClickListener {
                    editingRecipientId = recipient.id
                    binding.etRecipientName.setText(recipient.name)
                    binding.etBankDetails.setText(recipient.bankDetails)
                    if (recipient.type == "PERSON") {
                        binding.rbPerson.isChecked = true
                    } else {
                        binding.rbBank.isChecked = true
                    }
                    binding.tvFormTitle.text = "Edit Recipient"
                    binding.btnCancel.visibility = View.VISIBLE
                    binding.tilRecipientName.error = null
                    binding.etRecipientName.requestFocus()
                }

                itemBinding.btnDelete.setOnClickListener {
                    AlertDialog.Builder(requireContext())
                        .setTitle("Delete Recipient")
                        .setMessage("Are you sure you want to delete this recipient?")
                        .setPositiveButton("Delete") { _, _ ->
                            viewModel.deleteRecipient(recipient.id)
                            Toast.makeText(requireContext(), "Recipient deleted", Toast.LENGTH_SHORT).show()
                            if (editingRecipientId == recipient.id) {
                                resetFormState()
                            }
                        }
                        .setNegativeButton("Cancel", null)
                        .show()
                }

                binding.containerRecipients.addView(itemBinding.root)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
