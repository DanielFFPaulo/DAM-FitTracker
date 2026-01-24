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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import pt.ipt.ddam2025.fittrack.MainActivity
import pt.ipt.ddam2025.fittrack.R

class Register : AppCompatActivity() {

    private lateinit var usernameField: EditText
    private lateinit var passwdField: EditText
    private lateinit var emailField: EditText
    private lateinit var fullNameField: EditText
    private lateinit var weightField: EditText
    private lateinit var heightField: EditText
    private lateinit var dobField: EditText
    private lateinit var registerButton: Button
private val gender = "M"
    private lateinit var appSession: AppSession


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_register)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        appSession = AppSession.getInstance(this)

        usernameField   = findViewById(R.id.username_register)
        passwdField     = findViewById(R.id.passwd_register)
        emailField      = findViewById(R.id.email_register)
        fullNameField   = findViewById(R.id.full_name_register)
        weightField     = findViewById(R.id.weight_register)
        heightField     = findViewById(R.id.height_register)
        dobField        = findViewById(R.id.dob_register)

        registerButton = findViewById(R.id.register_acc_btn)
        registerButton.setOnClickListener {
            register()

        }
    }




private fun register() {
    val apiUrl = "https://damapi2026.onrender.com/api/register"
    val token = ""

    if (usernameField.text.toString().trim().isEmpty() || passwdField.text.toString().trim().isEmpty() || emailField.text.toString().trim().isEmpty() ||
        fullNameField.text.toString().trim().isEmpty() || weightField.text.toString().trim().isEmpty() || heightField.text.toString().trim().isEmpty() ||
        dobField.text.toString().trim().isEmpty()) {
        Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
        return
    }

    if(dobField.text.toString().trim().split("-").size != 3){
        Toast.makeText(this, "Please enter a valid date of birth", Toast.LENGTH_SHORT).show()
        return
    }

    val requestBody = mapOf(
        "username"      to usernameField.text.toString().trim(),
        "password"      to passwdField.text.toString().trim(),
        "email"         to emailField.text.toString().trim(),
        "full_name"     to fullNameField.text.toString().trim(),
        "date_of_birth" to dobField.text.toString().replace("/", "-"),
        "weight"        to weightField.text.toString().trim().toDouble(),
        "height"        to heightField.text.toString().trim().toDouble(),
        "gender"        to "M"
    )


    // Launch coroutine on IO dispatcher for network operation
    CoroutineScope(Dispatchers.IO).launch {
        try {
            val response = API.callApi(apiUrl, token, "POST", requestBody)

            // Switch to Main dispatcher to update UI
            withContext(Dispatchers.Main) {
                onRegistrySuccess(response)
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

fun onRegistrySuccess(jsonResponse: String) {
    lifecycleScope.launch(Dispatchers.IO) {
        try {
            // Verificar se a resposta contém "Exception" ou "Error"
            if (jsonResponse.contains("Exception") || jsonResponse.contains("Error")) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@Register, "Registry failed: $jsonResponse", Toast.LENGTH_LONG).show()
                }
                return@launch
            }

            val json = JSONObject(jsonResponse)

            // verificar se a resposta tem um campo "error"
            if (json.has("error")) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@Register, "Error: ${json.getString("error")}", Toast.LENGTH_LONG).show()
                }
                return@launch
            }

            withContext(Dispatchers.Main) {
                Toast.makeText(this@Register, "Account Created", Toast.LENGTH_SHORT).show()
                startActivity(Intent(this@Register, Login::class.java))
                finish()
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                Toast.makeText(this@Register, "Parse error: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }
}}