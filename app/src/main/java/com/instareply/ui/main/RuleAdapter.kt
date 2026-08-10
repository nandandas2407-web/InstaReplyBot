package com.instareply.ui.main

import android.graphics.Paint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.instareply.data.model.Rule
import com.instareply.databinding.ItemRuleBinding

class RuleAdapter(
    private val onRuleClick: (Rule) -> Unit,
    private val onRuleToggled: (Rule, Boolean) -> Unit,
    private val onDeleteClick: (Rule) -> Unit
) : ListAdapter<Rule, RuleAdapter.RuleViewHolder>(RuleDiffCallback()) {

    inner class RuleViewHolder(private val binding: ItemRuleBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(rule: Rule) {
            binding.tvRuleLabel.text = rule.label
            binding.tvPattern.text = "Trigger: ${rule.triggerPattern}"
            binding.tvProvider.text = rule.aiProvider.uppercase()
            binding.switchEnabled.isChecked = rule.isEnabled

            if (!rule.isEnabled) {
                binding.tvRuleLabel.paintFlags = binding.tvRuleLabel.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
            } else {
                binding.tvRuleLabel.paintFlags = binding.tvRuleLabel.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
            }

            binding.root.setOnClickListener { onRuleClick(rule) }
            binding.switchEnabled.setOnCheckedChangeListener { _, isChecked ->
                onRuleToggled(rule, isChecked)
            }
            binding.btnDelete.setOnClickListener { onDeleteClick(rule) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RuleViewHolder {
        val binding = ItemRuleBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return RuleViewHolder(binding)
    }

    override fun onBindViewHolder(holder: RuleViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class RuleDiffCallback : DiffUtil.ItemCallback<Rule>() {
        override fun areItemsTheSame(oldItem: Rule, newItem: Rule) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Rule, newItem: Rule) = oldItem == newItem
    }
}
