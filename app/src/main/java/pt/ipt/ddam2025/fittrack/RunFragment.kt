// Define o package do ficheiro
package pt.ipt.ddam2025.fittrack

// Importa a classe Manifest (para permissões)
import android.Manifest
// Importa PackageManager (para verificar permissões)
import android.content.pm.PackageManager
// Importa Location (objeto de localização GPS)
import android.location.Location
// Importa Bundle (estado do Fragment)
import android.os.Bundle
// Importa Handler (para executar tarefas repetidas)
import android.os.Handler
// Importa SystemClock (tempo preciso para cronómetro)
import android.os.SystemClock
// Importa LayoutInflater (carregar XML para View)
import android.view.LayoutInflater
// Importa View (base de componentes UI)
import android.view.View
// Importa ViewGroup (container de Views)
import android.view.ViewGroup
// Importa Button (botões start/stop)
import android.widget.Button
// Importa TextView (textos de distância/tempo/ritmo)
import android.widget.TextView
// Importa Toast (mensagens rápidas)
import android.widget.Toast
// Importa API para pedir permissões em runtime
import androidx.activity.result.contract.ActivityResultContracts
// Importa ContextCompat (helper de permissões)
import androidx.core.content.ContextCompat
// Importa Fragment (ecrã modular)
import androidx.fragment.app.Fragment
// Importa lifecycleScope (coroutines ligadas ao ciclo de vida)
import androidx.lifecycle.lifecycleScope
// Importa Fused Location APIs (Google Play Services)
import com.google.android.gms.location.*
// Importa launch (para iniciar coroutine)
import kotlinx.coroutines.launch
// Importa a base de dados Room
import pt.ipt.ddam2025.fittrack.data.FitTrackDatabase
// Importa a entidade RoutePoint (ponto GPS)
import pt.ipt.ddam2025.fittrack.data.entities.RoutePoint
// Importa a entidade Workout (treino)
import pt.ipt.ddam2025.fittrack.data.entities.Workout
// Importa round (arredondar números)
import kotlin.math.round
// Importa ColorStateList (cores de botões)
import android.content.res.ColorStateList
// Importa Color (converter hex em cor)
import android.graphics.Color

// Define o fragment responsável por rastrear a corrida
class RunFragment : Fragment() {

    // Cliente que fornece localizações (Fused Location Provider)
    private lateinit var fused: FusedLocationProviderClient
    // Pedido/configuração de updates de localização
    private lateinit var request: LocationRequest
    // Callback que recebe os resultados de localização
    private lateinit var callback: LocationCallback

    // Lista temporária de pontos GPS do percurso
    private val routeBuffer = mutableListOf<RoutePoint>()
    // Hora real do início do treino (para guardar no Workout)
    private var startWallClock: Long = 0L

    // Indica se está a gravar (tracking ativo)
    private var tracking = false
    // Guarda a última localização recebida (para calcular distância)
    private var lastLocation: Location? = null
    // Distância total percorrida (em metros)
    private var metersTotal = 0.0
    // Tempo inicial do cronómetro (elapsed realtime)
    private var startElapsed: Long = 0L

    // TextView onde mostramos a distância
    private lateinit var tvDistance: TextView
    // TextView onde mostramos o tempo
    private lateinit var tvTime: TextView
    // TextView onde mostramos o ritmo
    private lateinit var tvPace: TextView
    // Botão Start
    private lateinit var btnStart: Button
    // Botão Stop
    private lateinit var btnStop: Button

    // Handler para atualizar as stats a cada segundo
    private val handler = Handler()

    // Launcher para pedir permissões de localização (runtime)
    private val permissionLauncher =
        // Regista o pedido de várias permissões e recebe o resultado
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { grants ->
            // Verifica se foi concedida FINE ou COARSE
            val ok = (grants[Manifest.permission.ACCESS_FINE_LOCATION] == true) ||
                    (grants[Manifest.permission.ACCESS_COARSE_LOCATION] == true)
            // Se ok, começa tracking; senão mostra toast
            if (ok) startTracking() else showPermissionDeniedToast()
        }

    // Cria a View do fragment a partir do XML fragment_run
    override fun onCreateView(
        // Inflater para carregar o layout
        inflater: LayoutInflater,
        // Container (pai) onde a View será colocada
        container: ViewGroup?,
        // Estado guardado (se existir)
        savedInstanceState: Bundle?

        // Retorna a View inflada do layout
    ): View = inflater.inflate(R.layout.fragment_run, container, false)

