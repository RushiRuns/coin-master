package com.rushi.coinmaster.ui.goals

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.rushi.coinmaster.R
import com.rushi.coinmaster.databinding.ItemGoalBinding
import com.rushi.coinmaster.util.CurrencyFormatter
import com.rushi.coinmaster.util.DateFormatter

class GoalsAdapter(
    private val languageCode: String,
    private val onEditClick: (GoalUiModel) -> Unit,
    private val onDeleteClick: (GoalUiModel) -> Unit
) : ListAdapter<GoalUiModel, GoalsAdapter.GoalViewHolder>(GoalDiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GoalViewHolder {
        val binding = ItemGoalBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return GoalViewHolder(binding)
    }

    override fun onBindViewHolder(holder: GoalViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class GoalViewHolder(
        private val binding: ItemGoalBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(goal: GoalUiModel) {
            val context = binding.root.context
            binding.tvGoalName.text = goal.name
            binding.tvGoalCategory.text = context.getString(R.string.label_envelope_prefix, goal.categoryName)
            binding.tvGoalSaved.text = CurrencyFormatter.format(goal.savedAmountPaise, languageCode)
            binding.tvGoalTarget.text = context.getString(R.string.label_goal_target, CurrencyFormatter.format(goal.targetAmountPaise, languageCode))
            binding.tvGoalDuration.text = context.getString(R.string.label_goal_target_date, DateFormatter.formatMonthYear(goal.targetDate, languageCode))
            binding.tvGoalPercent.text = "${goal.percent}%"
            binding.progressGoal.progress = goal.percent

            val formattedNeeded = CurrencyFormatter.format(goal.monthlySavingsNeededPaise, languageCode)
            binding.tvMonthlySavingsNeeded.text = context.getString(R.string.label_monthly_savings_needed, formattedNeeded)

            binding.btnEditGoal.setOnClickListener {
                onEditClick(goal)
            }

            binding.btnDeleteGoal.setOnClickListener {
                onDeleteClick(goal)
            }
        }
    }

    companion object GoalDiffCallback : DiffUtil.ItemCallback<GoalUiModel>() {
        override fun areItemsTheSame(oldItem: GoalUiModel, newItem: GoalUiModel): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: GoalUiModel, newItem: GoalUiModel): Boolean {
            return oldItem == newItem
        }
    }
}
