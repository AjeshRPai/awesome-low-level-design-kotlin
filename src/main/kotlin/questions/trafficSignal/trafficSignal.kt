package questions.trafficSignal

import kotlinx.coroutines.*
import java.util.concurrent.atomic.AtomicBoolean

enum class Signal {
    RED,
    YELLOW,
    GREEN
}

class Road(val id: String, private val name: String) {
    var trafficLight: TrafficLight? = null
}

class TrafficController private constructor() {
    private val roads: MutableMap<String, Road> = HashMap()
    private var emergencyMode: AtomicBoolean = AtomicBoolean(false)
    private var emergencyRoadId: String? = null

    private val trafficLightScope = CoroutineScope(SupervisorJob())

    fun addRoad(road: Road) {
        roads[road.id] = road
    }

    fun removeRoad(roadId: String) {
        roads.remove(roadId)
    }


    @OptIn(DelicateCoroutinesApi::class)
    fun startTrafficControl() {
        // Launch each traffic light control in its own coroutine
        GlobalScope.launch {
            for (road in roads.values) {
                manageTrafficLight(road)
            }
        }
    }


    private suspend fun manageTrafficLight(road: Road) {
        val trafficLight = road.trafficLight ?: return
        try {
            if (!emergencyMode.get()) {
                println("Managing traffic light for road ${road.id}")
                delay(trafficLight.redDuration)
                trafficLight.changeSignal(Signal.GREEN)
                delay(trafficLight.greenDuration)
                trafficLight.changeSignal(Signal.YELLOW)
                delay(trafficLight.yellowDuration)
                trafficLight.changeSignal(Signal.RED)
            } else {
                delay(1000)
                println("Traffic control is in emergency mode")
            }
        } catch (exception: CancellationException) {
            println("Traffic control is stopped for road ${road.id}")
        }
    }

    @Synchronized
    fun handleEmergency(roadId: String) {
        println("Emergency detected on road $roadId")

        // Update emergency state
        emergencyMode.set(true)
        emergencyRoadId = roadId

        // Set all other roads' traffic lights to RED
        roads.values.forEach { road ->
            road.trafficLight?.changeSignal(Signal.RED)
        }

        // Set the emergency road's traffic light to GREEN
        val emergencyRoad = roads[roadId]
        emergencyRoad?.trafficLight?.changeSignal(Signal.GREEN)
    }

    suspend fun resolveEmergency() {
        println("Resolving emergency on road $emergencyRoadId")

        // Reset emergency mode
        emergencyMode.set(false)
        emergencyRoadId = null

        // Restart traffic control after a small delay
        delay(1000)
        startTrafficControl()
    }

    companion object {
        val INSTANCE: TrafficController by lazy(LazyThreadSafetyMode.SYNCHRONIZED) { TrafficController() }
    }
}

class TrafficLight(private val id: String, var redDuration: Long, var yellowDuration: Long, var greenDuration: Long) {
    var currentSignal: Signal
        private set

    init {
        this.currentSignal = Signal.RED
    }

    @Synchronized
    fun changeSignal(newSignal: Signal) {
        currentSignal = newSignal
        notifyObservers(newSignal)
    }

    private fun notifyObservers(newSignal: Signal) {
        println("Traffic Light $id changed to $newSignal")
    }
}

suspend fun main() {
    val trafficController: TrafficController = TrafficController.INSTANCE


    // Create roads
    val road1 = Road("R1", "Main Street")
    val road2 = Road("R2", "Broadway")
    val road3 = Road("R3", "Park Avenue")
    val road4 = Road("R4", "Elm Street")


    // Create traffic lights
    val trafficLight1 = TrafficLight("TL1", 2000, 1000, 2000)
    val trafficLight2 = TrafficLight("TL2", 2000, 1000, 2000)
    val trafficLight3 = TrafficLight("TL3", 2000, 1000, 2000)
    val trafficLight4 = TrafficLight("TL4", 2000, 1000, 2000)


    // Assign traffic lights to roads
    road1.trafficLight = trafficLight1
    road2.trafficLight = trafficLight2
    road3.trafficLight = trafficLight3
    road4.trafficLight = trafficLight4


    // Add roads to the traffic controller
    trafficController.addRoad(road1)
    trafficController.addRoad(road2)
    trafficController.addRoad(road3)
    trafficController.addRoad(road4)

    // Start traffic control concurrently for all roads
    trafficController.startTrafficControl()

    // Simulate an emergency on a specific road after a delay
    delay(13000)
    trafficController.handleEmergency("R2")

    // Simulate resolving the emergency after some time
    delay(20000)
    trafficController.resolveEmergency()

    delay(30000)
}
