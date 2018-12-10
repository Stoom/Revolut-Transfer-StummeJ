package uk.stumme

import io.ktor.application.*
import io.ktor.routing.*
import io.ktor.gson.*
import io.ktor.features.*
import io.ktor.http.HttpStatusCode
import io.ktor.response.respond
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import uk.stumme.account.AccountController
import uk.stumme.account.AccountRepo

import uk.stumme.account.accounts
import uk.stumme.models.database.Account
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.addFactory
import uy.kohesive.injekt.api.addSingleton
import uy.kohesive.injekt.api.get

fun main(args: Array<String>): Unit = io.ktor.server.netty.EngineMain.main(args)

fun Application.module() {
    install(ContentNegotiation) {
        gson {
        }
    }
    Injekt.addFactory { AccountRepo() }
    Injekt.addFactory { AccountController(Injekt.get()) }

    initDb()

    routing {
        accounts()
    }
}

private fun initDb() {
    Database.connect("jdbc:h2:mem:test;DB_CLOSE_DELAY=-1", "org.h2.Driver")

    transaction {
        SchemaUtils.create(Account)
    }
}
