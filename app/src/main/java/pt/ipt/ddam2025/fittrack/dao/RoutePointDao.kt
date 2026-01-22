package pt.ipt.ddam2025.fittrack.data.dao

// Anota esta interface como um DAO (Data Access Object) do Room
import androidx.room.Dao

// Anotação para inserir dados na base de dados
import androidx.room.Insert

// Anotação para executar queries SQL personalizadas
import androidx.room.Query

// Entidade que representa um ponto da rota GPS
import pt.ipt.ddam2025.fittrack.data.entities.RoutePoint

// Indica que esta interface é um DAO do Room
@Dao
interface RoutePointDao {

    // Insere uma lista de pontos GPS na base de dados
    // // suspend → função executada em background
    @Insert
    suspend fun insertAll(points: List<RoutePoint>)

    // Vai buscar todos os pontos GPS associados a um treino específico
    // Ordena os pontos pelo timestamp (ts) de forma crescente
    @Query("SELECT * FROM RoutePoint WHERE workoutId = :wId ORDER BY ts ASC")
    suspend fun forWorkout(wId: Long): List<RoutePoint>
}