    // Chamado depois da View ter sido criada
    override fun onViewCreated(
        // A View criada a partir do XML
        view: View,
        // Estado guardado (se existir)
        savedInstanceState: Bundle?
    ) {
        // Chama o método da classe pai
        super.onViewCreated(view, savedInstanceState)

        // Liga a distância ao TextView do layout
        tvDistance = view.findViewById(R.id.tvDistance)
        // Liga o tempo ao TextView do layout
        tvTime = view.findViewById(R.id.tvTime)
        // Liga o ritmo ao TextView do layout
        tvPace = view.findViewById(R.id.tvPace)
        // Liga o botão Start ao layout
        btnStart = view.findViewById(R.id.btnStart)
        // Liga o botão Stop ao layout
        btnStop = view.findViewById(R.id.btnStop)

        // Mostra tempo inicial no ecrã
        tvTime.text = "Tempo: 00:00:00"

        // Inicializa o fused location client
        fused = LocationServices.getFusedLocationProviderClient(requireActivity())
        // Cria o request: alta precisão e intervalo base de 1 segundo
        request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 1000L)
            // Define intervalo mínimo (mais frequente se possível)
            .setMinUpdateIntervalMillis(500L)
            // Finaliza o request
            .build()

        // Define o callback para receber localizações
        callback = object : LocationCallback() {
            // Chamado quando chegam localizações novas
            override fun onLocationResult(result: LocationResult) {
                // Percorre todas as localizações recebidas
                for (loc in result.locations) onNewLocation(loc)
            }
        }

        // Start → inicia tracking
        btnStart.setOnClickListener { startTracking() }
        // Stop → para tracking
        btnStop.setOnClickListener { stopTracking() }

