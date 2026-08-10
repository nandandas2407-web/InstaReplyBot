package com.instareply.ui.main

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.instareply.InstaReplyApp
import com.instareply.R
import com.instareply.data.model.Rule
import com.instareply.databinding.ActivityMainBinding
import com.instareply.service.InstaAccessibilityService
import com.instareply.service.InstaNotificationListener
import com.instareply.ui.rules.RuleEditorActivity
import com.instareply.ui.settings.SettingsActivity
import com.instareply.util.PrefsManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.Calendar

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var ruleAdapter: RuleAdapter
    private lateinit var prefs: PrefsManager

    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        prefs = PrefsManager(this)
        setSupportActionBar(binding.toolbar)

        requestNotificationPermissionIfNeeded()
        setupRecyclerView()
        setupClickListeners()
        observeRules()
        updateServiceStatus()
        startStatsRefresh()
    }

    override fun onResume() {
        super.onResume()
        updateServiceStatus()
        lifecycleScope.launch { refreshStats() }
    }

    override fun onDestroy() {
        super.onDestroy()
        binding.chartWeekly.clearAnimation()
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    private fun setupRecyclerView() {
        ruleAdapter = RuleAdapter(
            onRuleClick = { rule -> openRuleEditor(rule) },
            onRuleToggled = { rule, enabled -> toggleRule(rule, enabled) },
            onDeleteClick = { rule -> confirmDeleteRule(rule) }
        )

        binding.recyclerViewRules.apply {
            adapter = ruleAdapter
            layoutManager = LinearLayoutManager(this@MainActivity)
        }
    }

    private fun setupClickListeners() {
        binding.fabAddRule.setOnClickListener { openRuleEditor(null) }

        binding.btnNotificationAccess.setOnClickListener {
            startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
        }

        binding.btnAccessibilityAccess.setOnClickListener {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }

        binding.cardServiceStatus.setOnClickListener { updateServiceStatus() }

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
                binding.tvEmptyState.visibility = if (rules.isEmpty()) View.VISIBLE else View.GONE
                binding.tvStatRules.text = rules.count { it.isEnabled }.toString()
            }
        }
    }

    private fun startStatsRefresh() {
        lifecycleScope.launch {
            while (isActive) {
                refreshStats()
                delay(60_000)
            }
        }
    }

    private suspend fun refreshStats() {
        try {
            val db = InstaReplyApp.instance.database
            val dayMs = 24 * 60 * 60 * 1000L
            val todayStart = startOfToday()
            val weekStart = todayStart - 6 * dayMs

            val totalReplies = db.contactDao().getTotalReplies() ?: 0
            val totalContacts = db.contactDao().getTotalContacts()
            val activeRules = db.ruleDao().getEnabledRuleCount()
            val todayReplies = db.replyLogDao().getReplyCountSince(todayStart)

            binding.tvStatReplies.text = totalReplies.toString()
            binding.tvStatContacts.text = totalContacts.toString()
            binding.tvStatRules.text = activeRules.toString()
            binding.tvStatToday.text = todayReplies.toString()

            val counts = IntArray(7)
            db.replyLogDao().getSuccessTimestampsSince(weekStart).forEach { ts ->
                val index = ((ts - weekStart) / dayMs).toInt()
                if (index in 0..6) counts[index]++
            }
            binding.chartWeekly.setData(counts)
        } catch (t: Throwable) {
            // ignore stats refresh errors - they should never crash the dashboard
        }
    }

    private fun startOfToday(): Long {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }

    private fun updateServiceStatus() {
        val isListenerEnabled = InstaNotificationListener.isNotificationListenerEnabled(this)
        val isAccessibilityEnabled = InstaAccessibilityService.isAccessibilityServiceEnabled(this)
        val isEnabled = prefs.isEnabled()

        binding.switchMasterToggle.isChecked = isEnabled
        binding.tvServiceStatus.text = when {
            !isListenerEnabled -> "Notification access required"
            !isAccessibilityEnabled -> "Accessibility service required"
            !isEnabled -> "Service paused"
            else -> "Active — Listening for messages"
        }
        binding.tvServiceStatus.setTextColor(
            when {
                !isListenerEnabled -> getColor(R.color.status_error)
                !isAccessibilityEnabled -> getColor(R.color.status_warning)
                !isEnabled -> getColor(R.color.status_warning)
                else -> getColor(R.color.status_active)
            }
        )

        binding.btnNotificationAccess.visibility = if (isListenerEnabled) View.GONE else View.VISIBLE
        binding.btnAccessibilityAccess.visibility = if (isAccessibilityEnabled) View.GONE else View.VISIBLE
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