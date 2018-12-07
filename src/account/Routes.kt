package uk.stumme.account

import io.ktor.application.call
import io.ktor.http.HttpStatusCode
import io.ktor.response.*
import io.ktor.routing.*

fun Routing.accounts() {
    route("/accounts") {
        route("{srcAccount}") {
            route("transfer") {
                post("{dstAccount}") {
                    call.respond(HttpStatusCode.OK, "")
                }

                get {
                    call.respond(HttpStatusCode.OK, "")
                }
            }

            get {
                call.respond(HttpStatusCode.OK, "")
            }
        }
    }


}