        // Atualiza UI inicial
        updateUi()
    }

    // Inicia o tracking da corrida
    private fun startTracking() {
        // Se não tiver permissão, pede e sai
        if (!hasLocationPermission()) {
            // Pede permissões
            requestLocationPermission()
            // Sai do método
            return
        }

        // Ativa tracking
        tracking = true
        // Reseta a última localização
        lastLocation = null
        // Reseta distância total
        metersTotal = 0.0
        // Guarda tempo inicial do cronómetro
        startElapsed = SystemClock.elapsedRealtime()
        // Atualiza o texto do tempo
        tvTime.text = "Tempo: 00:00:00"

        // Começa a receber updates de localização
        safeRequestLocationUpdates()
        // Inicia o runnable do cronómetro/stats
        handler.post(tickRunnable)

        // Desativa botão Start
        btnStart.isEnabled = false
        // Pinta botão Start de cinzento
        btnStart.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#DDDDDD"))

        // Ativa botão Stop
        btnStop.isEnabled = true
        // Pinta botão Stop de vermelho
        btnStop.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#F44336"))

        // Limpa pontos do percurso anterior
        routeBuffer.clear()
        // Guarda o momento real do início do treino
        startWallClock = System.currentTimeMillis()

        // Atualiza UI
        updateUi()
    }

    // Para o tracking da corrida
    private fun stopTracking() {
        // Desativa tracking
        tracking = false
        // Para atualizações de localização
        safeRemoveLocationUpdates()
        // Para o runnable do cronómetro
        handler.removeCallbacks(tickRunnable)

        // Guarda o treino e os pontos GPS
        saveWorkout()
        // Atualiza UI
        updateUi()

        // Reativa botão Start
        btnStart.isEnabled = true
        // Pinta botão Start de verde
        btnStart.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#4CAF50"))

        // Desativa botão Stop
        btnStop.isEnabled = false
        // Pinta botão Stop de cinzento
        btnStop.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#CCCCCC"))
    }

    // Processa uma nova localização GPS recebida
    private fun onNewLocation(
        // Localização recebida
        loc: Location
    ) {
        // Se houver localização anterior, soma a distância até à nova
        lastLocation?.let { prev -> metersTotal += prev.distanceTo(loc) }
        // Atualiza a última localização
        lastLocation = loc

        // Adiciona um RoutePoint ao buffer (workoutId ainda é 0)
        routeBuffer += RoutePoint(
            // Workout ainda não existe na BD, por isso 0 temporariamente
            workoutId = 0,
            // Timestamp do ponto
            ts = System.currentTimeMillis(),
            // Latitude do ponto
            lat = loc.latitude,
            // Longitude do ponto
            lon = loc.longitude,
            // Altitude (se existir)
            alt = if (loc.hasAltitude()) loc.altitude else null
        )

        // Atualiza estatísticas no ecrã
        refreshStats()
    }

    // Atualiza estado dos botões e estatísticas
    private fun updateUi() {
        // Start fica ativo quando não há tracking
        btnStart.isEnabled = !tracking
        // Stop fica ativo quando há tracking
        btnStop.isEnabled = tracking
        // Se não está a gravar, mostra stats atuais
        if (tracking) refreshStats()
    }

    // Calcula e mostra distância, tempo e ritmo
    private fun refreshStats() {
        // Converte metros em km
        val km = metersTotal / 1000.0
        // Mostra distância formatada
        tvDistance.text = "Distância: %.2f km".format(km)

        // Obtém segundos passados
        val secs = elapsedSeconds()
        // Mostra tempo formatado
        tvTime.text = "Tempo: " + formatHMS(secs)

        // Mostra ritmo médio (min/km) ou 0 se ainda não há distância
        tvPace.text = if (km > 0.01) {
            // Calcula segundos por km
            val paceSecPerKm = secs / km
            // Calcula minutos do ritmo
            val paceMin = (paceSecPerKm / 60).toInt()
            // Calcula segundos do ritmo (arredondado)
            val paceSec = round(paceSecPerKm % 60).toInt()
            // Mostra o ritmo formatado
            "Ritmo: %d:%02d min/km".format(paceMin, paceSec)
        } else {
            // Evita dividir por zero e mostra ritmo a zero
            "Ritmo: 0:00 min/km"
        }
    }

    // Calcula os segundos desde o início do tracking
    private fun elapsedSeconds(): Int =
        // Diferença entre agora e o início, convertido para segundos
        ((SystemClock.elapsedRealtime() - startElapsed) / 1000).toInt()

    // Converte segundos totais em hh:mm:ss
    private fun formatHMS(
        // Total de segundos
        totalSec: Int
    ): String {
        // Calcula horas
        val h = totalSec / 3600
        // Calcula minutos
        val m = (totalSec % 3600) / 60
        // Calcula segundos
        val s = totalSec % 60
        // Formata com zeros à esquerda
        return "%02d:%02d:%02d".format(h, m, s)
    }

    // Runnable que atualiza stats a cada 1 segundo
    private val tickRunnable = object : Runnable {
        // Método chamado quando o runnable executa
        override fun run() {
            // Só atualiza se tracking estiver ativo
            if (tracking) {
                // Atualiza stats
                refreshStats()
                // Agenda a próxima execução em 1 segundo
                handler.postDelayed(this, 1000)
            }
        }
    }

    // Guarda o treino e os pontos na base de dados
    private fun saveWorkout() {
        // Context seguro para BD
        val ctx = requireContext().applicationContext
        // Distância final
        val distance = metersTotal
        // Duração final
        val durationSec = elapsedSeconds()
        // Calcula ritmo médio (seg/km) apenas se tiver distância suficiente
        val paceSecPerKm =
            if (distance > 10.0) (durationSec / (distance / 1000.0)).toInt() else 0

        // Coroutine para aceder à BD sem bloquear a UI
        lifecycleScope.launch {
            // Obtém BD
            val db = FitTrackDatabase.get(ctx)
            // Insere Workout e obtém id gerado
            val workoutId = db.workoutDao().insert(
                // Cria o objeto Workout
                Workout(
                    // Tipo do treino
                    type = "RUN",
                    // Início real do treino
                    startTime = startWallClock,
                    // Fim real do treino
                    endTime = System.currentTimeMillis(),
                    // Duração em segundos
                    durationSec = durationSec,
                    // Distância em metros
                    distanceMeters = distance,
                    // Ritmo médio seg/km
                    paceSecPerKm = paceSecPerKm
                )
            )

            // Se existirem pontos no buffer, guarda-os
            if (routeBuffer.isNotEmpty()) {
                // Copia pontos e coloca workoutId correto
                val points = routeBuffer.map { it.copy(workoutId = workoutId) }
                // Insere todos os pontos
                db.routePointDao().insertAll(points)
            }

            // Mostra mensagem de sucesso
            Toast.makeText(ctx, "Treino guardado ✅", Toast.LENGTH_SHORT).show()
        }
    }

    // Verifica se existe permissão de localização
    private fun hasLocationPermission(): Boolean {
        // Context do fragment
        val ctx = requireContext()
        // Retorna true se FINE ou COARSE estiverem concedidas
        return ContextCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_FINE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_COARSE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED
    }

    // Pede permissões de localização ao utilizador
    private fun requestLocationPermission() {
        // Lança o pedido das permissões
        permissionLauncher.launch(
            // Array com as permissões a pedir
            arrayOf(
                // Permissão de localização precisa
                Manifest.permission.ACCESS_FINE_LOCATION,
                // Permissão de localização aproximada
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )
    }

    // Mostra um toast quando o utilizador recusa permissões
    private fun showPermissionDeniedToast() =
        // Cria e mostra o toast
        Toast.makeText(requireContext(), "Permissão de localização recusada", Toast.LENGTH_SHORT).show()

    // Pede updates de localização de forma segura
    private fun safeRequestLocationUpdates() {
        // Se não tiver permissão, sai
        if (!hasLocationPermission()) return
        // Tenta pedir updates (pode lançar SecurityException)
        try {
            // Regista o callback para receber localizações
            fused.requestLocationUpdates(request, callback, requireActivity().mainLooper)
        } catch (_: SecurityException) {
            // Ignora exceção para evitar crash
        }
    }

    // Remove updates de localização de forma segura
    private fun safeRemoveLocationUpdates() {
        // Tenta remover updates
        try {
            // Remove o callback do fused location
            fused.removeLocationUpdates(callback)
        } catch (_: SecurityException) {
            // Ignora exceção
        }
    }

    // Chamado quando o fragment entra em foreground
    override fun onStart() {
        // Chama implementação base
        super.onStart()
        // Se estiver tracking e tiver permissão, retoma updates do runnable
        if (tracking && hasLocationPermission()) handler.post(tickRunnable)
    }

    // Chamado quando o fragment sai de foreground
    override fun onStop() {
        // Chama implementação base
        super.onStop()
        // Para o runnable para poupar bateria/recursos
        handler.removeCallbacks(tickRunnable)
    }
}
