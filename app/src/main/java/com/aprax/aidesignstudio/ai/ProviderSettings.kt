package com.aprax.aidesignstudio.ai

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import org.json.JSONArray
import org.json.JSONObject

class ProviderSettings(context: Context) {
    private val prefs = EncryptedSharedPreferences.create(
        context,
        "designora_secure_settings",
        MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build(),
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    var provider: String
        get() = prefs.getString("provider", "Gemini") ?: "Gemini"
        set(value) = prefs.edit().putString("provider", value).apply()

    var geminiKey: String
        get() = prefs.getString("gemini_key", "") ?: ""
        set(value) = prefs.edit().putString("gemini_key", value.trim()).apply()

    var openRouterKey: String
        get() = prefs.getString("openrouter_key", "") ?: ""
        set(value) = prefs.edit().putString("openrouter_key", value.trim()).apply()

    var model: String
        get() = prefs.getString("model", "gemini-2.0-flash") ?: "gemini-2.0-flash"
        set(value) = prefs.edit().putString("model", value).apply()

    /** User-managed AI instructions. Stored locally and included in every design-generation request. */
    var customInstructions: String
        get() = prefs.getString("custom_ai_instructions", "") ?: ""
        set(value) = prefs.edit().putString("custom_ai_instructions", value).apply()
}
