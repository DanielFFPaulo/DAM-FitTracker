// Define o package da aplicação
package pt.ipt.ddam2025.fittrack

// Importa Context (para aceder a serviços do sistema como sensores)
import android.content.Context
// Importa ColorStateList (para mudar cores dos botões)
import android.content.res.ColorStateList
// Importa Color (para definir cores em hexadecimal)
import android.graphics.Color
// Importa Sensor (tipo de sensor: acelerómetro)
import android.hardware.Sensor
// Importa SensorEvent (dados recebidos do sensor)
import android.hardware.SensorEvent
// Importa SensorEventListener (interface para receber eventos do sensor)
import android.hardware.SensorEventListener
// Importa SensorManager (gestor de sensores do Android)
import android.hardware.SensorManager
// Importa Bundle (estado do fragment)
import android.os.Bundle
// Importa Handler (para atualizar o cronómetro)
import android.os.Handler
// Importa SystemClock (tempo preciso do sistema para cronómetro)
import android.os.SystemClock
// Importa LayoutInflater (para criar a view do XML)
import android.view.LayoutInflater
// Importa View (elemento base UI)
import android.view.View
// Importa ViewGroup (container)
import android.view.ViewGroup
// Importa widgets (TextView, Button, Spinner, Toast, etc.)
import android.widget.*
// Importa Fragment (componente de UI)
import androidx.fragment.app.Fragment
// Importa lifecycleScope (coroutines ligadas ao ciclo de vida)
import androidx.lifecycle.lifecycleScope
// Importa launch (criar coroutine)
import kotlinx.coroutines.launch
// Importa base de dados Room
import pt.ipt.ddam2025.fittrack.data.FitTrackDatabase
// Importa entidade Workout
import pt.ipt.ddam2025.fittrack.data.entities.Workout
// Importa Looper (para garantir Handler na main thread)
import android.os.Looper

// Fragment responsável por contar repetições usando o acelerómetro
class RepsFragment : Fragment(), SensorEventListener {

    // Handler que executa tarefas na UI (main thread)
    private val handler = Handler(Looper.getMainLooper())

    // SensorManager para gerir sensores do dispositivo
    private lateinit var sensorManager: SensorManager
    // Referência ao sensor acelerómetro
    private var accelerometer: Sensor? = null

    // Contador de repetições
    private var reps = 0
    // Flag para detetar fase "a descer" (flexões/agachamentos)
    private var isGoingDown = false
    // Flag para detetar fase "a subir" (abdominais)
    private var isGoingUp = false
    // Indica se o tracking está ativo
    private var tracking = false
    // Guarda o instante em que o treino começou (cronómetro)
    private var startElapsed: Long = 0L

    // Referência ao TextView das repetições
    private lateinit var tvReps: TextView
    // Referência ao TextView do tempo
    private lateinit var tvTime: TextView
    // Botão de start
    private lateinit var btnStart: Button
    // Botão de stop
    private lateinit var btnStop: Button
    // Spinner para escolher exercício
    private lateinit var spExercise: Spinner
    // Exercício selecionado por default
    private var selectedExercise = "Flexões"

