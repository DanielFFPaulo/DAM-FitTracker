package pt.ipt.ddam2025.fittrack.api

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.google.gson.Gson
import com.google.gson.JsonObject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import pt.ipt.ddam2025.fittrack.MainActivity
import pt.ipt.ddam2025.fittrack.R

class Login : AppCompatActivity() {
    private lateinit var appSession: AppSession

    private lateinit var usernameField: EditText
    private lateinit var passwdField: EditText

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_login)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        appSession = AppSession.getInstance(this)

        // Check if already logged in
        if (appSession.isLoggedIn()) {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
            return
        }

        usernameField = findViewById(R.id.usernameField)
        passwdField = findViewById(R.id.passwdField)


        val loginBtn : Button= findViewById(R.id.login_btn)
        loginBtn.setOnClickListener{
            login()
        }
        val registerPage : Button = findViewById<Button>(R.id.register_btn)
        registerPage.setOnClickListener{
            goToMain(true)
        }
    }

    fun goToMain(canGoBack: Boolean = false){
        val message = "A message"
        val intent = Intent(this, Register::class.java)
        intent.putExtra("MESSAGE1", message)
        startActivity(intent)
        if(!canGoBack){finish()}
    }

    fun login(){
        val apiUrl = "https://damapi2026.onrender.com/api/login"
        val token = ""

        if (usernameField.text.toString().trim().isEmpty() || passwdField.text.toString().trim().isEmpty()) {
            Toast.makeText(this, "Please enter username and password", Toast.LENGTH_SHORT).show()
            return
        }
        val requestBody = mapOf(
            "username" to usernameField.text.toString().trim(),
            "password" to passwdField.text.toString().trim()
        )



        // Launch coroutine on IO dispatcher for network operation
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = API.callApi(apiUrl, token, "POST", requestBody)

                // Switch to Main dispatcher to update UI
                withContext(Dispatchers.Main) {
                    onLoginSuccess(response)
                    // Handle successful response here
                    // e.g., goToMain()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    println("Error: ${e.message}")
                    // Handle error here
                }
            }
        }
    }

    fun onLoginSuccess(jsonResponse: String) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                // First check if it's an error
                if (jsonResponse.contains("Exception") || jsonResponse.contains("Error")) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@Login, "Login failed: $jsonResponse", Toast.LENGTH_LONG).show()
                    }
                    return@launch
                }

                val json = JSONObject(jsonResponse)

                // Check if response has error field
                if (json.has("error")) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@Login, "Error: ${json.getString("error")}", Toast.LENGTH_LONG).show()
                    }
                    return@launch
                }

                // Save token and user_id (persistent)
                appSession.saveLoginData(
                    json.getString("token"),
                    json.getInt("user_id")
                )

                // Load profile data (in-memory)
                appSession.loadUserProfile()

                withContext(Dispatchers.Main) {
                    Toast.makeText(this@Login, "Welcome ${appSession.fullName}!", Toast.LENGTH_SHORT).show()
                    startActivity(Intent(this@Login, MainActivity::class.java))
                    finish()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@Login, "Parse error: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}