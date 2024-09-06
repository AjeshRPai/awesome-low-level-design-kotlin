package questions.parkinglot

import java.util.concurrent.ConcurrentHashMap

val EMPTY_LICENSE_PLATE = ""

object ParkingLot {
    private val levels: MutableList<Level> = mutableListOf()
    private val vehicleSpotMap: ConcurrentHashMap<String, ParkingSpot> = ConcurrentHashMap()

    fun addLevel(level: Level) {
        levels.add(level)
    }

    @Synchronized
    fun parkVehicle(vehicle: Vehicle): Boolean {
        // Check if the vehicle is already parked
        if (vehicleSpotMap.containsKey(vehicle.licensePlate)) {
            println("Vehicle with license plate ${vehicle.licensePlate} is already parked.")
            return false
        }

        // Find an available spot and park the vehicle
        for (level in levels) {
            val spot = level.findAvailableSpot(vehicle)
            if (spot != null) {
                spot.parkVehicle(vehicle)
                vehicleSpotMap[vehicle.licensePlate] = spot
                println("Vehicle parked at level ${level.floor}, spot ${spot.spotNumber}.")
                return true
            }
        }
        println("No available spots for vehicle type: ${vehicle.vehicleType}.")
        return false
    }

    @Synchronized
    fun unparkVehicle(vehicle: Vehicle): Boolean {
        // Find the spot from the map and unpark the vehicle
        val spot = vehicleSpotMap[vehicle.licensePlate]
        if (spot != null) {
            spot.unparkVehicle()
            vehicleSpotMap.remove(vehicle.licensePlate)
            println("Vehicle with license plate ${vehicle.licensePlate} unparked.")
            return true
        }
        println("Vehicle with license plate ${vehicle.licensePlate} not found.")
        return false
    }

    fun displayAvailability() {
        for (level in levels) {
            level.displayAvailability()
        }
    }
}

data class Level(val floor: Int, val capacity: Int) {
    private val parkingSpots: MutableList<ParkingSpot> = mutableListOf()

    init {
        for (i in 0 until capacity) {
            parkingSpots.add(ParkingSpot(spotNumber = i, spotType = SpotType.CAR_SPOT)) // Customize spot type as needed
        }
    }

    fun findAvailableSpot(vehicle: Vehicle): ParkingSpot? {
        // Return the first available spot that can fit the vehicle
        return parkingSpots.firstOrNull { it.isAvailable && it.canFitVehicle(vehicle) }
    }

    fun displayAvailability() {
        val availableCount = parkingSpots.count { it.isAvailable }
        println("Level $floor: $availableCount parking spots available")
    }
}

enum class SpotType {
    CAR_SPOT,
    TRUCK_SPOT,
    MOTORCYCLE_SPOT
}

data class ParkingSpot(val spotNumber: Int, val spotType: SpotType) {
    var licensePlate: String = EMPTY_LICENSE_PLATE
        private set
    var isAvailable: Boolean = true
        private set

    fun canFitVehicle(vehicle: Vehicle): Boolean {
        return when (vehicle.vehicleType) {
            VehicleType.CAR -> spotType == SpotType.CAR_SPOT
            VehicleType.TRUCK -> spotType == SpotType.TRUCK_SPOT
            VehicleType.MOTORCYCLE -> spotType == SpotType.MOTORCYCLE_SPOT
        }
    }

    fun parkVehicle(vehicle: Vehicle) {
        isAvailable = false
        licensePlate = vehicle.licensePlate
    }

    fun unparkVehicle() {
        isAvailable = true
        licensePlate = EMPTY_LICENSE_PLATE
    }
}

sealed class Vehicle(open val licensePlate: String, val vehicleType: VehicleType) {
    data class Car(override val licensePlate: String) : Vehicle(licensePlate, VehicleType.CAR)
    data class Truck(override val licensePlate: String) : Vehicle(licensePlate, VehicleType.TRUCK)
    data class Motorcycle(override val licensePlate: String) : Vehicle(licensePlate, VehicleType.MOTORCYCLE)
}

enum class VehicleType {
    CAR,
    TRUCK,
    MOTORCYCLE
}

fun main() {
    val parkingLot = ParkingLot

    parkingLot.addLevel(Level(floor = 1, capacity = 100))
    parkingLot.addLevel(Level(floor = 2, capacity = 80))

    val car = Vehicle.Car("ABC123")
    val truck = Vehicle.Truck("XYZ789")
    val motorcycle = Vehicle.Motorcycle("M1234")

    parkingLot.parkVehicle(car)
    parkingLot.parkVehicle(truck)
    parkingLot.parkVehicle(motorcycle)

    parkingLot.displayAvailability()

    parkingLot.unparkVehicle(motorcycle)

    parkingLot.displayAvailability()
}
