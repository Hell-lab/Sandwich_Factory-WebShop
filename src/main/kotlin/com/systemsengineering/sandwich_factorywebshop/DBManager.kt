package com.systemsengineering.sandwich_factorywebshop

import java.sql.*
import java.time.LocalTime

class DBManager {
	private val verbose: Boolean = false

	private var dbConnection: Connection? = null
	fun openConnection() {
		check(dbConnection == null) { "Already connected to database" }
		try {
			dbConnection = DriverManager.getConnection(DB_URL)
			println("Opened connection.")
		} catch (e: SQLException) {
			throw handleError("Could not open connection.", e)
		}
	}

	fun closeConnection() {
		if (dbConnection == null) {
			println("Connection was already closed.")
		} else {
			dbConnection!!.close()
			println("Closed connection.")
		}
	}

	fun initializeTables() {
		println("Initializing new data base.")
		try {
			deleteTables()
		} catch (e: SQLException) {
			throw handleError("Could not delete table.", e)
		}
		try {
			createTables()
		} catch (e: SQLException) {
			throw handleError("Could not create table.", e)
		}
	}

	private fun deleteTables() {
		println("Deleting old tables.")
		dbConnection!!.createStatement().use { statement ->
			statement.execute("DROP TABLE $ORDER_PRODUCTS_TABLE_NAME")
			statement.execute("DROP TABLE $ORDERS_TABLE_NAME")
			statement.execute("DROP TABLE $CUSTOMERS_TABLE_NAME")
			statement.execute("DROP TABLE $PAYMENTS_TABLE_NAME")
			statement.execute("DROP TABLE $DELIVERIES_TABLE_NAME")
			statement.execute("DROP TABLE $PRODUCTS_TABLE_NAME")
		}
	}

	private fun createTables() {
		println("Creating new tables.")

		println(" - Deliveries")
		dbConnection!!.createStatement().use { statement ->
			statement.execute(
				"CREATE TABLE $DELIVERIES_TABLE_NAME ("
						+ "$DELIVERY_ID INT PRIMARY KEY GENERATED ALWAYS AS IDENTITY (START WITH 1, INCREMENT BY 1), "
						+ "$DELIVERY_ADDRESS VARCHAR(255), "
						+ "$DELIVERY_TYPE VARCHAR(255), "
						+ "$DELIVERY_DESCRIPTION VARCHAR(255))"

			)
		}

		println(" - Payments")
		dbConnection!!.createStatement().use { statement ->
			statement.execute(
				"CREATE TABLE $PAYMENTS_TABLE_NAME ("
						+ "$PAYMENT_ID INT PRIMARY KEY GENERATED ALWAYS AS IDENTITY (START WITH 1, INCREMENT BY 1), "
						+ "$PAYMENT_METHOD VARCHAR(255)) "
			)
		}

		println(" - Customers")
		dbConnection!!.createStatement().use { statement ->
			statement.execute(
				"CREATE TABLE $CUSTOMERS_TABLE_NAME ("
						+ "$CUSTOMER_ID INT PRIMARY KEY GENERATED ALWAYS AS IDENTITY (START WITH 1, INCREMENT BY 1), "
						+ "$CUSTOMER_FIRST_NAME VARCHAR(255), "
						+ "$CUSTOMER_LAST_NAME VARCHAR(255), "
						+ "$CUSTOMER_ADDRESS VARCHAR(255), "
						+ "$CUSTOMER_MAIL_ADDRESS VARCHAR(255),"
						+ "$CUSTOMER_PHONE_NUMBER VARCHAR(255))"
			)
		}

		println(" - Orders")
		dbConnection!!.createStatement().use { statement ->
			statement.execute(
				"CREATE TABLE $ORDERS_TABLE_NAME ("
						+ "$ORDER_ID INT PRIMARY KEY GENERATED ALWAYS AS IDENTITY (START WITH 1, INCREMENT BY 1), "
						+ "$ORDER_STATUS VARCHAR(255), "
						+ "$ORDER_ESTIMATED_SHIPPING_TIME TIME,"
						+ "$CUSTOMER_ID INT, "
						+ "$DELIVERY_ID INT, "
						+ "$PAYMENT_ID INT, "
						+ "FOREIGN KEY ($CUSTOMER_ID) REFERENCES $CUSTOMERS_TABLE_NAME($CUSTOMER_ID), "
						+ "FOREIGN KEY ($DELIVERY_ID) REFERENCES $DELIVERIES_TABLE_NAME($DELIVERY_ID), "
						+ "FOREIGN KEY ($PAYMENT_ID) REFERENCES $PAYMENTS_TABLE_NAME($PAYMENT_ID)) "

			)
		}

		println(" - Products")
		dbConnection!!.createStatement().use { statement ->
			statement.execute(
				"CREATE TABLE $PRODUCTS_TABLE_NAME ("
						+ "$PRODUCT_ID INT PRIMARY KEY GENERATED ALWAYS AS IDENTITY (START WITH 1, INCREMENT BY 1), "
						+ "$PRODUCT_NAME VARCHAR(255), "
						+ "$PRODUCT_WEIGHT INT) "
			)
		}

		println(" - Order products")
		dbConnection!!.createStatement().use { statement ->
			statement.execute(
				"CREATE TABLE $ORDER_PRODUCTS_TABLE_NAME ("
						+ "$ORDER_ID INT, "
						+ "$PRODUCT_ID INT, "
						+ "FOREIGN KEY ($PRODUCT_ID) REFERENCES $PRODUCTS_TABLE_NAME($PRODUCT_ID), "
						+ "FOREIGN KEY ($ORDER_ID) REFERENCES $ORDERS_TABLE_NAME($ORDER_ID))"
			)
		}
	}

