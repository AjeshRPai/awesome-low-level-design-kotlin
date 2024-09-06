package questions.elevatorSystem

import kotlinx.coroutines.*
import kotlin.math.abs

enum class ElevatorDirection {
    UP, DOWN, IDLE
}

data class ElevatorRequest(
    val sourceFloor: Int,
    val destinationFloor: Int
)

data class Elevator(
    val id: Int,
    val capacityLimit: Int,
    var currentFloor: Int = 0,
    var currentDirection: ElevatorDirection = ElevatorDirection.IDLE,
    private val requests: MutableList<ElevatorRequest> = mutableListOf()
) {
    private val scope = CoroutineScope(Dispatchers.Default + Job())

    fun addRequest(request: ElevatorRequest) {
        synchronized(requests) {
            requests.add(request)
            requests.sortWith(compareBy { abs(it.sourceFloor - currentFloor) })
        }
    }

    private suspend fun moveToFloor(floor: Int) {
        println("Elevator $id moving from $currentFloor to $floor")
        delay(500) // Simulating movement time
        currentFloor = floor
        println("Elevator $id reached floor $floor")
    }

    private suspend fun processRequest(request: ElevatorRequest) {
        if (currentFloor != request.sourceFloor) {
            currentDirection = if (request.sourceFloor > currentFloor) ElevatorDirection.UP else ElevatorDirection.DOWN
            moveToFloor(request.sourceFloor)
        }
        currentDirection = if (request.destinationFloor > currentFloor) ElevatorDirection.UP else ElevatorDirection.DOWN
        moveToFloor(request.destinationFloor)
        currentDirection = ElevatorDirection.IDLE
    }

    fun run() {
        scope.launch {
            while (true) {
                val request = getNextRequest()
                if (request != null) {
                    processRequest(request)
                } else {
                    delay(100) // No request, avoid busy-waiting
                }
            }
        }
    }

    private fun getNextRequest(): ElevatorRequest? {
        synchronized(requests) {
            return if (requests.isNotEmpty()) requests.removeAt(0) else null
        }
    }
}

class ElevatorController(numElevators: Int, capacity: Int) {
    private val elevators: List<Elevator> = List(numElevators) { Elevator(it + 1, capacity) }

    init {
        elevators.forEach { it.run() }
    }

    fun requestElevator(sourceFloor: Int, destinationFloor: Int) {
        val optimalElevator = findOptimalElevator(sourceFloor)
        optimalElevator.addRequest(ElevatorRequest(sourceFloor, destinationFloor))
    }

    private fun findOptimalElevator(sourceFloor: Int): Elevator {
        return elevators.minByOrNull { abs(it.currentFloor - sourceFloor) + (if (it.currentDirection == ElevatorDirection.IDLE) 0 else 1) }!!
    }
}

fun main() {
    val controller = ElevatorController(3, 5)

    controller.requestElevator(5, 10)
    controller.requestElevator(3, 7)
    controller.requestElevator(8, 2)
    controller.requestElevator(1, 9)

    // Allow time for processing requests
    Thread.sleep(10000)
}
