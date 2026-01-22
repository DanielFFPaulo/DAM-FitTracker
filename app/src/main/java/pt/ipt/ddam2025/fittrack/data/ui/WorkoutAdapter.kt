// Define o pacote onde esta classe está guardada
package pt.ipt.ddam2025.fittrack.ui

// Importa a classe para criar caixas de diálogo (confirmar apagar)
import android.app.AlertDialog
// Importa Context (necessário para inflar layouts, abrir intents, BD, etc.)
import android.content.Context
// Importa Intent (para partilhar o treino com outras apps)
import android.content.Intent
// Importa LayoutInflater (para criar a View do item da lista)
import android.view.LayoutInflater
// Importa View (tipo base de qualquer componente visual)
import android.view.View
// Importa ViewGroup (container de Views, usado no RecyclerView)
import android.view.ViewGroup
// Importa Button (botões do item: partilhar/remover)
import android.widget.Button
// Importa TextView (textos do item)
import android.widget.TextView
// Importa Toast (mensagens rápidas no ecrã)
import android.widget.Toast

// Importa RecyclerView (lista eficiente para mostrar muitos itens)
import androidx.recyclerview.widget.RecyclerView

// Importa CoroutineScope (para lançar coroutines manualmente)
import kotlinx.coroutines.CoroutineScope
// Importa Dispatchers (define em que “tipo de thread” corre a coroutine)
import kotlinx.coroutines.Dispatchers
// Importa launch (cria/inicia uma coroutine)
import kotlinx.coroutines.launch

// Importa recursos da app (layouts, ids, etc.)
import pt.ipt.ddam2025.fittrack.R
// Importa a base de dados Room da aplicação
import pt.ipt.ddam2025.fittrack.data.FitTrackDatabase
// Importa a entidade Workout (modelo de um treino)
import pt.ipt.ddam2025.fittrack.data.entities.Workout

// Importa formatador de datas
import java.text.SimpleDateFormat
// Importa Date para converter timestamps
import java.util.Date
// Importa Locale para formatar conforme linguagem/região
import java.util.Locale

