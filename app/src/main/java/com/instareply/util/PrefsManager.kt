package com.instareply.util

import android.content.Context
import android.content.SharedPreferences

class PrefsManager(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("instareply_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_ENABLED = "enabled"
        private const val KEY_USER_NAME = "user_name"
        private const val KEY_USER_LOCATION = "user_location"
        private const val KEY_USER_BIO = "user_bio"
        private const val KEY_SYSTEM_PROMPT = "system_prompt"
        private const val KEY_API_KEY_PREFIX = "api_key_"
        private const val KEY_MODEL_PREFIX = "model_"
    }

    fun isEnabled(): Boolean = prefs.getBoolean(KEY_ENABLED, false)

    fun setEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_ENABLED, enabled).apply()
    }

    fun getUserName(): String = prefs.getString(KEY_USER_NAME, "") ?: ""

    fun setUserName(name: String) {
        prefs.edit().putString(KEY_USER_NAME, name).apply()
    }

    fun getUserLocation(): String = prefs.getString(KEY_USER_LOCATION, "") ?: ""

    fun setUserLocation(location: String) {
        prefs.edit().putString(KEY_USER_LOCATION, location).apply()
    }

    fun getUserBio(): String = prefs.getString(KEY_USER_BIO, "") ?: ""

    fun setUserBio(bio: String) {
        prefs.edit().putString(KEY_USER_BIO, bio).apply()
    }

    fun getSystemPrompt(): String = prefs.getString(KEY_SYSTEM_PROMPT, "") ?: ""

    fun setSystemPrompt(prompt: String) {
        prefs.edit().putString(KEY_SYSTEM_PROMPT, prompt).apply()
    }

    fun getApiKey(provider: String): String? {
        return prefs.getString("$KEY_API_KEY_PREFIX$provider", null)
    }

    fun setApiKey(provider: String, key: String) {
        prefs.edit().putString("$KEY_API_KEY_PREFIX$provider", key).apply()
    }

    fun getModel(provider: String): String {
        return prefs.getString("$KEY_MODEL_PREFIX$provider", "") ?: ""
    }

    fun setModel(provider: String, model: String) {
        prefs.edit().putString("$KEY_MODEL_PREFIX$provider", model).apply()
    }

    fun hasApiKey(provider: String): Boolean {
        return getApiKey(provider)?.isNotEmpty() == true
    }

    fun getAllApiKeys(): Map<String, String> {
        val providers = listOf("gemini", "openrouter", "nvidia", "openai", "opencode")
        return providers.associateWith { provider ->
            getApiKey(provider) ?: ""
        }
    }
}
