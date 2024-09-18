package questions.onlineBrokerageSystem

import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicInteger

data class Stock(
    val symbol: String,
    val name: String,
    private var _price: Double
) {
    private val observers: MutableSet<StockObserver> = mutableSetOf()

    var price: Double
        get() = _price
        set(value) {
            if (_price != value) {
                _price = value
                notifyObservers()
            }
        }

    fun addObserver(observer: StockObserver) {
        observers.add(observer)
    }

    fun removeObserver(observer: StockObserver) {
        observers.remove(observer)
    }

    private fun notifyObservers() {
        observers.forEach { it.update(this) }
    }
}

// 1. Create an interface for observers
interface StockObserver {
    fun update(stock: Stock)
}

data class User(
    val id: String,
    val name: String,
    val email: String
) : StockObserver {
    override fun update(stock: Stock) {
        println("Notification for user $name: The price of ${stock.symbol} has changed to ${stock.price}")
    }
}


data class Account(
    val accountId: String,
    val user: User,
    var balance: Double
) {
    var portfolio: Portfolio = Portfolio(this)

    fun deposit(amount: Double) {
        synchronized(this) {
            balance += amount
        }
    }

    fun withdraw(amount: Double) {
        synchronized(this) {
            if (balance >= amount) {
                balance -= amount
            } else {
                throw InsufficientFundsException()
            }
        }
    }

    private val subscribedStocks: MutableSet<Stock> = mutableSetOf()

    fun subscribeToStock(stock: Stock) {
        subscribedStocks.add(stock)
        stock.addObserver(user)
    }

    fun unsubscribeFromStock(stock: Stock) {
        subscribedStocks.remove(stock)
        stock.removeObserver(user)
    }
}

abstract class Order(
    protected val orderId: String,
    protected val account: Account,
    protected val stock: Stock,
    protected val quantity: Int,
    protected val price: Double
) {
    protected var status: OrderStatus = OrderStatus.PENDING

    abstract fun execute()
}

class SellOrder(orderId: String, account: Account, stock: Stock, quantity: Int, price: Double) :
    Order(orderId, account, stock, quantity, price) {
    override fun execute() {
        // Check if the user has sufficient quantity of the stock to sell
        // Update portfolio and perform necessary actions
        val totalProceeds = quantity * price
        account.deposit(totalProceeds)
        status = OrderStatus.EXECUTED
        account.unsubscribeFromStock(stock)
        account.portfolio.removeStock(stock, quantity)
    }
}

class BuyOrder(orderId: String, account: Account, stock: Stock, quantity: Int, price: Double) :
    Order(orderId, account, stock, quantity, price) {
    override fun execute() {
        val totalCost = quantity * price
        if (account.balance >= totalCost) {
            account.withdraw(totalCost)
            status = OrderStatus.EXECUTED
            println("Buy order executed for $quantity of stock $stock at price $price with total cost at $totalCost")
            account.subscribeToStock(stock)
            account.portfolio.addStock(stock, quantity)
        } else {
            status = OrderStatus.REJECTED
            throw InsufficientFundsException("Insufficient funds to execute the buy order. Account balance is ${account.balance} and the total cost is totalCost $totalCost")
        }
    }
}

enum class OrderStatus {
    PENDING,
    EXECUTED,
    REJECTED
}

class Portfolio(private val account: Account) {
    val holdings: MutableMap<String, Int> = ConcurrentHashMap()

    fun addStock(stock: Stock, quantity: Int) {
        synchronized(holdings) {
            holdings[stock.symbol] = holdings.getOrDefault(stock.symbol, 0) + quantity
        }
    }

    fun removeStock(stock: Stock, quantity: Int) {
        synchronized(holdings) {
            val symbol = stock.symbol
            if (holdings.containsKey(symbol)) {
                val currentQuantity = holdings[symbol]!!
                if (currentQuantity > quantity) {
                    holdings[symbol] = currentQuantity - quantity
                } else if (currentQuantity == quantity) {
                    holdings.remove(symbol)
                } else {
                    throw InSufficientStockException("Insufficient stock quantity in the portfolio.")
                }
            } else {
                throw InSufficientStockException("Stock not found in the portfolio.")
            }
        }
    }
}

class InsufficientFundsException(override val message: String? = "User has insufficient funds") : Exception()

class InSufficientStockException(override val message: String? = "There are no stocks available to buy") : Exception()


class StockBroker private constructor() {
    private val accounts: MutableMap<String, Account> = ConcurrentHashMap()
    private val stocks: MutableMap<String, Stock> = ConcurrentHashMap()
    private val orderQueue: Queue<Order> = ConcurrentLinkedQueue()
    private val accountIdCounter = AtomicInteger(1)

    fun createAccount(user: User?, initialBalance: Double) {
        val accountId = generateAccountId()
        val account = Account(accountId, user!!, initialBalance)
        accounts[accountId] = account
    }

    fun getAccount(accountId: String): Account? {
        return accounts[accountId]
    }

    fun addStock(stock: Stock) {
        stocks[stock.symbol] = stock
    }

    fun getStock(symbol: String): Stock? {
        return stocks[symbol]
    }

    fun placeOrder(order: Order) {
        orderQueue.offer(order)
        processOrders()
    }

    private fun processOrders() {
        while (!orderQueue.isEmpty()) {
            val order = orderQueue.poll()
            try {
                order.execute()
            } catch (e: InsufficientFundsException) {
                // Handle exception and notify user
                System.out.println("Order failed: " + e.message)
            } catch (e: InSufficientStockException) {
                System.out.println("Order failed: " + e.message)
            }
        }
    }

    fun updateStockPrice(symbol: String, newPrice: Double) {
        val stock = stocks[symbol]
        if (stock != null) {
            stock.price = newPrice
        } else {
            println("Stock with symbol $symbol not found")
        }
    }

    private fun generateAccountId(): String {
        val accountId = accountIdCounter.getAndIncrement()
        return "A" + String.format("%03d", accountId)
    }

    companion object {
        val INSTANCE: StockBroker by lazy(LazyThreadSafetyMode.SYNCHRONIZED) { StockBroker() }
    }
}

fun main() {
    val stockBroker: StockBroker = StockBroker.INSTANCE

    // Create user and account
    val user = User("U001", "John Doe", "john@example.com")
    stockBroker.createAccount(user, 10000.0)
    val account = stockBroker.getAccount("A001")
    println(account)

    // Add stocks to the stock broker
    val stock1 = Stock("AAPL", "Apple Inc.", 150.0)
    val stock2 = Stock("GOOGL", "Alphabet Inc.", 2000.0)
    stockBroker.addStock(stock1)
    stockBroker.addStock(stock2)

    // Place buy orders
    val buyOrder1: Order = BuyOrder("O001", account!!, stock1, 10, 150.0)
    val buyOrder2: Order = BuyOrder("O002", account, stock2, 5, 2000.0)
    stockBroker.placeOrder(buyOrder1)
    stockBroker.placeOrder(buyOrder2)

    // Update stock price
    stockBroker.updateStockPrice("AAPL", 155.0)

    // Place sell orders
    val sellOrder1: Order = SellOrder("O003", account, stock1, 5, 160.0)
    stockBroker.placeOrder(sellOrder1)

    // account should not receive notification
    stockBroker.updateStockPrice("AAPL", 162.0)

    // Print account balance and portfolio
    println("Account Balance: $" + account.balance)
    println("Portfolio: " + account.portfolio.holdings)
}