package pt.ipt.ddam2025.fittrack.api

import android.content.Context
import android.content.SharedPreferences

class UserPreferences(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_TOKEN = "token"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_USERNAME = "username"
        private const val KEY_EMAIL = "email"
        private const val KEY_FULL_NAME = "full_name"
        private const val KEY_WEIGHT = "weight"
        private const val KEY_HEIGHT = "height"
        private const val KEY_GENDER = "gender"
    }

    // Token
    var token: String
        get() = prefs.getString(KEY_TOKEN, "") ?: ""
        set(value) = prefs.edit().putString(KEY_TOKEN, value).apply()

    // User ID
    var userId: Int
        get() = prefs.getInt(KEY_USER_ID, -1)
        set(value) = prefs.edit().putInt(KEY_USER_ID, value).apply()

    // Username
    var username: String
        get() = prefs.getString(KEY_USERNAME, "") ?: ""
        set(value) = prefs.edit().putString(KEY_USERNAME, value).apply()

    // Email
    var email: String
        get() = prefs.getString(KEY_EMAIL, "") ?: ""
        set(value) = prefs.edit().putString(KEY_EMAIL, value).apply()

    // Full Name
    var fullName: String
        get() = prefs.getString(KEY_FULL_NAME, "") ?: ""
        set(value) = prefs.edit().putString(KEY_FULL_NAME, value).apply()

    // Weight
    var weight: Double
        get() = prefs.getString(KEY_WEIGHT, "0.0")?.toDoubleOrNull() ?: 0.0
        set(value) = prefs.edit().putString(KEY_WEIGHT, value.toString()).apply()

    // Height
    var height: Double
        get() = prefs.getString(KEY_HEIGHT, "0.0")?.toDoubleOrNull() ?: 0.0
        set(value) = prefs.edit().putString(KEY_HEIGHT, value.toString()).apply()

    // Gender
    var gender: String
        get() = prefs.getString(KEY_GENDER, "") ?: ""
        set(value) = prefs.edit().putString(KEY_GENDER, value).apply()

    fun isLoggedIn(): Boolean {
        return token.isNotEmpty() && userId != -1
    }

    fun getAuthHeader(): String {
        return "Bearer $token"
    }

    fun clear() {
        prefs.edit().clear().apply()
    }
}