	fun populateProductsWithDummyData() {
		println("Populating Products with dummy data.")
		Product.getProducts().forEach { addProduct(it) }
	}


	private fun addProduct(product: Product) {
		println(" - Adding product \"${product.name}\" to database.")
		try {
			val insertStatement: PreparedStatement = dbConnection!!.prepareStatement(
				"INSERT INTO $PRODUCTS_TABLE_NAME ($PRODUCT_NAME, $PRODUCT_WEIGHT) VALUES(?,?)",
				Statement.RETURN_GENERATED_KEYS
			)
			insertStatement.setString(1, product.name)
			insertStatement.setInt(2, product.weight)

			val affectedRows = insertStatement.executeUpdate()
			if (affectedRows != 1) {
				throw handleError("Failed to add product ${product.name} to database.")
			}

			val generatedKeys: ResultSet = insertStatement.generatedKeys
			generatedKeys.next()
			product.id = generatedKeys.getInt(1)
		} catch (e: SQLException) {
			throw handleError("Failed to add product ${product.name} to database.", e)
		}
	}

	fun addOrder(order: Order): Order {
		if (verbose) println(" - Adding order to database.")
		try {
			val insertStatement: PreparedStatement = dbConnection!!.prepareStatement(
				"INSERT INTO $ORDERS_TABLE_NAME ( $ORDER_STATUS, $ORDER_ESTIMATED_SHIPPING_TIME, $CUSTOMER_ID, $DELIVERY_ID, $PAYMENT_ID) VALUES(?,?,?,?,?)",
				Statement.RETURN_GENERATED_KEYS
			)
			insertStatement.setString(1, order.status.name)
			insertStatement.setTime(2, Time.valueOf(LocalTime.now()))
			insertStatement.setInt(3, order.customer.id)
			insertStatement.setInt(4, order.delivery.id)
			insertStatement.setInt(5, order.payment.id)

			val affectedRows = insertStatement.executeUpdate()
			if (affectedRows != 1) {
				throw handleError("Failed to add order to database.")
			}

			// return created order ID (primary key)
			val generatedKeys: ResultSet = insertStatement.generatedKeys
			generatedKeys.next()
			order.id = generatedKeys.getInt(1)

			order.products.forEach { addOrderItem(order.id, it) }
		} catch (e: SQLException) {
			throw handleError("Failed to add order to database.", e)
		}
		if (verbose) println(" - com.systemsengineering.webshop.Order has ID ${order.id}.")
		return order
	}

