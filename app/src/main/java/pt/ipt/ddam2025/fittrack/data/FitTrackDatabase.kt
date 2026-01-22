// Define o package onde esta classe está localizada
package pt.ipt.ddam2025.fittrack.data

// Importa Context (necessário para criar a base de dados)
import android.content.Context

// Anotação que indica que esta classe é uma base de dados Room
import androidx.room.Database

// Classe Room para criar instâncias da base de dados
import androidx.room.Room

// Classe base que todas as bases de dados Room devem herdar
import androidx.room.RoomDatabase

// Classes usadas para migração de versões da base de dados
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

// Importa os DAOs da aplicação
import pt.ipt.ddam2025.fittrack.data.dao.RoutePointDao
import pt.ipt.ddam2025.fittrack.data.dao.WorkoutDao

// Importa as entidades (tabelas) da base de dados
import pt.ipt.ddam2025.fittrack.data.entities.RoutePoint
import pt.ipt.ddam2025.fittrack.data.entities.Workout

// Anotação que define esta classe como uma base de dados Room
@Database(
    // Lista de entidades (tabelas) da base de dados
    entities = [Workout::class, RoutePoint::class],
    // Versão atual da base de dados
    version = 2,
    // Não exporta o esquema da base de dados
    exportSchema = false
)
// Classe abstrata que representa a base de dados da aplicação
abstract class FitTrackDatabase : RoomDatabase() {

    // Função que dá acesso ao DAO de Workout
    abstract fun workoutDao(): WorkoutDao

    // Função que dá acesso ao DAO de RoutePoint
    abstract fun routePointDao(): RoutePointDao

    // Companion object para implementar o padrão Singleton
    companion object {

        // Instância única da base de dados (visível entre threads)
        @Volatile
        private var INSTANCE: FitTrackDatabase? = null

        // Migração da versão 1 para a versão 2 da base de dados
        // Adiciona a coluna "reps" à tabela Workout
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE Workout ADD COLUMN reps INTEGER")
            }
        }

        // Função que devolve a instância única da base de dados
        fun get(ctx: Context): FitTrackDatabase =
            // Se já existir uma instância, devolve-a
            INSTANCE ?: synchronized(this) {
                // Se ainda não existir, cria a base de dados
                INSTANCE ?: Room.databaseBuilder(
                    // Usa o contexto da aplicação (evita memory leaks)
                    ctx.applicationContext,
                    // Classe da base de dados
                    FitTrackDatabase::class.java,
                    // Nome do ficheiro da base de dados
                    "fittrack.db"
                )
                    // Regista a migração da versão 1 para 2
                    .addMigrations(MIGRATION_1_2)
                    // Cria a base de dados
                    .build()
                    // Guarda a instância criada
                    .also { INSTANCE = it }
            }
    }
}
