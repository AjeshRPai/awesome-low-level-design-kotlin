package designPatterns
// Coffee.kt

// Step 1: Define the Coffee interface
private interface Coffee {
    fun getDescription(): String
    fun getCost(): Double
}

// Step 2: Implement the base SimpleCoffee class
private class SimpleCoffee : Coffee {
    override fun getDescription() = "Simple coffee"
    override fun getCost() = 5.0
}

// Step 3: Implement decorators using delegation and private visibility
private class MilkDecorator(private val coffee: Coffee) : Coffee by coffee {
    override fun getDescription() = coffee.getDescription() + ", milk"
    override fun getCost() = coffee.getCost() + 1.5
}

private class SugarDecorator(private val coffee: Coffee) : Coffee by coffee {
    override fun getDescription() = coffee.getDescription() + ", sugar"
    override fun getCost() = coffee.getCost() + 0.5
}

private class WhippedCreamDecorator(private val coffee: Coffee) : Coffee by coffee {
    override fun getDescription() = coffee.getDescription() + ", whipped cream"
    override fun getCost() = coffee.getCost() + 2.0
}

// Step 4: Test function - keep it private to restrict access within this file
private fun main() {
    // Start with a simple coffee
    var coffee: Coffee = SimpleCoffee()
    println("${coffee.getDescription()} costs ${coffee.getCost()}")

    // Add milk
    coffee = MilkDecorator(coffee)
    println("${coffee.getDescription()} costs ${coffee.getCost()}")

    // Add sugar
    coffee = SugarDecorator(coffee)
    println("${coffee.getDescription()} costs ${coffee.getCost()}")

    // Add whipped cream
    coffee = WhippedCreamDecorator(coffee)
    println("${coffee.getDescription()} costs ${coffee.getCost()}")
}