	fun addCustomer(customer: Customer) {
		if (verbose) println(" - Adding customer to database.")
		try {
			val insertStatement: PreparedStatement = dbConnection!!.prepareStatement(
				"INSERT INTO $CUSTOMERS_TABLE_NAME($CUSTOMER_FIRST_NAME, $CUSTOMER_LAST_NAME, $CUSTOMER_ADDRESS, $CUSTOMER_MAIL_ADDRESS, $CUSTOMER_PHONE_NUMBER) VALUES(?,?,?,?,?)",
				Statement.RETURN_GENERATED_KEYS
			)
			insertStatement.setString(1, customer.firstName)
			insertStatement.setString(2, customer.lastName)
			insertStatement.setString(3, customer.address)
			insertStatement.setString(4, customer.mailAddress)
			insertStatement.setString(5, customer.phoneNumber)

			val affectedRows = insertStatement.executeUpdate()
			if (affectedRows != 1) {
				throw handleError("Failed to add customer to database.")
			}

			// return created customer ID (primary key)
			val generatedKeys: ResultSet = insertStatement.generatedKeys
			generatedKeys.next()
			customer.id = generatedKeys.getInt(1)
		} catch (e: SQLException) {
			throw handleError("Failed to add customer to database.", e)
		}
		if (verbose) println(" - com.systemsengineering.webshop.Customer has ID ${customer.id}")
	}

	fun addPayment(payment: Payment) {
		if (verbose) println(" - Adding payment to database.")
		try {
			val insertStatement: PreparedStatement = dbConnection!!.prepareStatement(
				"INSERT INTO $PAYMENTS_TABLE_NAME($PAYMENT_METHOD) VALUES(?)",
				Statement.RETURN_GENERATED_KEYS
			)
			insertStatement.setString(1, payment.paymentMethod.name)

			val affectedRows = insertStatement.executeUpdate()
			if (affectedRows != 1) {
				throw handleError("Failed to add payment to database.")
			}

			// return created payment ID (primary key)
			val generatedKeys: ResultSet = insertStatement.generatedKeys
			generatedKeys.next()
			payment.id = generatedKeys.getInt(1)
		} catch (e: SQLException) {
			throw handleError(" - Failed to add payment to database.")
		}
		if (verbose) println(" - com.systemsengineering.webshop.Payment has ID ${payment.id}")
	}

	fun addDelivery(delivery: Delivery) {
		if (verbose) println(" - Adding delivery to database.")
		try {
			val insertStatement: PreparedStatement = dbConnection!!.prepareStatement(
				"INSERT INTO $DELIVERIES_TABLE_NAME($DELIVERY_ADDRESS, $DELIVERY_TYPE, $DELIVERY_DESCRIPTION) VALUES(?,?,?)",
				Statement.RETURN_GENERATED_KEYS
			)
			insertStatement.setString(1, delivery.address)
			insertStatement.setString(2, delivery.type.name)
			insertStatement.setString(3, delivery.description)

			val affectedRows = insertStatement.executeUpdate()
			if (affectedRows != 1) {
				throw handleError("Failed to add delivery to database.")
			}

			// return created delivery ID (primary key)
			val generatedKeys: ResultSet = insertStatement.generatedKeys
			generatedKeys.next()
			delivery.id = generatedKeys.getInt(1)
		} catch (e: SQLException) {
			throw handleError(" - Failed to add payment to database.")
		}
		if (verbose) println(" - com.systemsengineering.webshop.Payment has ID ${delivery.id}")
	}

	private fun addOrderItem(orderID: Int, product: Product) {
		if (verbose) println(" -- Adding product \"${product.name}\" to order $orderID.")
		try {
			val insertStatement: PreparedStatement = dbConnection!!.prepareStatement(
				"INSERT INTO $ORDER_PRODUCTS_TABLE_NAME ( $ORDER_ID, $PRODUCT_ID ) VALUES(?,?)"
			)

			insertStatement.setInt(1, orderID)
			insertStatement.setInt(2, product.id)

			val affectedRows = insertStatement.executeUpdate()
			if (affectedRows != 1) {
				throw handleError("Failed to add order_item to database.")
			}

		} catch (e: SQLException) {
			throw handleError("Failed to add order_item to database.", e)
		}
	}

