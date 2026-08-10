package com.instareply.ui.settings

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.instareply.R
import com.instareply.databinding.ActivitySettingsBinding
import com.instareply.util.PrefsManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

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
        binding.etMaxTokens.setText(prefs.getMaxTokens().toString())

        // Load API keys (masked)
        binding.etGeminiKey.setText(prefs.getApiKey("gemini") ?: "")
        binding.etOpenRouterKey.setText(prefs.getApiKey("openrouter") ?: "")
        binding.etNvidiaKey.setText(prefs.getApiKey("nvidia") ?: "")
        binding.etOpenaiKey.setText(prefs.getApiKey("openai") ?: "")
        binding.etOpencodeKey.setText(prefs.getApiKey("opencode") ?: "")
        binding.etGroqKey.setText(prefs.getApiKey("groq") ?: "")

        // Load model spinners with saved selection
        setupSpinner(
            binding.spinnerGeminiModel,
            R.array.gemini_models,
            prefs.getModel("gemini"),
            arrayOf("gemini-3.6-flash")
        )
        setupSpinner(
            binding.spinnerOpenrouterModel,
            R.array.openrouter_models,
            prefs.getModel("openrouter"),
            arrayOf("nvidia/nemotron-3-super-120b-a12b:free")
        )
        setupSpinner(
            binding.spinnerNvidiaModel,
            R.array.nvidia_models,
            prefs.getModel("nvidia"),
            arrayOf("nvidia/llama-3.3-nemotron-super-49b-v1.5")
        )
        setupSpinner(
            binding.spinnerOpenaiModel,
            R.array.openai_models,
            prefs.getModel("openai"),
            arrayOf("gpt-5.4-mini")
        )
        setupSpinner(
            binding.spinnerOpencodeModel,
            R.array.opencode_models,
            prefs.getModel("opencode"),
            arrayOf("gpt-5.6-luna")
        )
        setupSpinner(
            binding.spinnerGroqModel,
            R.array.groq_models,
            prefs.getModel("groq"),
            arrayOf("llama-3.3-70b-versatile")
        )
    }

    private fun setupSpinner(
        spinner: android.widget.Spinner,
        arrayRes: Int,
        savedModel: String,
        defaultModels: Array<String>
    ) {
        val models = resources.getStringArray(arrayRes)
        spinner.adapter = ArrayAdapter(
            this, android.R.layout.simple_spinner_dropdown_item, models
        )
        val index = models.indexOf(savedModel)
        spinner.setSelection(if (index >= 0) index else 0)
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

        binding.btnTestOpenaiKey.setOnClickListener {
            testApiKey("openai")
        }

        binding.btnTestOpencodeKey.setOnClickListener {
            testApiKey("opencode")
        }

        binding.btnTestGroqKey.setOnClickListener {
            testApiKey("groq")
        }
    }

    private fun saveSettings() {
        prefs.setUserName(binding.etUserName.text.toString().trim())
        prefs.setUserLocation(binding.etUserLocation.text.toString().trim())
        prefs.setUserBio(binding.etUserBio.text.toString().trim())
        prefs.setSystemPrompt(binding.etSystemPrompt.text.toString().trim())

        // Save API keys
        binding.etGeminiKey.text.toString().trim().let { if (it.isNotEmpty()) prefs.setApiKey("gemini", it) }
        binding.etOpenRouterKey.text.toString().trim().let { if (it.isNotEmpty()) prefs.setApiKey("openrouter", it) }
        binding.etNvidiaKey.text.toString().trim().let { if (it.isNotEmpty()) prefs.setApiKey("nvidia", it) }
        binding.etOpenaiKey.text.toString().trim().let { if (it.isNotEmpty()) prefs.setApiKey("openai", it) }
        binding.etOpencodeKey.text.toString().trim().let { if (it.isNotEmpty()) prefs.setApiKey("opencode", it) }
        binding.etGroqKey.text.toString().trim().let { if (it.isNotEmpty()) prefs.setApiKey("groq", it) }

        // Save models
        prefs.setModel("gemini", binding.spinnerGeminiModel.selectedItem.toString())
        prefs.setModel("openrouter", binding.spinnerOpenrouterModel.selectedItem.toString())
        prefs.setModel("nvidia", binding.spinnerNvidiaModel.selectedItem.toString())
        prefs.setModel("openai", binding.spinnerOpenaiModel.selectedItem.toString())
        prefs.setModel("opencode", binding.spinnerOpencodeModel.selectedItem.toString())
        prefs.setModel("groq", binding.spinnerGroqModel.selectedItem.toString())

        val maxTokens = binding.etMaxTokens.text.toString().trim().toIntOrNull()
        if (maxTokens != null && maxTokens in 1..8192) {
            prefs.setMaxTokens(maxTokens)
        }

        Toast.makeText(this, "Settings saved", Toast.LENGTH_SHORT).show()
        finish()
    }

    private fun testApiKey(provider: String) {
        val key = when (provider) {
            "gemini" -> binding.etGeminiKey.text.toString().trim()
            "openrouter" -> binding.etOpenRouterKey.text.toString().trim()
            "nvidia" -> binding.etNvidiaKey.text.toString().trim()
            "openai" -> binding.etOpenaiKey.text.toString().trim()
            "opencode" -> binding.etOpencodeKey.text.toString().trim()
            "groq" -> binding.etGroqKey.text.toString().trim()
            else -> ""
        }

        if (key.isEmpty()) {
            Toast.makeText(this, "Please enter an API key", Toast.LENGTH_SHORT).show()
            return
        }

        Toast.makeText(this, "Testing $provider API key...", Toast.LENGTH_SHORT).show()
        lifecycleScope.launch {
            val valid = withContext(Dispatchers.IO) { verifyKey(provider, key) }
            val message = if (valid) "$provider API key is valid" else "$provider API key is invalid or rate-limited"
            Toast.makeText(this@SettingsActivity, message, Toast.LENGTH_SHORT).show()
        }
    }

    private fun verifyKey(provider: String, key: String): Boolean {
        return try {
            val url = when (provider) {
                "gemini" -> "https://generativelanguage.googleapis.com/v1beta/models?key=$key"
                "openrouter" -> "https://openrouter.ai/api/v1/auth/key"
                "nvidia" -> "https://integrate.api.nvidia.com/v1/models"
                "openai" -> "https://api.openai.com/v1/models"
                "opencode" -> "https://opencode.ai/zen/v1/models"
                "groq" -> "https://api.groq.com/openai/v1/models"
                else -> return false
            }
            val builder = Request.Builder().url(url)
            if (provider != "gemini") {
                builder.addHeader("Authorization", "Bearer $key")
            }
            val client = OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(15, TimeUnit.SECONDS)
                .build()
            client.newCall(builder.build()).execute().use { it.isSuccessful }
        } catch (e: Exception) {
            false
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}