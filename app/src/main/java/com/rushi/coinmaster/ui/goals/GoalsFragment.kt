package com.rushi.coinmaster.ui.goals

import android.app.AlertDialog
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
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupWithNavController
import com.rushi.coinmaster.MainActivity
import com.rushi.coinmaster.R
import com.rushi.coinmaster.databinding.FragmentGoalsBinding
import com.rushi.coinmaster.util.LocaleHelper
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class GoalsFragment : Fragment() {

    private var _binding: FragmentGoalsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: GoalsViewModel by viewModels()
    private lateinit var adapter: GoalsAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentGoalsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val languageCode = LocaleHelper.getLanguage(requireContext())

        // Setup toolbar with hamburger icon for sidebar navigation
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

        // Setup Adapter
        adapter = GoalsAdapter(
            languageCode = languageCode,
            onEditClick = { goal ->
                val action = GoalsFragmentDirections.actionGoalsFragmentToAddEditGoalFragment(goalId = goal.id)
                findNavController().navigate(action)
            },
            onDeleteClick = { goal ->
                showDeleteConfirmationDialog(goal)
            }
        )
        binding.rvGoals.adapter = adapter

        // Setup FAB
        binding.fabAddGoal.setOnClickListener {
            val action = GoalsFragmentDirections.actionGoalsFragmentToAddEditGoalFragment(goalId = 0L)
            findNavController().navigate(action)
        }

        // Observe reactive goals list
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.goalsList.collect { list ->
                    adapter.submitList(list)
                    if (list.isEmpty()) {
                        binding.layoutEmptyState.visibility = View.VISIBLE
                        binding.rvGoals.visibility = View.GONE
                    } else {
                        binding.layoutEmptyState.visibility = View.GONE
                        binding.rvGoals.visibility = View.VISIBLE
                    }
                }
            }
        }
    }

    private fun showDeleteConfirmationDialog(goal: GoalUiModel) {
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.dialog_delete_goal_title)
            .setMessage(getString(R.string.dialog_delete_goal_message, goal.name))
            .setPositiveButton(R.string.btn_delete) { _, _ ->
                viewModel.deleteGoal(goal.id)
            }
            .setNegativeButton(R.string.btn_cancel, null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
