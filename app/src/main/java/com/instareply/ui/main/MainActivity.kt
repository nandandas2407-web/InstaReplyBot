package com.instareply.ui.main

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.instareply.InstaReplyApp
import com.instareply.R
import com.instareply.data.model.Rule
import com.instareply.databinding.ActivityMainBinding
import com.instareply.service.InstaNotificationListener
import com.instareply.ui.rules.RuleEditorActivity
import com.instareply.ui.settings.SettingsActivity
import com.instareply.util.PrefsManager
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var ruleAdapter: RuleAdapter
    private lateinit var prefs: PrefsManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        prefs = PrefsManager(this)
        setSupportActionBar(binding.toolbar)

        setupRecyclerView()
        setupClickListeners()
        observeRules()
        updateServiceStatus()
    }

    override fun onResume() {
        super.onResume()
        updateServiceStatus()
    }

    private fun setupRecyclerView() {
        ruleAdapter = RuleAdapter(
            onRuleClick = { rule ->
                openRuleEditor(rule)
            },
            onRuleToggled = { rule, enabled ->
                toggleRule(rule, enabled)
            },
            onDeleteClick = { rule ->
                confirmDeleteRule(rule)
            }
        )

        binding.recyclerViewRules.apply {
            adapter = ruleAdapter
            layoutManager = LinearLayoutManager(this@MainActivity)
        }
    }

    private fun setupClickListeners() {
        binding.fabAddRule.setOnClickListener {
            openRuleEditor(null)
        }

        binding.btnNotificationAccess.setOnClickListener {
            val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
            startActivity(intent)
        }

        binding.btnAccessibilityAccess.setOnClickListener {
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
            startActivity(intent)
        }

        binding.cardServiceStatus.setOnClickListener {
            updateServiceStatus()
        }

        binding.menuSettings.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        binding.switchMasterToggle.setOnCheckedChangeListener { _, isChecked ->
            prefs.setEnabled(isChecked)
            updateServiceStatus()
        }
    }

    private fun observeRules() {
        val db = InstaReplyApp.instance.database
        lifecycleScope.launch {
            db.ruleDao().getAllRules().collectLatest { rules ->
                ruleAdapter.submitList(rules)
                binding.tvEmptyState.visibility = if (rules.isEmpty()) {
                    android.view.View.VISIBLE
                } else {
                    android.view.View.GONE
                }
            }
        }
    }

    private fun updateServiceStatus() {
        val listener = InstaNotificationListener()
        val isListenerEnabled = listener.isNotificationListenerEnabled()
        val isEnabled = prefs.isEnabled()

        binding.switchMasterToggle.isChecked = isEnabled
        binding.tvServiceStatus.text = when {
            !isListenerEnabled -> "Notification access required"
            !isEnabled -> "Service paused"
            else -> "Active - Listening for messages"
        }
        binding.tvServiceStatus.setTextColor(
            when {
                !isListenerEnabled -> getColor(R.color.status_error)
                !isEnabled -> getColor(R.color.status_warning)
                else -> getColor(R.color.status_active)
            }
        )

        binding.btnNotificationAccess.visibility = if (isListenerEnabled) {
            android.view.View.GONE
        } else {
            android.view.View.VISIBLE
        }
    }

    private fun openRuleEditor(rule: Rule?) {
        val intent = Intent(this, RuleEditorActivity::class.java).apply {
            rule?.let { putExtra("rule_id", it.id) }
        }
        startActivity(intent)
    }

    private fun toggleRule(rule: Rule, enabled: Boolean) {
        lifecycleScope.launch {
            val db = InstaReplyApp.instance.database
            db.ruleDao().updateRule(rule.copy(isEnabled = enabled))
        }
    }

    private fun confirmDeleteRule(rule: Rule) {
        AlertDialog.Builder(this)
            .setTitle("Delete Rule")
            .setMessage("Are you sure you want to delete '${rule.label}'?")
            .setPositiveButton("Delete") { _, _ ->
                lifecycleScope.launch {
                    val db = InstaReplyApp.instance.database
                    db.ruleDao().deleteRule(rule)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
