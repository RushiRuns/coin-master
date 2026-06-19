package com.rushi.coinmaster.ui.home

import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.rushi.coinmaster.R
import com.rushi.coinmaster.data.local.model.EnvelopeWithAllocation
import com.rushi.coinmaster.databinding.DialogSearchEnvelopesBinding
import com.rushi.coinmaster.databinding.ItemSearchEnvelopeBinding
import com.rushi.coinmaster.util.CurrencyFormatter
import com.rushi.coinmaster.util.LocaleHelper
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class SearchEnvelopesBottomSheetDialogFragment : BottomSheetDialogFragment() {

    private var _binding: DialogSearchEnvelopesBinding? = null
    private val binding get() = _binding!!

    // Share HomeViewModel from the parent HomeFragment
    private val viewModel: HomeViewModel by viewModels({ requireParentFragment() })

    private lateinit var adapter: SearchEnvelopesAdapter
    private var allEnvelopes: List<EnvelopeWithAllocation> = emptyList()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogSearchEnvelopesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupSearchInput()

        // Observe parent ViewModel's envelopes state
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collectLatest { state ->
                    allEnvelopes = state.envelopes
                    filterList(binding.etSearch.text?.toString().orEmpty())
                }
            }
        }
    }

    private fun setupRecyclerView() {
        adapter = SearchEnvelopesAdapter()
        binding.rvSearchResults.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@SearchEnvelopesBottomSheetDialogFragment.adapter
        }
    }

    private fun setupSearchInput() {
        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filterList(s?.toString().orEmpty())
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun filterList(query: String) {
        val filtered = if (query.isBlank()) {
            allEnvelopes
        } else {
            val q = query.trim().lowercase()
            allEnvelopes.filter { env ->
                env.categoryName.lowercase().contains(q)
            }
        }

        adapter.submitList(filtered)

        if (filtered.isEmpty()) {
            binding.tvEmptyState.visibility = View.VISIBLE
            binding.rvSearchResults.visibility = View.GONE
        } else {
            binding.tvEmptyState.visibility = View.GONE
            binding.rvSearchResults.visibility = View.VISIBLE
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    // Inner Search Adapter class
    private class SearchEnvelopesAdapter : ListAdapter<EnvelopeWithAllocation, SearchViewHolder>(DiffCallback) {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SearchViewHolder {
            val itemBinding = ItemSearchEnvelopeBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
            return SearchViewHolder(itemBinding)
        }

        override fun onBindViewHolder(holder: SearchViewHolder, position: Int) {
            holder.bind(getItem(position))
        }

        companion object {
            private val DiffCallback = object : DiffUtil.ItemCallback<EnvelopeWithAllocation>() {
                override fun areItemsTheSame(oldItem: EnvelopeWithAllocation, newItem: EnvelopeWithAllocation): Boolean {
                    return oldItem.categoryId == newItem.categoryId
                }

                override fun areContentsTheSame(oldItem: EnvelopeWithAllocation, newItem: EnvelopeWithAllocation): Boolean {
                    return oldItem == newItem
                }
            }
        }
    }

    private class SearchViewHolder(private val itemBinding: ItemSearchEnvelopeBinding) : RecyclerView.ViewHolder(itemBinding.root) {
        fun bind(envelope: EnvelopeWithAllocation) {
            val context = itemView.context
            val langCode = LocaleHelper.getLanguage(context)

            itemBinding.tvEnvelopeName.text = envelope.categoryName
            itemBinding.tvEnvelopeSpent.text = "Spent: " + CurrencyFormatter.format(envelope.spentAmountPaise, langCode)
            itemBinding.tvEnvelopeAllocated.text = CurrencyFormatter.format(envelope.allocatedAmountPaise, langCode)

            // Setup color badge
            val color = try {
                Color.parseColor(envelope.colorHex)
            } catch (e: Exception) {
                Color.GRAY
            }
            itemBinding.viewEnvelopeColor.setBackgroundColor(color)

            // Icon Setup
            val iconResId = try {
                val resName = envelope.iconName ?: "ic_category"
                context.resources.getIdentifier(resName, "drawable", context.packageName)
            } catch (e: Exception) {
                0
            }
            if (iconResId != 0) {
                itemBinding.ivEnvelopeIcon.setImageResource(iconResId)
            } else {
                itemBinding.ivEnvelopeIcon.setImageResource(R.drawable.ic_category)
            }
        }
    }
}
