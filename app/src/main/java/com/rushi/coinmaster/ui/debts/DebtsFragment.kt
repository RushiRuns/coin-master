package com.rushi.coinmaster.ui.debts

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.tabs.TabLayout
import com.rushi.coinmaster.databinding.FragmentDebtsBinding
import com.rushi.coinmaster.util.CurrencyFormatter
import com.rushi.coinmaster.util.LocaleHelper
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class DebtsFragment : Fragment() {

    private var _binding: FragmentDebtsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: DebtsViewModel by viewModels()
    private lateinit var adapter: DebtsAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDebtsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.toolbar.setNavigationOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }

        setupRecyclerView()
        setupTabLayout()
        setupFAB()

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { uiState ->
                    val languageCode = LocaleHelper.getLanguage(requireContext())

                    // Update summary header totals
                    binding.tvTotalLent.text = CurrencyFormatter.format(uiState.totalLentPaise, languageCode)
                    binding.tvTotalBorrowed.text = CurrencyFormatter.format(uiState.totalBorrowedPaise, languageCode)

                    // Show active or settled list based on selected tab index
                    val isSettledTab = binding.tabLayout.selectedTabPosition == 1
                    updateList(uiState, isSettledTab)
                }
            }
        }
    }

    private fun setupRecyclerView() {
        adapter = DebtsAdapter { debtId ->
            val action = DebtsFragmentDirections.actionDebtsFragmentToDebtDetailsFragment(debtId)
            findNavController().navigate(action)
        }
        binding.rvDebts.layoutManager = LinearLayoutManager(requireContext())
        binding.rvDebts.adapter = adapter
    }

    private fun setupTabLayout() {
        binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                val state = viewModel.uiState.value
                val isSettledTab = tab?.position == 1
                updateList(state, isSettledTab)
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
    }

    private fun updateList(state: DebtsUiState, isSettledTab: Boolean) {
        val list = if (isSettledTab) {
            binding.layoutSummary.visibility = View.GONE
            state.settledDebts
        } else {
            binding.layoutSummary.visibility = View.VISIBLE
            state.activeDebts
        }

        adapter.submitList(list)

        if (list.isEmpty()) {
            binding.tvEmptyState.visibility = View.VISIBLE
            binding.rvDebts.visibility = View.GONE
            binding.tvEmptyState.text = if (isSettledTab) {
                "No settled loans or debts."
            } else {
                "No active loans or debts."
            }
        } else {
            binding.tvEmptyState.visibility = View.GONE
            binding.rvDebts.visibility = View.VISIBLE
        }
    }

    private fun setupFAB() {
        binding.fabAddDebt.setOnClickListener {
            val action = DebtsFragmentDirections.actionDebtsFragmentToAddEditDebtFragment(0L)
            findNavController().navigate(action)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
