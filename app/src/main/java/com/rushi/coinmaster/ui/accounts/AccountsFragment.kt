package com.rushi.coinmaster.ui.accounts

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
import com.google.android.material.snackbar.Snackbar
import com.rushi.coinmaster.R
import com.rushi.coinmaster.databinding.FragmentAccountsBinding
import com.rushi.coinmaster.util.CurrencyFormatter
import com.rushi.coinmaster.util.LocaleHelper
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class AccountsFragment : Fragment() {

    private var _binding: FragmentAccountsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AccountsViewModel by viewModels()
    private lateinit var adapter: AccountsAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAccountsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Setup RecyclerView Adapter
        adapter = AccountsAdapter(
            onEditClick = { accountId ->
                val action = AccountsFragmentDirections.actionAccountsFragmentToAddEditAccountFragment(accountId)
                findNavController().navigate(action)
            },
            onDeleteClick = { account ->
                viewModel.deleteAccount(account)
                Snackbar.make(binding.root, "Account deleted: ${account.name}", Snackbar.LENGTH_LONG)
                    .setAction("UNDO") {
                        viewModel.restoreAccount(account)
                    }
                    .setActionTextColor(resources.getColor(R.color.secondary, null))
                    .show()
            }
        )
        binding.rvAccounts.adapter = adapter

        // Setup FAB Click
        binding.fabAddAccount.setOnClickListener {
            val action = AccountsFragmentDirections.actionAccountsFragmentToAddEditAccountFragment(0L)
            findNavController().navigate(action)
        }

        // Collect State Flow
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    adapter.submitList(state.accounts)
                    
                    val languageCode = LocaleHelper.getLanguage(requireContext())
                    binding.tvNetWorth.text = CurrencyFormatter.format(state.netWorth, languageCode)
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
