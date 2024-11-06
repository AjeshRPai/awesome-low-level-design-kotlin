
// CoffeeShop.kt

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

// Step 3: Create a base CoffeeDecorator class that implements Coffee and delegates functionality
private abstract class CoffeeDecorator(private val decoratedCoffee: Coffee) : Coffee {
    override fun getDescription(): String = decoratedCoffee.getDescription()
    override fun getCost(): Double = decoratedCoffee.getCost()
}

// Step 4: Implement specific decorators
private class MilkDecorator(coffee: Coffee) : CoffeeDecorator(coffee) {
    override fun getDescription() = "${super.getDescription()}, milk"
    override fun getCost() = super.getCost() + 1.5
}

private class SugarDecorator(coffee: Coffee) : CoffeeDecorator(coffee) {
    override fun getDescription() = "${super.getDescription()}, sugar"
    override fun getCost() = super.getCost() + 0.5
}

private class WhippedCreamDecorator(coffee: Coffee) : CoffeeDecorator(coffee) {
    override fun getDescription() = "${super.getDescription()}, whipped cream"
    override fun getCost() = super.getCost() + 2.0
}

// Step 5: Test the decorator pattern
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
