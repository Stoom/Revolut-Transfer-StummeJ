package uk.stumme.account

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import io.ktor.application.call
import io.ktor.http.HttpStatusCode
import io.ktor.request.receiveText
import io.ktor.response.*
import io.ktor.routing.*
import uk.stumme.models.NewAccountRequest
import uk.stumme.models.NewAccountResponse
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

inline fun <reified T> Gson.fromJson(json: String) = this.fromJson<T>(json, object: TypeToken<T>() {}.type)

fun Routing.accounts(
    controller: AccountController = Injekt.get()
) {
    val gson = Gson()

    route("/accounts") {
        post {
            val request = Gson().fromJson<NewAccountRequest>(call.receiveText())
            val accountNumber = controller.createAccount(request.countryCode, request.initialDeposit)

            call.respond(HttpStatusCode.OK, Gson().toJson(NewAccountResponse(accountNumber)))
        }

        route("{srcAccount}") {
            get {
                call.respond(HttpStatusCode.OK, "")
            }

            route("transfer") {
                post("{dstAccount}") {
                    call.respond(HttpStatusCode.OK, "")
                }

                get {
                    call.respond(HttpStatusCode.OK, "")
                }
            }
        }
    }
}