package pt.ipt.ddam2025.fittrack.data.dao

// Indica que esta interface é um DAO do Room
import androidx.room.Dao

// Anotação para apagar registos da base de dados
import androidx.room.Delete

// Anotação para inserir dados na base de dados
import androidx.room.Insert

// Anotação para executar queries SQL personalizadas
import androidx.room.Query

// Entidade que representa um treino
import pt.ipt.ddam2025.fittrack.data.entities.Workout

// DAO responsável pelo acesso à tabela de treinos
@Dao
interface WorkoutDao {

    // Insere um treino na base de dados
    // suspend → executado numa coroutine para não bloquear a interface
    // Uma coroutine Uma coroutine é uma forma de executar tarefas demoradas em segundo plano, sem bloquear a aplicação.
    // Retorna o ID do treino inserido
    @Insert
    suspend fun insert(workout: Workout): Long

    // Vai buscar todos os treinos guardados
    // Ordenados do mais recente para o mais antigo
    // LiveData → atualiza a interface automaticamente quando os dados mudam
    @Query("SELECT * FROM Workout ORDER BY startTime DESC")
    fun getAll(): androidx.lifecycle.LiveData<List<Workout>>

    // Remove um treino da base de dados
    // suspend → executado em background
    @Delete
    suspend fun delete(workout: Workout)
}
