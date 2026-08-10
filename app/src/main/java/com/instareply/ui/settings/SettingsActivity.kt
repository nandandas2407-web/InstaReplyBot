package com.instareply.ui.settings

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.instareply.R
import com.instareply.databinding.ActivitySettingsBinding
import com.instareply.util.PrefsManager

class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding
    private lateinit var prefs: PrefsManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        prefs = PrefsManager(this)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Settings"

        loadSettings()
        setupClickListeners()
    }

    private fun loadSettings() {
        binding.etUserName.setText(prefs.getUserName())
        binding.etUserLocation.setText(prefs.getUserLocation())
        binding.etUserBio.setText(prefs.getUserBio())
        binding.etSystemPrompt.setText(prefs.getSystemPrompt())

        // Load API keys (masked)
        binding.etGeminiKey.setText(prefs.getApiKey("gemini") ?: "")
        binding.etOpenRouterKey.setText(prefs.getApiKey("openrouter") ?: "")
        binding.etNvidiaKey.setText(prefs.getApiKey("nvidia") ?: "")
        binding.etOpenaiKey.setText(prefs.getApiKey("openai") ?: "")
        binding.etOpencodeKey.setText(prefs.getApiKey("opencode") ?: "")

        // Load models
        val modelAdapter = ArrayAdapter.createFromResource(
            this,
            R.array.gemini_models,
            android.R.layout.simple_spinner_dropdown_item
        )
        binding.spinnerGeminiModel.adapter = modelAdapter

        val openrouterAdapter = ArrayAdapter.createFromResource(
            this,
            R.array.openrouter_models,
            android.R.layout.simple_spinner_dropdown_item
        )
        binding.spinnerOpenrouterModel.adapter = openrouterAdapter
    }

    private fun setupClickListeners() {
        binding.btnSave.setOnClickListener {
            saveSettings()
        }

        binding.btnTestGeminiKey.setOnClickListener {
            testApiKey("gemini")
        }

        binding.btnTestOpenrouterKey.setOnClickListener {
            testApiKey("openrouter")
        }

        binding.btnTestNvidiaKey.setOnClickListener {
            testApiKey("nvidia")
        }
    }

    private fun saveSettings() {
        prefs.setUserName(binding.etUserName.text.toString().trim())
        prefs.setUserLocation(binding.etUserLocation.text.toString().trim())
        prefs.setUserBio(binding.etUserBio.text.toString().trim())
        prefs.setSystemPrompt(binding.etSystemPrompt.text.toString().trim())

        // Save API keys
        val geminiKey = binding.etGeminiKey.text.toString().trim()
        if (geminiKey.isNotEmpty()) prefs.setApiKey("gemini", geminiKey)

        val openrouterKey = binding.etOpenRouterKey.text.toString().trim()
        if (openrouterKey.isNotEmpty()) prefs.setApiKey("openrouter", openrouterKey)

        val nvidiaKey = binding.etNvidiaKey.text.toString().trim()
        if (nvidiaKey.isNotEmpty()) prefs.setApiKey("nvidia", nvidiaKey)

        val openaiKey = binding.etOpenaiKey.text.toString().trim()
        if (openaiKey.isNotEmpty()) prefs.setApiKey("openai", openaiKey)

        val opencodeKey = binding.etOpencodeKey.text.toString().trim()
        if (opencodeKey.isNotEmpty()) prefs.setApiKey("opencode", opencodeKey)

        // Save models
        prefs.setModel("gemini", binding.spinnerGeminiModel.selectedItem.toString())
        prefs.setModel("openrouter", binding.spinnerOpenrouterModel.selectedItem.toString())

        Toast.makeText(this, "Settings saved", Toast.LENGTH_SHORT).show()
        finish()
    }

    private fun testApiKey(provider: String) {
        val key = when (provider) {
            "gemini" -> binding.etGeminiKey.text.toString().trim()
            "openrouter" -> binding.etOpenRouterKey.text.toString().trim()
            "nvidia" -> binding.etNvidiaKey.text.toString().trim()
            else -> ""
        }

        if (key.isEmpty()) {
            Toast.makeText(this, "Please enter an API key", Toast.LENGTH_SHORT).show()
            return
        }

        Toast.makeText(this, "Testing $provider API key...", Toast.LENGTH_SHORT).show()
        // TODO: Implement actual API key testing
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}
