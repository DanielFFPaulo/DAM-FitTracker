// Define o package onde este fragment está localizado
package pt.ipt.ddam2025.fittrack

// Classes base do Android
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role.Companion.Image

// Fragmento da interface
import androidx.fragment.app.Fragment

// RecyclerView para listas
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import pt.ipt.ddam2025.fittrack.api.AppSession
import pt.ipt.ddam2025.fittrack.api.Login

// Base de dados da aplicação
import pt.ipt.ddam2025.fittrack.data.FitTrackDatabase

// Adapter responsável por mostrar os treinos
import pt.ipt.ddam2025.fittrack.ui.WorkoutAdapter

// Fragment responsável por mostrar o histórico de treinos
class HistoryFragment : Fragment() {

    private lateinit var appSession: AppSession
    // RecyclerView que vai mostrar a lista de treinos
    private lateinit var rvHistory: RecyclerView

    // Adapter que liga os dados (Workouts) à RecyclerView
    private lateinit var adapter: WorkoutAdapter

    // Cria e devolve a vista associada a este fragment
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View =
        inflater.inflate(R.layout.fragment_history, container, false)

    // Chamado depois da view estar criada
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // Obtém a RecyclerView do layout
        rvHistory = view.findViewById(R.id.rvHistory)

        // Define o layout da lista como vertical
        rvHistory.layoutManager = LinearLayoutManager(requireContext())

        // Cria o adapter apenas uma vez (boa prática)
        adapter = WorkoutAdapter(requireContext(), mutableListOf())

        // Liga o adapter à RecyclerView
        rvHistory.adapter = adapter

        // Obtém instância da base de dados
        val db = FitTrackDatabase.get(requireContext())

        // Observa a lista de treinos da base de dados
        db.workoutDao().getAll().observe(viewLifecycleOwner) { workouts ->

            // Atualiza a lista do adapter sempre que os dados mudam
            adapter.update(workouts)
        }

        appSession = AppSession.getInstance(requireActivity())
        val fullname : TextView = view.findViewById(R.id.FullName_display)
        fullname. text = appSession.fullName

        // Now you can use it
        val logoutButton = view.findViewById<Button>(R.id.btnLogout)
        logoutButton.setOnClickListener {
            logout()
        }
    }

    private fun logout() {
        appSession.logout()

        // Navigate to login activity
        val intent = Intent(requireActivity(), Login::class.java)
        startActivity(intent)
        requireActivity().finish()
    }


}
