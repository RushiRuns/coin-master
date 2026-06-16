package com.rushi.coinmaster.ui.income

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
import com.rushi.coinmaster.databinding.FragmentIncomeBinding
import com.rushi.coinmaster.databinding.ItemOnboardingIncomeStreamBinding
import com.rushi.coinmaster.domain.model.IncomeStream
import com.rushi.coinmaster.util.CurrencyFormatter
import com.rushi.coinmaster.util.LocaleHelper
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class IncomeFragment : Fragment() {

    private var _binding: FragmentIncomeBinding? = null
    private val binding get() = _binding!!

    private val viewModel: IncomeViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentIncomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Setup toolbar with navigation drawer support (hamburger icon)
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

        binding.btnAddStream.setOnClickListener {
            addIncomeStreamFromInput()
        }

        // Observe income streams
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.incomeStreams.collectLatest { streams ->
                        renderIncomeStreams(streams)
                    }
                }
                launch {
                    viewModel.totalIncomePaise.collectLatest { totalPaise ->
                        val langCode = LocaleHelper.getLanguage(requireContext())
                        binding.tvTotalIncome.text = CurrencyFormatter.format(totalPaise, langCode)
                    }
                }
            }
        }
    }

    private fun addIncomeStreamFromInput() {
        val name = binding.etStreamName.text?.toString()?.trim() ?: ""
        val amountStr = binding.etStreamAmount.text?.toString()?.trim() ?: ""

        if (name.isEmpty()) {
            binding.tilStreamName.error = "Name cannot be empty"
            return
        }
        binding.tilStreamName.error = null

        val amount = amountStr.toDoubleOrNull()
        if (amount == null || amount <= 0.0) {
            binding.tilStreamAmount.error = "Enter a valid amount greater than 0"
            return
        }
        binding.tilStreamAmount.error = null

        viewModel.addIncomeStream(name, amount)

        binding.etStreamName.text = null
        binding.etStreamAmount.text = null

        Toast.makeText(requireContext(), "Income stream added", Toast.LENGTH_SHORT).show()
    }

    private fun renderIncomeStreams(streams: List<IncomeStream>) {
        val langCode = LocaleHelper.getLanguage(requireContext())
        binding.containerIncomeStreams.removeAllViews()

        val count = streams.size
        binding.tvStreamCount.text = "$count income stream${if (count == 1) "" else "s"}"

        if (streams.isEmpty()) {
            binding.tvEmpty.visibility = View.VISIBLE
        } else {
            binding.tvEmpty.visibility = View.GONE
            for (stream in streams) {
                val itemBinding = ItemOnboardingIncomeStreamBinding.inflate(
                    layoutInflater,
                    binding.containerIncomeStreams,
                    false
                )
                itemBinding.tvStreamName.text = stream.name
                itemBinding.tvStreamAmount.text = CurrencyFormatter.format(stream.amountPaise, langCode) + " / month"

                itemBinding.btnDelete.setOnClickListener {
                    AlertDialog.Builder(requireContext())
                        .setTitle("Delete Income Stream")
                        .setMessage("Remove \"${stream.name}\" from your income sources?")
                        .setPositiveButton("Delete") { _, _ ->
                            viewModel.deleteIncomeStream(stream.id)
                        }
                        .setNegativeButton("Cancel", null)
                        .show()
                }

                binding.containerIncomeStreams.addView(itemBinding.root)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
