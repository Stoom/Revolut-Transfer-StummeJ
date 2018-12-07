package uk.stumme

import io.ktor.application.*
import io.ktor.routing.*
import io.ktor.gson.*
import io.ktor.features.*

import uk.stumme.account.accounts

fun main(args: Array<String>): Unit = io.ktor.server.netty.EngineMain.main(args)

fun Application.module() {
    install(ContentNegotiation) {
        gson {
        }
    }

    routing {
        accounts()
    }
}
