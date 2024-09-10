package questions.uber

import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue

data class Location(
    val longitude: Long,
    val latitude: Long
)

enum class RideType {
    REGULAR,
    SEDAN,
    PREMIUM
}

data class RideRequest(
    val id: String,
    val pickUpLocation: Location,
    val destinationLocation: Location,
    val desiredRideType: RideType
)

enum class DriverStatus {
    AVAILABLE,
    BUSY
}

data class Driver(
    val id: Int,
    val name: String,
    val contact: String,
    val licensePlate: String,
    val location: Location,
    var status: DriverStatus
)

data class Passenger(
    val id: Int,
    val name: String,
    val contact: String,
    val location: Location
)

class Payment {
    private val id = 0
    private val ride: Ride? = null
    private val amount = 0.0
    private val status: PaymentStatus? = null
}

enum class PaymentStatus {
    PENDING,
    COMPLETED,
    FAILED
}

data class Ride(
    val id: Int,
    val passenger: Passenger,
    var driver: Driver?,
    val pickUpLocation: Location,
    val destinationLocation: Location,
    var rideStatus: RideStatus,
    var fare: Double
)

enum class RideStatus {
    REQUESTED,
    ACCEPTED,
    IN_PROGRESS,
    COMPLETED,
    CANCELLED
}

class RideService private constructor() {
    private val passengers: MutableMap<Int, Passenger> = ConcurrentHashMap()
    private val drivers: MutableMap<Int, Driver> = ConcurrentHashMap()
    val rides: Map<Int, Ride> = ConcurrentHashMap()
    val requestedRides: Queue<Ride> = ConcurrentLinkedQueue()

    fun addPassenger(passenger: Passenger) {
        passengers[passenger.id] = passenger
    }

    fun addDriver(driver: Driver) {
        drivers[driver.id] = driver
    }

    fun requestRide(passenger: Passenger, source: Location?, destination: Location?) {
        val ride = Ride(
            generateRideId(),
            passenger,
            null,
            source!!,
            destination!!,
            RideStatus.REQUESTED,
            0.0
        )
        requestedRides.offer(ride)
        notifyDrivers(ride)
    }

    fun acceptRide(driver: Driver, ride: Ride) {
        if (ride.rideStatus == RideStatus.REQUESTED) {
            ride.driver = driver
            ride.rideStatus = RideStatus.ACCEPTED
            driver.status = DriverStatus.BUSY
            notifyPassenger(ride)
        }
    }

    fun startRide(ride: Ride) {
        if (ride.rideStatus === RideStatus.ACCEPTED) {
            ride.rideStatus = RideStatus.IN_PROGRESS
            notifyPassenger(ride)
        }
    }

    fun completeRide(ride: Ride) {
        if (ride.rideStatus === RideStatus.IN_PROGRESS) {
            ride.rideStatus = RideStatus.COMPLETED
            ride.driver?.status = DriverStatus.AVAILABLE
            ride.fare = calculateFare(ride)
            processPayment(ride, ride.fare)
            notifyPassenger(ride)
            notifyDriver(ride)
        }
    }

    private fun notifyPassenger(ride: Ride) {
        // Notify the passenger about ride status updates
        // ...
        val passenger: Passenger = ride.passenger
        var message = ""
        when (ride.rideStatus) {
            RideStatus.ACCEPTED -> message = "Your ride has been accepted by driver: " + ride.driver?.name
            RideStatus.IN_PROGRESS -> message = "Your ride is in progress"
            RideStatus.COMPLETED -> message = "Your ride has been completed. Fare: $" + ride.fare
            RideStatus.CANCELLED -> message = "Your ride has been cancelled"
            RideStatus.REQUESTED -> {}
        }
        // Send notification to the passenger
        println("Notifying passenger: " + passenger.id + " - " + message)
    }

    private fun notifyDrivers(ride: Ride) {
        for (driver in drivers.values) {
            if (driver.status === DriverStatus.AVAILABLE) {
                val distance = calculateDistance(driver.location, ride.pickUpLocation)
                if (distance <= 5.0) { // Notify drivers within 5 km radius
                    // Send notification to the driver
                    System.out.println(("Notifying driver: " + driver.name).toString() + " about ride request: " + ride.id)
                }
            }
        }
    }


    fun processTransaction() {

    }

//    fun findNearestDrivers(rideRequest: RideRequest): List<Driver> {
//        val availableDrivers = drivers.filter { it.isAvailable && it.rideType == rideRequest.desiredRideType }
//        return availableDrivers.filter { driver ->
//            isNearBy(driver.location, rideRequest.pickUpLocation)
//        }
//    }

