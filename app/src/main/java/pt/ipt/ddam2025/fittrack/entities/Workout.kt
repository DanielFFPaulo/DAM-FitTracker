// Define o package onde esta entity está localizada
package pt.ipt.ddam2025.fittrack.data.entities

// Anotação que indica que esta classe é uma tabela da base de dados Room
import androidx.room.Entity

// Anotação para definir a chave primária da tabela
import androidx.room.PrimaryKey

// Define esta classe como uma entidade Room (tabela Workout)
@Entity
data class Workout(

    // Chave primária da tabela (gerada automaticamente)
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    // Tipo de treino (ex: RUN, FLEXÕES, AGACHAMENTOS, ABDOMINAIS)
    val type: String,

    // Momento de início do treino (em epoch milliseconds)
    //Epoch millis é o número de milissegundos, evita problemas de formatos de data
    val startTime: Long,

    // Momento de fim do treino (em epoch milliseconds)
    val endTime: Long,

    // Duração total do treino em segundos
    val durationSec: Int,

    // Distância percorrida em metros (usada nas corridas)
    val distanceMeters: Double,

    // Ritmo médio em segundos por quilómetro (apenas para corridas)
    val paceSecPerKm: Int,

    // Número de repetições (usado apenas em exercícios de reps)
    // Pode ser null em treinos de corrida
    val reps: Int? = null
)
