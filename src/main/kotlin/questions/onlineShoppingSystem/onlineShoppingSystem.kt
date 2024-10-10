package questions.onlineShoppingSystem

import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

data class User(
    val id: String,
    val name: String,
    val email: String,
    val password: String,
    var orders: MutableList<Order> = mutableListOf<Order>()
) {
    fun addOrder(order: Order) {
        orders.add(order)
    }
}

data class Product(
    val id: String,
    val name: String,
    val description: String,
    val price: Double,
    var quantity: Int
) {
    fun hasQuantity(quantity: Int): Boolean {
        synchronized(this) {
            return quantity < this.quantity
        }
    }

    fun reduceQuantity(quantity: Int) {
        synchronized(this) {
            this.quantity -= quantity
        }
    }

    fun addQuantity(quantity: Int) {
        synchronized(this) {
            this.quantity += quantity
        }
    }
}

data class Order(
    val id: String,
    val user: User,
    val items: List<OrderItem>,
    var status: OrderStatus? = null
) {
    fun getTotalAmount(): Double {
        return items.map { it.product.price * it.quantity }
            .reduce { sum, itemPrice -> sum + itemPrice }
    }
}

class OrderItem(
    val product: Product,
    val quantity: Int
) {
}

enum class OrderStatus {
    PROCESSING,
    CANCELLED
}

class ShoppingCart {
    val items: MutableMap<String, OrderItem> = mutableMapOf()

    fun addItem(product: Product, quantity: Int) {
        val productId = product.id
        val existingQuantity = items[productId]?.quantity ?: 0
        items[productId] = OrderItem(product, existingQuantity + quantity)
    }

    fun removeItem(productId: String) {
        items.remove(productId)
    }

    fun updateItemQuantity(productId: String, quantity: Int) {
        val item = items[productId]
        if (item != null) {
            items[productId] = OrderItem(item.product, quantity)
        }
    }

    fun getItems(): List<OrderItem> {
        return items.values.toList()
    }

    fun clear() {
        items.clear()
    }
}


abstract class Payment() {
    abstract fun processPayment(totalAmount: Double): Boolean
}

class CardPayment : Payment() {
    override fun processPayment(totalAmount: Double): Boolean {
        return true
    }
}

class OnlineShoppingService {
    val users: MutableMap<String, User> = ConcurrentHashMap()
    val products: MutableMap<String, Product> = ConcurrentHashMap()
    val orders: MutableMap<String, Order> = ConcurrentHashMap()

    fun registerUser(user: User) {
        users[user.id] = user
    }

    fun getUser(userId: String): User? {
        return users[userId]
    }

    fun addProduct(product: Product) {
        products[product.id] = product
    }

    fun getProduct(productId: String): Product? {
        return products[productId]
    }

    fun searchProducts(keyword: String): List<Product> {
        return products.values.filter { product -> product.name.contains(keyword, ignoreCase = true) }
    }


    fun placeOrder(user: User, cart: ShoppingCart, payment: Payment): Order {
        val orderItems = arrayListOf<OrderItem>()
        for (item in cart.getItems()) {
            if (item.product.hasQuantity(item.quantity)) {
                item.product.reduceQuantity(item.quantity)
                orderItems.add(item)
            }
        }
        orderItems.ifEmpty { throw IllegalStateException("No available products in the cart") }

        val orderId = generateOrderId()
        val order = Order(orderId, user, orderItems)
        orders.put(orderId, order)
        user.addOrder(order)
        cart.clear()

        if (payment.processPayment(order.getTotalAmount())) {
            order.status = OrderStatus.PROCESSING
        } else {
            order.status = OrderStatus.CANCELLED
            for (item in orderItems) {
                val product = item.product
                val quantity = item.quantity
                product.addQuantity(quantity)
            }
        }
        return order
    }

    private fun generateOrderId(): String {
        return UUID.randomUUID().toString()
    }

    companion object {
        val INSTANCE by lazy(LazyThreadSafetyMode.SYNCHRONIZED) { OnlineShoppingService() }
    }
}

fun main() {
    val shoppingService = OnlineShoppingService.INSTANCE

    // Register users
    val user1 = User("U001", "John Doe", "john@example.com", "password123")
    val user2 = User("U002", "Jane Smith", "jane@example.com", "password456")
    shoppingService.registerUser(user1)
    shoppingService.registerUser(user2)

    // Add products
    val product1 = Product("P001", "Smartphone", "High-end smartphone", 999.99, 10)
    val product2 = Product("P002", "Laptop", "Powerful gaming laptop", 1999.99, 5)
    shoppingService.addProduct(product1)
    shoppingService.addProduct(product2)

    // User 1 adds products to cart and places an order
    val cart1 = ShoppingCart()
    cart1.addItem(product1, 2)
    cart1.addItem(product2, 1)
    val payment1: Payment = CardPayment()
    val order1 = shoppingService.placeOrder(user1, cart1, payment1)
    println("Order placed: ${order1.id}")

    // User 2 searches for products and adds to cart
    val searchResults = shoppingService.searchProducts("laptop")
    println("Search Results:")
    searchResults.forEach { product -> println(product.name) }

    val cart2 = ShoppingCart()
    cart2.addItem(searchResults[0], 1)
    val payment2: Payment = CardPayment()
    val order2 = shoppingService.placeOrder(user2, cart2, payment2)
    println("Order placed: ${order2.id}")

    // User 1 views order history
    val userOrders = user1.orders
    println("User 1 Order History:")
    userOrders.forEach { order ->
        println("Order ID: ${order.id}")
        println("Total Amount: \$${order.getTotalAmount()}")
        println("Status: ${order.status}")
    }

}





