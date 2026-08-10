package com.instareply.ui.rules

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.instareply.InstaReplyApp
import com.instareply.R
import com.instareply.data.model.MatchType
import com.instareply.data.model.Rule
import com.instareply.databinding.ActivityRuleEditorBinding
import kotlinx.coroutines.launch

class RuleEditorActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRuleEditorBinding
    private var editingRuleId: Long = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRuleEditorBinding.inflate(layoutInflater)
        setContentView(binding.root)

        editingRuleId = intent.getLongExtra("rule_id", 0)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = if (editingRuleId > 0) "Edit Rule" else "New Rule"

        setupSpinners()
        setupClickListeners()

        if (editingRuleId > 0) {
            loadRule()
        }
    }

    private fun setupSpinners() {
        // Match type spinner
        val matchTypes = MatchType.values().map { it.name }
        binding.spinnerMatchType.adapter = ArrayAdapter(
            this, android.R.layout.simple_dropdown_item_1, matchTypes
        )

        // AI provider spinner
        val providers = listOf("gemini", "openrouter", "nvidia", "openai", "opencode")
        binding.spinnerAiProvider.adapter = ArrayAdapter(
            this, android.R.layout.simple_dropdown_item_1, providers
        )
    }

    private fun setupClickListeners() {
        binding.btnSave.setOnClickListener {
            saveRule()
        }

        binding.btnCancel.setOnClickListener {
            finish()
        }
    }

    private fun loadRule() {
        lifecycleScope.launch {
            val db = InstaReplyApp.instance.database
            val rule = db.ruleDao().getRuleById(editingRuleId)
            rule?.let { populateFields(it) }
        }
    }

    private fun populateFields(rule: Rule) {
        binding.etRuleLabel.setText(rule.label)
        binding.etTriggerPattern.setText(rule.triggerPattern)
        binding.spinnerMatchType.setSelection(MatchType.values().indexOfFirst { it.name == rule.matchType.name })
        binding.etReplyTemplate.setText(rule.replyTemplate)
        binding.switchUseAI.isChecked = rule.useAI

        val providers = listOf("gemini", "openrouter", "nvidia", "openai", "opencode")
        binding.spinnerAiProvider.setSelection(providers.indexOf(rule.aiProvider))

        binding.etDelay.setText(rule.delayMs.toString())
        binding.etMaxReplies.setText(rule.maxRepliesPerDay.toString())
        binding.switchGroups.isChecked = rule.applyToGroups
        binding.etSpecificContacts.setText(rule.specificContacts)
        binding.etIgnoredContacts.setText(rule.ignoredContacts)
        binding.switchEnabled.isChecked = rule.isEnabled
    }

    private fun saveRule() {
        val label = binding.etRuleLabel.text.toString().trim()
        if (label.isEmpty()) {
            Toast.makeText(this, "Please enter a rule name", Toast.LENGTH_SHORT).show()
            return
        }

        val providers = listOf("gemini", "openrouter", "nvidia", "openai", "opencode")

        val rule = Rule(
            id = editingRuleId,
            label = label,
            triggerPattern = binding.etTriggerPattern.text.toString().trim(),
            matchType = MatchType.values()[binding.spinnerMatchType.selectedItemPosition],
            replyTemplate = binding.etReplyTemplate.text.toString().trim(),
            useAI = binding.switchUseAI.isChecked,
            aiProvider = providers[binding.spinnerAiProvider.selectedItemPosition],
            delayMs = binding.etDelay.text.toString().toLongOrNull() ?: 0,
            maxRepliesPerDay = binding.etMaxReplies.text.toString().toIntOrNull() ?: 10,
            applyToGroups = binding.switchGroups.isChecked,
            specificContacts = binding.etSpecificContacts.text.toString().trim(),
            ignoredContacts = binding.etIgnoredContacts.text.toString().trim(),
            isEnabled = binding.switchEnabled.isChecked
        )

        lifecycleScope.launch {
            val db = InstaReplyApp.instance.database
            if (editingRuleId > 0) {
                db.ruleDao().updateRule(rule)
                Toast.makeText(this@RuleEditorActivity, "Rule updated", Toast.LENGTH_SHORT).show()
            } else {
                db.ruleDao().insertRule(rule)
                Toast.makeText(this@RuleEditorActivity, "Rule created", Toast.LENGTH_SHORT).show()
            }
            finish()
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}
