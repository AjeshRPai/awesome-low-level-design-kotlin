package questions.splitwise

import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.abs

// Data classes for User and Expense entities
data class User(
    val id: String,
    val name: String,
    val email: String,
    val balances: MutableMap<CompositeKey, Double> = mutableMapOf()
)

data class Expense(
    val id: String,
    val description: String,
    val amount: Double,
    val paidBy: User,
    val splits: List<Split>
)

data class Group(
    val id: String,
    val name: String,
    val members: MutableList<User> = mutableListOf(),
    val expenses: MutableList<Expense> = mutableListOf()
) {
    fun addExpense(expense: Expense) {
        expenses.add(expense)
    }

    fun addMember(user: User) {
        members.add(user)
    }
}

// CompositeKey class for consistent balance keys
data class CompositeKey(val user1: String, val user2: String) {
    init {
        require(user1 < user2) { "user1 should be less than user2 for consistent ordering" }
    }

    companion object {
        fun create(id1: String, id2: String): CompositeKey {
            return if (id1 < id2) CompositeKey(id1, id2) else CompositeKey(id2, id1)
        }
    }
}

// Refactor Split into a simple data class
data class Split(val user: User, val amount: Double)

// Define an interface for different split strategies
interface SplitStrategy {
    fun calculateSplits(amount: Double, users: List<User>): List<Split>
}

class EqualSplitStrategy : SplitStrategy {
    override fun calculateSplits(amount: Double, users: List<User>): List<Split> {
        val splitAmount = amount / users.size
        return users.map { user -> Split(user, splitAmount) }
    }
}

class PercentSplitStrategy(private val percents: Map<User, Double>) : SplitStrategy {
    override fun calculateSplits(amount: Double, users: List<User>): List<Split> {
        return users.map { user ->
            val percent = percents[user] ?: 0.0
            Split(user, amount * percent / 100)
        }
    }
}

// Services for handling users, groups, expenses, and transactions
class UserService {
    private val users: MutableMap<String, User> = ConcurrentHashMap()

    fun addUser(user: User) {
        users[user.id] = user
    }

    fun getUser(userId: String): User? = users[userId]
}

class GroupService {
    private val groups: MutableMap<String, Group> = ConcurrentHashMap()

    fun addGroup(group: Group) {
        groups[group.id] = group
    }

    fun getGroup(groupId: String): Group? = groups[groupId]
}

class ExpenseService(private val userService: UserService) {

    fun addExpense(group: Group, description: String, amount: Double, paidBy: User, splitStrategy: SplitStrategy) {
        val splits = splitStrategy.calculateSplits(amount, group.members)
        val expense = Expense(UUID.randomUUID().toString(), description, amount, paidBy, splits)
        group.addExpense(expense)
        updateBalances(expense)
    }

    private fun updateBalances(expense: Expense) {
        for (split in expense.splits) {
            val paidBy = expense.paidBy
            val user = split.user
            val amount = split.amount

            if (paidBy != user) {
                updateBalance(paidBy, user, amount)
                updateBalance(user, paidBy, -amount)
            }
        }
    }

    private fun updateBalance(user1: User, user2: User, amount: Double) {
        val key = CompositeKey.create(user1.id, user2.id)
        val currentBalance = user1.balances.getOrDefault(key, 0.0)
        user1.balances[key] = currentBalance + amount
    }
}

class TransactionService {
    private val transactionCounter = AtomicInteger(0)
    private val transactions: MutableList<Transaction> = mutableListOf()

    fun createTransaction(sender: User, receiver: User, amount: Double) {
        val transactionId = generateTransactionId()
        val transaction = Transaction(transactionId, sender, receiver, amount)
        transactions.add(transaction)
        println("Transaction created: $transaction")
    }

    private fun generateTransactionId(): String {
        val transactionNumber = transactionCounter.incrementAndGet()
        return "TXN" + String.format("%06d", transactionNumber)
    }
}

// Data class for Transactions
data class Transaction(
    val id: String,
    val sender: User,
    val receiver: User,
    val amount: Double
)

// Main application class that integrates all services
class SplitwiseService {
    private val userService = UserService()
    private val groupService = GroupService()
    private val expenseService = ExpenseService(userService)
    private val transactionService = TransactionService()

    fun addUser(user: User) = userService.addUser(user)

    fun addGroup(group: Group) = groupService.addGroup(group)

    fun addExpense(groupId: String, description: String, amount: Double, paidBy: User, splitStrategy: SplitStrategy) {
        val group = groupService.getGroup(groupId)
        if (group != null) {
            expenseService.addExpense(group, description, amount, paidBy, splitStrategy)
        } else {
            println("Group not found")
        }
    }

    fun settleBalance(userId1: String, userId2: String) {
        val user1 = userService.getUser(userId1)
        val user2 = userService.getUser(userId2)

        if (user1 != null && user2 != null) {
            val key = CompositeKey.create(user1.id, user2.id)
            val balance = user1.balances.getOrDefault(key, 0.0)

            if (balance > 0) {
                transactionService.createTransaction(user1, user2, balance)
                user1.balances[key] = 0.0
                user2.balances[key] = 0.0
            } else if (balance < 0) {
                transactionService.createTransaction(user2, user1, abs(balance))
                user1.balances[key] = 0.0
                user2.balances[key] = 0.0
            }
        }
    }

    companion object {
        val INSTANCE: SplitwiseService by lazy(LazyThreadSafetyMode.SYNCHRONIZED) { SplitwiseService() }
    }

}

// Main function to demonstrate the functionality
suspend fun main() {
    val splitwiseService = SplitwiseService.INSTANCE

    // Create users
    val user1 = User("1", "Alice", "alice@example.com")
    val user2 = User("2", "Bob", "bob@example.com")
    val user3 = User("3", "Charlie", "charlie@example.com")

    splitwiseService.addUser(user1)
    splitwiseService.addUser(user2)
    splitwiseService.addUser(user3)

    // Create a group
    val group = Group("1", "Apartment")
    group.addMember(user1)
    group.addMember(user2)
    group.addMember(user3)

    splitwiseService.addGroup(group)

    // Add an expense using EqualSplitStrategy
    val equalSplitStrategy = EqualSplitStrategy()
    splitwiseService.addExpense(group.id, "Rent", 300.0, user1, equalSplitStrategy)

    // Add an expense using PercentSplitStrategy
    val percentSplitStrategy = PercentSplitStrategy(mapOf(user1 to 40.0, user2 to 30.0, user3 to 30.0))
    splitwiseService.addExpense(group.id, "Groceries", 150.0, user2, percentSplitStrategy)

    // Settle balances
//    splitwiseService.settleBalance(user1.id, user2.id)
//    splitwiseService.settleBalance(user1.id, user3.id)

    // Print user balances
    for (user in listOf(user1, user2, user3)) {
        println("User: ${user.name}")
        for ((key, value) in user.balances) {
            println("  Balance with ${key.user1}:${key.user2} - Amount: $value")
        }
    }
}