    // could be two cases in future
    // driver is available and nearby
    // driver is finishing a ride and is near by pick up location
    // for now the pickup location and current location of driver is compared
    fun isNearBy(location1: Location, location2: Location): Boolean {
        return location1.latitude - location2.latitude < 5 && location1.longitude - location1.latitude < 5
    }

    fun notifyDriver(ride: Ride) {
        val driver = ride.driver
        if (driver != null) {
            var message = ""
            when (ride.rideStatus) {
                RideStatus.COMPLETED -> message = "Ride completed. Fare: $" + ride.fare
                RideStatus.CANCELLED -> message = "Ride cancelled by passenger"
                else -> {}
            }
            // Send notification to the driver
            System.out.println("Notifying driver: " + driver.name + " - " + message)
        }
    }

    fun calculateFare(ride: Ride): Double {
        val baseFare = 2.0
        val perKmFare = 1.5
        val perMinuteFare = 0.25

        val distance = calculateDistance(ride.pickUpLocation, ride.destinationLocation)
        val duration = calculateDuration(ride.pickUpLocation, ride.destinationLocation)

        val fare = baseFare + (distance * perKmFare) + (duration * perMinuteFare)
        return Math.round(fare * 100.0) / 100.0 // Round to 2 decimal places
    }

    fun calculateDistance(source: Location?, Destination: Location?): Double {
        // Calculate the distance between two locations using a distance formula (e.g., Haversine formula)
        // For simplicity, let's assume a random distance between 1 and 20 km
        return Math.random() * 20 + 1
    }

    private fun calculateDuration(source: Location, destination: Location): Double {
        // Calculate the estimated duration between two locations based on distance and average speed
        // For simplicity, let's assume an average speed of 30 km/h
        val distance = calculateDistance(source, destination)
        return (distance / 30) * 60 // Convert hours to minutes
    }

    private fun processPayment(ride: Ride, amount: Double) {
        // Process the payment for the ride
        // ...
    }

    private fun generateRideId(): Int {
        return (System.currentTimeMillis() / 1000).toInt()
    }

    fun cancelRide(ride: Ride) {
        if (ride.rideStatus === RideStatus.REQUESTED || ride.rideStatus === RideStatus.ACCEPTED) {
            ride.rideStatus = RideStatus.CANCELLED
            ride.driver?.status = DriverStatus.AVAILABLE
            notifyPassenger(ride)
            notifyDriver(ride)
        }
    }

    companion object {
        val INSTANCE: RideService by lazy(LazyThreadSafetyMode.SYNCHRONIZED) { RideService() }
    }
}

fun main() {
    val rideService: RideService = RideService.INSTANCE

    // Create passengers
    val passenger1 = Passenger(1, "John Doe", "1234567890", Location(37.7749.toLong(), -122.4194.toLong()))
    val passenger2 = Passenger(2, "Jane Smith", "9876543210", Location(37.7860.toLong(), (-122.4070).toLong()))
    rideService.addPassenger(passenger1)
    rideService.addPassenger(passenger2)

    // Create drivers
    val driver1 = Driver(
        1,
        "Alice Johnson",
        "4567890123",
        "ABC123",
        Location(37.7749.toLong(), -122.4194.toLong()),
        DriverStatus.AVAILABLE
    )
    val driver2 = Driver(
        2,
        "Bob Williams",
        "7890123456",
        "XYZ789",
        Location(37.7860.toLong(), -122.4070.toLong()),
        DriverStatus.AVAILABLE
    )
    rideService.addDriver(driver1)
    rideService.addDriver(driver2)

    // Passenger 1 requests a ride
    rideService.requestRide(passenger1, passenger1.location, Location(37.7887.toLong(), -122.4098.toLong()))

    // Driver 1 accepts the ride
    val ride = rideService.requestedRides.poll()
    rideService.acceptRide(driver1, ride)

    // Start the ride
    rideService.startRide(ride)

    // Complete the ride
    rideService.completeRide(ride)

    // Passenger 2 requests a ride
    rideService.requestRide(passenger2, passenger2.location, Location(37.7749.toLong(), -122.4194.toLong()))

    // Driver 2 accepts the ride
    val ride2 = rideService.requestedRides.poll()
    rideService.acceptRide(driver2, ride2)

    // Passenger 2 cancels the ride
    rideService.cancelRide(ride2)
}