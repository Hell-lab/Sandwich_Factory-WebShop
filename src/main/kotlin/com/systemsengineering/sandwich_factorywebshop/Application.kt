package com.systemsengineering.sandwich_factorywebshop

import javafx.application.Application
import javafx.fxml.FXMLLoader
import javafx.scene.Scene
import javafx.stage.Stage
import java.sql.*

class Application : Application() {
	override fun start(stage: Stage) {
		val fxmlLoader = FXMLLoader(Application::class.java.getResource("com/systemsengineering/webshop/view.fxml"))
		val scene = Scene(fxmlLoader.load(), 320.0, 240.0)
		stage.title = "WebShop - Sandwich Factory"
		stage.scene = scene
		stage.show()
	}
}

fun main() {
	val db = DBManager()
	db.openConnection()
	try {
		db.initializeTables()
		db.populateProductsWithDummyData()
		Application.launch(Application::class.java)
	} catch (e: SQLException) {
		e.printStackTrace()
	} finally {
		db.closeConnection()
	}
}
