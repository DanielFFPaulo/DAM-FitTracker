package pt.ipt.ddam2025.fittrack

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class Login : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_login)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val loginBtn : Button= findViewById(R.id.login_btn)
        loginBtn.setOnClickListener{
            switchActivity()
        }
        val forgotPass = findViewById<Button>(R.id.forgot_pass)
        forgotPass.setOnClickListener{
            switchActivity(true)
        }
    }

    fun switchActivity(canGoBack: Boolean = false){
        val message = "A message"
        val intent = Intent(this, MainActivity::class.java)
        intent.putExtra("MESSAGE1", message)
        startActivity(intent)
        if(!canGoBack){finish()}
    }
}