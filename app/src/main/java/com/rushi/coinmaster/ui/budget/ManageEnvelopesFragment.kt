package com.rushi.coinmaster.ui.budget

import android.os.Bundle
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
import com.rushi.coinmaster.R
import com.rushi.coinmaster.data.local.entity.CategoryEntity
import com.rushi.coinmaster.databinding.FragmentManageEnvelopesBinding
import com.rushi.coinmaster.databinding.ItemManageEnvelopeBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ManageEnvelopesFragment : Fragment() {

    private var _binding: FragmentManageEnvelopesBinding? = null
    private val binding get() = _binding!!

    private val viewModel: BudgetViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentManageEnvelopesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.toolbar.setNavigationOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }

        binding.btnAddEnvelope.setOnClickListener {
            val name = binding.etEnvelopeName.text.toString().trim()
            if (name.isBlank()) {
                Toast.makeText(requireContext(), "Envelope name cannot be empty", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            viewModel.saveCategory(
                id = 0L,
                name = name,
                bucketType = null,
                colorHex = "#9E9E9E",
                iconName = "ic_category"
            )
            binding.etEnvelopeName.setText("") // Clear field for continuous additions
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.allCategoriesState.collectLatest { categories ->
                        renderCategories(categories)
                    }
                }
                launch {
                    viewModel.uiEvent.collect { event ->
                        when (event) {
                            is BudgetUiEvent.SuccessSave -> {
                                Toast.makeText(requireContext(), "Envelope saved!", Toast.LENGTH_SHORT).show()
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
    }

    private fun renderCategories(categories: List<CategoryEntity>) {
        binding.containerEnvelopes.removeAllViews()
        val activeCategories = categories.filter { !it.isDeleted }
        for (category in activeCategories) {
            val itemBinding = ItemManageEnvelopeBinding.inflate(layoutInflater, null, false)
            itemBinding.tvEnvelopeName.text = category.name
            itemBinding.tvEnvelopeBucket.text = category.bucketType?.name ?: "Unassigned"
            itemBinding.viewEnvelopeColor.setBackgroundColor(
                android.graphics.Color.parseColor(category.colorHex)
            )
            itemBinding.ivEnvelopeIcon.setImageResource(getIconDrawableResId(category.iconName))

            itemBinding.btnEditEnvelope.setOnClickListener {
                val action = ManageEnvelopesFragmentDirections.actionManageEnvelopesFragmentToAddEditEnvelopeFragment(
                    categoryId = category.id,
                    bucketTypeOrdinal = -1
                )
                findNavController().navigate(action)
            }

            binding.containerEnvelopes.addView(itemBinding.root)
        }
    }

    private fun getIconDrawableResId(iconName: String): Int {
        return when (iconName) {
            "ic_rent" -> android.R.drawable.ic_menu_today
            "ic_groceries" -> android.R.drawable.ic_menu_gallery
            "ic_utilities" -> android.R.drawable.ic_menu_info_details
            "ic_dining" -> android.R.drawable.ic_menu_compass
            "ic_entertainment" -> android.R.drawable.ic_menu_slideshow
            "ic_shopping" -> android.R.drawable.ic_menu_view
            "ic_savings" -> android.R.drawable.ic_menu_save
            "ic_emergency" -> android.R.drawable.ic_menu_help
            else -> android.R.drawable.ic_menu_help
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
