package questions.concertTicketingSystem

import java.time.LocalDateTime
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.function.Consumer
import kotlin.streams.toList


data class Concert(
    val id: String,
    val artist: String,
    val venue: String,
    val time: LocalDateTime,
    val seats: List<Seat>
)

data class Seat(
    val id: String,
    val seatNumber: String,
    val seatType: SeatType,
    var status: SeatStatus = SeatStatus.AVAILABLE,
    val price: Double
) {
    fun book() {
        synchronized(this) {
            status = SeatStatus.BOOKED
        }
    }

    fun release() {
        synchronized(this) {
            status = SeatStatus.AVAILABLE
        }

    }
}

enum class SeatType {
    REGULAR,
    PREMIUM,
    VIP
}

enum class SeatStatus {
    AVAILABLE,
    BOOKED
}

data class User(
    val id: String,
    val name: String,
    val emailId: String
)

data class Booking(
    val id: String,
    val user: User,
    val concert: Concert,
    val seats: List<Seat>,
    var totalPrice: Double = 0.0,
    var status: BookingStatus? = null
) {
    init {
        totalPrice = calculateTotalPrice()
    }

    private fun calculateTotalPrice(): Double {
        return seats.stream().mapToDouble(Seat::price).sum()
    }

    fun confirmBooking() {
        if (status === BookingStatus.PENDING) {
            status = BookingStatus.CONFIRMED
            // Send booking confirmation to the user
            // ...
        }
    }

    fun cancelBooking() {
        if (status === BookingStatus.CONFIRMED) {
            status = BookingStatus.CANCELLED
            seats.forEach(Seat::release)
            System.out.printf("Booking %s cancelled\n", id)
            // Send booking cancellation notification to the user
            // ...
        }
    }

}

enum class BookingStatus {
    PENDING,
    CONFIRMED,
    CANCELLED
}

class SeatNotAvailableException(s: String) : RuntimeException()

class ConcertBookingSystem private constructor(
    private val concerts: MutableMap<String, Concert> = ConcurrentHashMap(),
    private val bookings: MutableMap<String, Booking> = ConcurrentHashMap()
){
    fun addConcert(concert: Concert){
        concerts[concert.id] = concert
    }

    fun searchConcerts(artist: String?, venue: String?, dateTime: LocalDateTime?): List<Concert> {
        return concerts.values.filter { concert ->
                concert.artist == artist &&
                        concert.venue == venue &&
                        concert.time == dateTime
            }
    }

    fun bookTickets(user: User?, concert: Concert?, seats: List<Seat>): Booking {
        synchronized(this) {
            // Check seat availability and book seats
            for ((_, seatNumber, _, status) in seats) {
                if (status != SeatStatus.AVAILABLE) {
                    throw SeatNotAvailableException("Seat $seatNumber is not available.")
                }
            }
            seats.forEach(Consumer { obj: Seat -> obj.book() })

            // Create booking
            val bookingId: String = generateBookingId()
            val booking = Booking(bookingId, user!!, concert!!, seats)
            bookings[bookingId] = booking

            // Process payment
            processPayment(booking)

            // Confirm booking
            booking.confirmBooking()

            println("Booking " + booking.id + " - " + booking.seats.size + " seats booked at price " + booking.totalPrice)
            return booking
        }
    }

    fun cancelBooking(bookingId: String?) {
        val booking = bookings[bookingId]
        if (booking != null) {
            booking.cancelBooking()
            bookings.remove(bookingId)
        }
    }

    private fun processPayment(booking: Booking) {
        // Process payment for the booking
        // ...
    }

    private fun generateBookingId(): String {
        return "BKG" + UUID.randomUUID()
    }


    companion object {
        val INSTANCE: ConcertBookingSystem by lazy(LazyThreadSafetyMode.SYNCHRONIZED) { ConcertBookingSystem() }
    }
}

fun main() {
    // Create concert ticket booking system instance
    val bookingSystem: ConcertBookingSystem = ConcertBookingSystem.INSTANCE

    // Create concerts
    val concert1Seats = generateSeats(100)
    val concert1Date = LocalDateTime.now().plusDays(30)

    val concert1 = Concert("C001", "Artist 1", "Venue 1", concert1Date, concert1Seats)
    bookingSystem.addConcert(concert1)

    val concert2Seats = generateSeats(50)
    val concert2 = Concert("C002", "Artist 2", "Venue 2", LocalDateTime.now().plusDays(60), concert2Seats)
    bookingSystem.addConcert(concert2)

    // Create users
    val user1 = User("U001", "John Doe", "john@example.com")
    val user2 = User("U002", "Jane Smith", "jane@example.com")

    // Search concerts
    val searchResults: List<Concert> =
        bookingSystem.searchConcerts("Artist 1", "Venue 1", concert1Date)
    println("Search Results: $searchResults")
    for ((_, _, artist, venue) in searchResults) {
        println("Concert: $artist at $venue")
    }

    // Book tickets
    val selectedSeats1 = selectSeats(concert1, 3)
    val booking1: Booking = bookingSystem.bookTickets(user1, concert1, selectedSeats1)

    val selectedSeats2 = selectSeats(concert2, 2)
    val booking2: Booking = bookingSystem.bookTickets(user2, concert2, selectedSeats2)

    // Cancel booking
    bookingSystem.cancelBooking(booking1.id)

    // Book tickets again
    val selectedSeats3 = selectSeats(concert1, 2)
    val booking3: Booking = bookingSystem.bookTickets(user2, concert1, selectedSeats3)
}

private fun generateSeats(numberOfSeats: Int): List<Seat> {
    val seats: MutableList<Seat> = ArrayList()
    for (i in 1..numberOfSeats) {
        val seatNumber = "S$i"
        val seatType = if ((i <= 10)) SeatType.VIP else if ((i <= 30)) SeatType.PREMIUM else SeatType.REGULAR
        val price = if ((seatType == SeatType.VIP)) 100.0 else if ((seatType == SeatType.PREMIUM)) 75.0 else 50.0
        seats.add(Seat(seatNumber, seatNumber, seatType, price = price))
    }
    return seats
}

private fun selectSeats(concert: Concert, numberOfSeats: Int): List<Seat> {
    val selectedSeats: MutableList<Seat> = ArrayList()
    val availableSeats: List<Seat> = concert.seats.stream()
        .filter { seat: Seat -> seat.status == SeatStatus.AVAILABLE }
        .limit(numberOfSeats.toLong())
        .toList()
    selectedSeats.addAll(availableSeats)
    return selectedSeats
}