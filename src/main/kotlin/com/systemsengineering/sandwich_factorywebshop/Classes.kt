package com.systemsengineering.sandwich_factorywebshop

data class Order(
	var id: Int = -1,
	val products: List<Product>,
	var status: OrderStatus = OrderStatus.CREATING,
	val customer: Customer,
	val delivery: Delivery,
	var payment: Payment
)

class Payment(
	var id: Int = -1,
	val paymentMethod: PaymentMethod
)

class Delivery(
	var id: Int = -1,
	val address: String,
	val type: DeliveryType,
	val description: String)

class Customer(
	var id: Int = -1,
	val firstName: String,
	val lastName: String,
	val address: String,
	val mailAddress: String,
	val phoneNumber: String
)

enum class DeliveryType {
	WORMHOLE, DRONE, CAR
}

enum class PaymentMethod {
	PAYPAL, CREDITCARD, BANKACCOUNT
}

enum class OrderStatus {
	CREATING, PAID, WAITING, SCHEDULED, PREPARING, SHIPPING, DELIVERED, NOT_DELIVERABLE
}

data class Product(var id: Int = -1, val name: String, val weight: Int) {

	companion object {
		private val cheeseSandwich = Product(
			-1,
			"Cheese Sandwich",
			250
		)
		private val hamSandwich = Product(
			-1,
			"Ham Sandwich",
			300
		)
		private val potatoSandwich = Product(
			-1,
			"Potato Sandwich",
			250
		)
		private val veggieSandwich = Product(
			-1,
			"Veggie Sandwich",
			150
		)
		private val clubSandwich = Product(
			-1,
			"Club Sandwich",
			400
		)


		fun getProducts(): List<Product> {
			return listOf(
				cheeseSandwich,
				hamSandwich,
				potatoSandwich,
				veggieSandwich,
				clubSandwich
			)
		}
	}
}