	fun updateOrderStatus(orderID: Int, orderStatus: OrderStatus) {
		try {
			val updateStatement: PreparedStatement = dbConnection!!.prepareStatement(
				"UPDATE $ORDERS_TABLE_NAME SET $ORDER_STATUS = ? WHERE $ORDER_ID = ?"
			)
			updateStatement.setString(1, orderStatus.name)
			updateStatement.setInt(2, orderID)
			updateStatement.executeUpdate()
		} catch (e: SQLException) {
			throw handleError("com.systemsengineering.webshop.Order status could not be updated.", e)
		}
	}

	fun updateOrderETA(orderID: Int, time: LocalTime) {
		try {
			val updateStatement: PreparedStatement = dbConnection!!.prepareStatement(
				"UPDATE $ORDERS_TABLE_NAME SET $ORDER_ESTIMATED_SHIPPING_TIME = ? WHERE $ORDER_ID = ?"
			)
			updateStatement.setTime(1, Time.valueOf(time))
			updateStatement.setInt(2, orderID)
			updateStatement.executeUpdate()
		} catch (e: SQLException) {
			throw handleError("com.systemsengineering.webshop.Order status could not be updated.", e)
		}
	}

	fun startTransaction() {
		dbConnection!!.autoCommit = false
	}

	fun commitTransaction() {
		dbConnection!!.commit()
		dbConnection!!.autoCommit = true
	}

	fun rollbackTransaction() {
		dbConnection!!.rollback()
		dbConnection!!.autoCommit = true
	}

	/**
	 * Makes sure that a connection is closed if a fatal error occurs.
	 */
	private fun handleError(message: String, cause: Exception? = null): RuntimeException {
		try {
			closeConnection()
		} catch (e: SQLException) {
			throw RuntimeException("Could not close connection.", e)
		}
		return if (cause != null) RuntimeException(cause) else RuntimeException(message)
	}

	companion object {
		private const val DB_URL = "jdbc:derby:demoDB;create=true"

		// table names and their columns
		private const val ORDERS_TABLE_NAME = "ORDERS"
		private const val ORDER_ID = "ORDER_ID"
		private const val ORDER_STATUS = "STATUS"
		private const val ORDER_ESTIMATED_SHIPPING_TIME = "ESTIMATED_SHIPPING_TIME"

		private const val CUSTOMERS_TABLE_NAME = "CUSTOMERS"
		private const val CUSTOMER_ID = "CUSTOMER_ID"
		private const val CUSTOMER_FIRST_NAME = "FIRST_NAME"
		private const val CUSTOMER_LAST_NAME = "LAST_NAME"
		private const val CUSTOMER_ADDRESS = "ADDRESS"
		private const val CUSTOMER_MAIL_ADDRESS = "MAIL_ADDRESS"
		private const val CUSTOMER_PHONE_NUMBER = "PHONE_NUMBER"

		private const val PAYMENTS_TABLE_NAME = "PAYMENTS"
		private const val PAYMENT_ID = "PAYMENT_ID"
		private const val PAYMENT_METHOD = "PAYMENT_METHOD"

		private const val DELIVERIES_TABLE_NAME = "DELIVERIES"
		private const val DELIVERY_ID = "DELIVERY_ID"
		private const val DELIVERY_ADDRESS = "DELIVERY_ADDRESS"
		private const val DELIVERY_TYPE = "DELIVERY_TYPE"
		private const val DELIVERY_DESCRIPTION = "DELIVERY_DESCRIPTION"

		private const val PRODUCTS_TABLE_NAME = "PRODUCTS"
		private const val PRODUCT_ID = "PRODUCT_ID"
		private const val PRODUCT_NAME = "PRODUCT_NAME"
		private const val PRODUCT_WEIGHT = "PRODUCT_WEIGHT"

		private const val ORDER_PRODUCTS_TABLE_NAME = "ORDER_PRODUCTS"


	}
}
