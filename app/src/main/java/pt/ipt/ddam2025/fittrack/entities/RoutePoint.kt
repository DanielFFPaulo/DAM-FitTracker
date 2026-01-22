// Define o package onde esta entity está localizada
package pt.ipt.ddam2025.fittrack.data.entities

// Anotação que indica que esta classe é uma tabela da base de dados Room
import androidx.room.Entity

// Permite criar índices na tabela para melhorar desempenho das queries
import androidx.room.Index

// Anotação para definir a chave primária da tabela
import androidx.room.PrimaryKey

// Define esta classe como uma entidade Room (tabela)
// Cria um índice sobre a coluna workoutId para acelerar pesquisas por treino
@Entity(indices = [Index("workoutId")])
data class RoutePoint(

    // Chave primária da tabela (gerada automaticamente)
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    // Identificador do treino a que este ponto pertence
    val workoutId: Long,

    // Timestamp do ponto (momento em que a posição foi registada)
    val ts: Long,

    // Latitude da localização GPS
    val lat: Double,

    // Longitude da localização GPS
    val lon: Double,

    // Altitude da localização GPS (pode ser nula)
    val alt: Double?
)