// Classe Adapter: liga a lista de Workouts ao RecyclerView do histórico
class WorkoutAdapter(
    // Context necessário para inflar layouts, intents e base de dados
    private val context: Context,
    // Lista mutável de treinos que o adapter vai mostrar
    private val workouts: MutableList<Workout>
) : RecyclerView.Adapter<WorkoutAdapter.WorkoutViewHolder>() {

    // Função para atualizar o conteúdo do RecyclerView quando a BD muda
    fun update(newList: List<Workout>) {
        // Limpa a lista atual
        workouts.clear()
        // Adiciona todos os elementos da nova lista
        workouts.addAll(newList)
        // Notifica o RecyclerView que tudo mudou
        notifyDataSetChanged()
    }

    // Cria um ViewHolder (uma linha do RecyclerView)
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WorkoutViewHolder {
        // Cria a View a partir do layout XML do item (item_workout.xml)
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_workout, parent, false)
        // Devolve o ViewHolder com essa View
        return WorkoutViewHolder(view)
    }

    // Liga os dados de um Workout à linha correspondente
    override fun onBindViewHolder(holder: WorkoutViewHolder, position: Int) {
        // Pede ao holder para preencher a UI com o workout daquela posição
        holder.bind(workouts[position])
    }

    // Diz ao RecyclerView quantos itens existem
    override fun getItemCount(): Int = workouts.size

    // ViewHolder: representa e gere um item individual (linha) do RecyclerView
    inner class WorkoutViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        // TextView do emoji/ícone do treino
        private val tvWorkoutIcon: TextView = itemView.findViewById(R.id.tvWorkoutIcon)
        // TextView do nome do treino
        private val tvWorkoutType: TextView = itemView.findViewById(R.id.tvWorkoutType)
        // TextView da data do treino
        private val tvWorkoutDate: TextView = itemView.findViewById(R.id.tvWorkoutDate)
        // TextView com detalhes (tempo, distância, reps, ritmo)
        private val tvWorkoutDetails: TextView = itemView.findViewById(R.id.tvWorkoutDetails)

        // Botão para partilhar o treino
        private val btnShare: Button = itemView.findViewById(R.id.btnShare)
        // Botão para remover o treino
        private val btnDelete: Button = itemView.findViewById(R.id.btnDelete)

        // Função que preenche a linha com os dados do treino
        fun bind(workout: Workout) {
            // Cria formatador de data/hora no formato dd/MM/yyyy HH:mm
            val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
            // Converte startTime (epoch) para uma string de data legível
            val dateStr = sdf.format(Date(workout.startTime))

            // Escolhe o ícone e o nome “bonito” conforme o tipo de treino
            val (icon, label) = when (workout.type.uppercase()) {
                // Corrida
                "RUN" -> "🏃" to "Corrida"
                // Flexões (com e sem acento)
                "FLEXÕES", "FLEXOES" -> "💪" to "Flexões"
                // Agachamentos
                "AGACHAMENTOS" -> "🦵" to "Agachamentos"
                // Abdominais
                "ABDOMINAIS" -> "🔥" to "Abdominais"
                // Qualquer outro tipo
                else -> "🏋️" to workout.type
            }

            // Coloca o ícone na UI
            tvWorkoutIcon.text = icon
            // Coloca o nome do treino na UI
            tvWorkoutType.text = label
            // Coloca a data na UI
            tvWorkoutDate.text = dateStr

            // Formata a duração do treino para mm:ss
            val duration = "%02d:%02d".format(workout.durationSec / 60, workout.durationSec % 60)

            // Decide o texto dos detalhes conforme for corrida ou exercício
            val details = if (workout.type.uppercase() == "RUN") {
                // Converte metros para km
                val km = workout.distanceMeters / 1000.0
                // Formata o ritmo (min:sec) se existir
                val pace = if (workout.paceSecPerKm > 0) {
                    "%d:%02d".format(workout.paceSecPerKm / 60, workout.paceSecPerKm % 60)
                } else {
                    // Se não houver ritmo calculado ainda
                    "—"
                }

                // Texto final para corrida
                "Distância: %.2f km\nTempo: %s\nRitmo: %s min/km".format(km, duration, pace)
            } else {
                // Se existir reps, adiciona uma linha “Repetições: X”
                val repsLine = workout.reps?.let { "\nRepetições: $it" } ?: ""
                // Texto final para exercícios
                "Tempo: $duration$repsLine"
            }

            // Coloca os detalhes na UI
            tvWorkoutDetails.text = details

            // Quando clicas em “Partilhar”
            btnShare.setOnClickListener {
                // Monta o texto a enviar para outras apps
                val shareText = "📊 O meu treino no FitTrack:\n\n" +
                        "$icon $label\n" +
                        "$dateStr\n" +
                        "$details\n\n" +
                        "#FitTrack"

                // Cria um intent de partilha (texto)
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    // Tipo de conteúdo: texto simples
                    type = "text/plain"
                    // Conteúdo do texto
                    putExtra(Intent.EXTRA_TEXT, shareText)
                }

                // Abre o seletor de apps para partilhar
                context.startActivity(Intent.createChooser(shareIntent, "Partilhar treino via"))
            }

            // Quando clicas em “Remover”
            btnDelete.setOnClickListener {
                // Abre um diálogo a confirmar a remoção
                AlertDialog.Builder(context)
                    // Título do diálogo
                    .setTitle("Remover treino")
                    // Mensagem do diálogo
                    .setMessage("Tens a certeza que queres eliminar este treino?")
                    // Se carregar “Sim”, chama a função de delete
                    .setPositiveButton("Sim") { _, _ -> deleteWorkout(workout) }
                    // Se carregar “Cancelar”, fecha
                    .setNegativeButton("Cancelar", null)
                    // Mostra o diálogo
                    .show()
            }
        }

        // Função que remove o treino da base de dados e da lista
        private fun deleteWorkout(workout: Workout) {
            // Procura a posição do workout pelo id
            val pos = workouts.indexOfFirst { it.id == workout.id }
            // Se não encontrar, sai
            if (pos == -1) return

            // Lança uma coroutine em background para mexer na base de dados
            CoroutineScope(Dispatchers.IO).launch {
                // Obtém instância da base de dados
                val db = FitTrackDatabase.get(context)
                // Apaga o treino da base de dados
                db.workoutDao().delete(workout)

                // Volta à thread principal para atualizar a UI
                CoroutineScope(Dispatchers.Main).launch {
                    // Remove da lista local
                    workouts.removeAt(pos)
                    // Notifica o RecyclerView que aquele item saiu
                    notifyItemRemoved(pos)
                    // Mostra mensagem ao utilizador
                    Toast.makeText(context, "Treino removido ✅", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}