    // Cria a view do fragment a partir do XML
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Carrega o layout fragment_reps.xml
        return inflater.inflate(R.layout.fragment_reps, container, false)
    }

    // Chamado depois da view estar criada
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        // Chama implementação base
        super.onViewCreated(view, savedInstanceState)

        // Liga variáveis aos elementos do layout
        tvReps = view.findViewById(R.id.tvReps)
        tvTime = view.findViewById(R.id.tvTime)
        btnStart = view.findViewById(R.id.btnStart)
        btnStop = view.findViewById(R.id.btnStop)
        spExercise = view.findViewById(R.id.spExercise)

        // Texto inicial do cronómetro
        tvTime.text = "Tempo: 00:00"
        // Texto inicial de reps
        tvReps.text = "Repetições: 0"

        // Obtém o serviço de sensores do Android
        sensorManager = requireActivity().getSystemService(Context.SENSOR_SERVICE) as SensorManager
        // Obtém o sensor acelerómetro do dispositivo
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        // Configura o spinner com os exercícios disponíveis
        setupExerciseSelector()

        // Ao carregar start, inicia tracking
        btnStart.setOnClickListener { startTracking() }
        // Ao carregar stop, para tracking e guarda treino
        btnStop.setOnClickListener { stopTracking() }

        // Atualiza estado inicial dos botões/UI
        updateUi()
    }

    // Preenche o spinner e guarda a escolha do utilizador
    private fun setupExerciseSelector() {
        // Lista de exercícios disponíveis
        val exercises = listOf("Flexões", "Agachamentos", "Abdominais")
        // Adapter do spinner
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, exercises)
        // Layout do dropdown do spinner
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        // Liga adapter ao spinner
        spExercise.adapter = adapter

        // Listener quando o utilizador seleciona um exercício
        spExercise.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            // Atualiza variável selectedExercise
            override fun onItemSelected(parent: AdapterView<*>, view: View?, pos: Int, id: Long) {
                selectedExercise = exercises[pos]
            }
            // Não usado
            override fun onNothingSelected(parent: AdapterView<*>) {}
        }
    }

    // Inicia o treino de repetições
    private fun startTracking() {
        // Reinicia contador
        reps = 0
        // Reinicia flags
        isGoingDown = false
        isGoingUp = false
        // Ativa tracking
        tracking = true
        // Guarda o instante inicial do cronómetro
        startElapsed = SystemClock.elapsedRealtime()

        // Mostra tempo inicial
        tvTime.text = "Tempo: 00:00"

        // Desativa botão start
        btnStart.isEnabled = false
        // Muda cor do start para cinzento
        btnStart.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#DDDDDD"))

        // Ativa botão stop
        btnStop.isEnabled = true
        // Muda cor do stop para vermelho
        btnStop.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#F44336"))

        // Regista o listener do acelerómetro (começa a receber dados)
        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_GAME)

        // Inicia o cronómetro (atualiza de 1 em 1 segundo)
        handler.post(tickRunnable)

        // Atualiza UI
        updateUi()
    }

    // Para o treino de repetições
    private fun stopTracking() {
        // Desativa tracking
        tracking = false
        // Para de receber eventos do sensor
        sensorManager.unregisterListener(this)
        // Para o cronómetro
        handler.removeCallbacks(tickRunnable)

        // Guarda o treino na base de dados
        saveWorkout()

        // Ativa botão start
        btnStart.isEnabled = true
        // Muda cor do start para verde
        btnStart.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#4CAF50"))

        // Desativa botão stop
        btnStop.isEnabled = false
        // Muda cor do stop para cinzento
        btnStop.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#CCCCCC"))

        // Faz reset visual do tempo
        tvTime.text = "Tempo: 00:00"

        // Atualiza UI
        updateUi()
    }

    // Chamado sempre que o acelerómetro envia valores novos
    override fun onSensorChanged(event: SensorEvent?) {
        // Se não estiver a gravar ou evento for nulo, ignora
        if (!tracking || event == null) return

        // Valor do eixo Z (frente/trás)
        val z = event.values[2]
        // Valor do eixo Y (cima/baixo)
        val y = event.values[1]

        // Escolhe lógica conforme o exercício selecionado
        when (selectedExercise) {

            // Flexões: usa eixo Z e deteta "descer -> subir"
            "Flexões" -> {
                // Quando z fica muito baixo, considera que está a descer
                if (z < -5 && !isGoingDown) isGoingDown = true
                // Quando z fica alto depois de descer, conta uma repetição
                if (z > 5 && isGoingDown) {
                    reps++
                    isGoingDown = false
                    tvReps.text = "Repetições: $reps"
                }
            }

            // Agachamentos: usa eixo Y e deteta "descer -> subir"
            "Agachamentos" -> {
                // Quando y fica baixo, considera que está a descer
                if (y < -4 && !isGoingDown) isGoingDown = true
                // Quando y fica alto depois de descer, conta uma repetição
                if (y > 4 && isGoingDown) {
                    reps++
                    isGoingDown = false
                    tvReps.text = "Repetições: $reps"
                }
            }

            // Abdominais: usa eixo Z e deteta "subir -> descer"
            "Abdominais" -> {
                // Quando z fica alto, considera que está a subir
                if (z > 6 && !isGoingUp) isGoingUp = true
                // Quando z volta a baixar, conta uma repetição
                if (z < 2 && isGoingUp) {
                    reps++
                    isGoingUp = false
                    tvReps.text = "Repetições: $reps"
                }
            }
        }
    }

    // Não usado aqui (precisão do sensor)
    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    // Atualiza estado dos botões e tempo
    private fun updateUi() {
        // Start só pode quando não está a gravar
        btnStart.isEnabled = !tracking
        // Stop só pode quando está a gravar
        btnStop.isEnabled = tracking

        // Se não está a gravar, mostra tempo calculado
        if (!tracking) {
            tvTime.text = "Tempo: " + formatHMS(elapsedSeconds())
        }
    }

    // Calcula quantos segundos passaram desde o início
    private fun elapsedSeconds(): Int =
        ((SystemClock.elapsedRealtime() - startElapsed) / 1000).toInt()

    // Converte segundos para mm:ss
    private fun formatHMS(totalSec: Int): String {
        val m = totalSec / 60
        val s = totalSec % 60
        return "%02d:%02d".format(m, s)
    }

    // Runnable que atualiza o cronómetro a cada 1 segundo
    private val tickRunnable = object : Runnable {
        override fun run() {
            // Só atualiza se estiver a gravar
            if (tracking) {
                tvTime.text = "Tempo: " + formatHMS(elapsedSeconds())
                // Agenda próxima atualização
                handler.postDelayed(this, 1000)
            }
        }
    }

    // Guarda o treino na base de dados
    private fun saveWorkout() {
        // Usa o contexto da aplicação (seguro para BD)
        val ctx = requireContext().applicationContext
        // Duração final do treino
        val durationSec = elapsedSeconds()

        // Coroutine ligada ao ciclo de vida do fragment
        lifecycleScope.launch {
            // Obtém base de dados
            val db = FitTrackDatabase.get(ctx)

            // Insere um Workout de exercício (sem distância e sem ritmo)
            db.workoutDao().insert(
                Workout(
                    type = selectedExercise.uppercase(),
                    startTime = System.currentTimeMillis() - durationSec * 1000,
                    endTime = System.currentTimeMillis(),
                    durationSec = durationSec,
                    distanceMeters = 0.0,
                    paceSecPerKm = 0,
                    reps = reps
                )
            )

            // Feedback para o utilizador
            Toast.makeText(ctx, "Treino guardado ✅", Toast.LENGTH_SHORT).show()
        }
    }
}
