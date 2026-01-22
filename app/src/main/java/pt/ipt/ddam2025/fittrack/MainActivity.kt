// Define o package da aplicação
package pt.ipt.ddam2025.fittrack

// Classe base para activities com suporte a AppCompat
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

// Fragment que contém o sistema de navegação
import androidx.navigation.fragment.NavHostFragment

// Função que liga a navegação ao BottomNavigationView
import androidx.navigation.ui.setupWithNavController

// Componente de menu inferior (bottom navigation)
import com.google.android.material.bottomnavigation.BottomNavigationView

// Activity principal da aplicação
class MainActivity : AppCompatActivity() {

    // Método chamado quando a activity é criada
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Define o layout principal da aplicação
        setContentView(R.layout.activity_main)

        // Obtém o NavHostFragment que gere a navegação entre fragments
        val navHost = supportFragmentManager
            .findFragmentById(R.id.nav_host) as NavHostFragment

        // Obtém o controlador de navegação
        val navController = navHost.navController

        // Obtém o menu inferior (BottomNavigationView)
        val bottom = findViewById<BottomNavigationView>(R.id.bottomNav)

        // Liga o menu inferior ao sistema de navegação
        bottom.setupWithNavController(navController)
    }
}
