package pt.ipt.ddam2025.fittrack.api

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat.startActivity
import org.json.JSONObject
import pt.ipt.ddam2025.fittrack.MainActivity

class AppSession(private val context: Context) {

    private val prefs = UserPreferences(context)

    // Session data (in-memory, fast access)
    var username: String = ""
        private set
    var email: String = ""
        private set
    var fullName: String = ""
        private set
    var weight: Double = 0.0
        private set
    var height: Double = 0.0
        private set
    var gender: String = ""
        private set

    // Persistent data (survives app restart)
    val token: String
        get() = prefs.token

    val userId: Int
        get() = prefs.userId

    fun isLoggedIn(): Boolean = prefs.isLoggedIn()

    fun getAuthHeader(): String = prefs.getAuthHeader()

    // Save login data
    fun saveLoginData(token: String, userId: Int) {
        prefs.token = token
        prefs.userId = userId
    }

    // Load user profile (call after login or on app start)
    suspend fun loadUserProfile() {
        try {
            // Get user details
            val userJson = API.callApi(
                "https://damapi2026.onrender.com/api/user/me",
                token,
                "GET"
            )

            val userDetails = JSONObject(userJson)
            username = userDetails.getString("username")
            email = userDetails.getString("email")

            // Get profile
            val profileJson = API.callApi(
                "https://damapi2026.onrender.com/api/profile",
                token,
                "GET"
            )

            val profile = JSONObject(profileJson)
            fullName = profile.getString("full_name")
            weight = profile.getString("weight").toDouble()
            height = profile.getString("height").toDouble()
            gender = profile.getString("gender")

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun logout() {
        prefs.clear()
        username = ""
        email = ""
        fullName = ""
        weight = 0.0
        height = 0.0
        gender = ""
        val profileJson = API.callApi(
            "https://damapi2026.onrender.com/api/logout",
            token,
            "POST",
            mapOf("" to "")
        )


    }

    companion object {
        @Volatile
        private var instance: AppSession? = null

        fun getInstance(context: Context): AppSession {
            return instance ?: synchronized(this) {
                instance ?: AppSession(context.applicationContext).also { instance = it }
            }
        }
    }
}
