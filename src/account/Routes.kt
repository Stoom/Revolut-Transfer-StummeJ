package uk.stumme.account

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import exceptions.AccountNotFoundException
import exceptions.InsufficientFunds
import exceptions.InvalidArgumentException
import io.ktor.application.call
import io.ktor.http.HttpStatusCode
import io.ktor.request.receiveText
import io.ktor.response.*
import io.ktor.routing.*
import uk.stumme.models.*
import uk.stumme.models.domain.Iban
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

inline fun <reified T> Gson.fromJson(json: String) = this.fromJson<T>(json, object: TypeToken<T>() {}.type)

fun Routing.accounts(
    controller: AccountController = Injekt.get()
) {
    route("/accounts") {
        post {
            try {
                val request = Gson().fromJson<NewAccountRequest>(call.receiveText())
                val accountNumber = controller.createAccount(request.countryCode, request.initialDeposit)

                call.respond(HttpStatusCode.OK, Gson().toJson(NewAccountResponse(accountNumber.toString())))
            } catch (e: InvalidArgumentException) {
                call.respond(HttpStatusCode.BadRequest)
            }
        }

        route("{srcAccount}") {
            get {
                try {
                    val srcAccount = Iban(call.parameters["srcAccount"]!!)

                    val account = controller.getAccount(srcAccount)
                    val resp = Gson().toJson(GetAccountResponse(account.accountNumber.toString(), account.balance))

                    call.respond(HttpStatusCode.OK, resp)
                } catch (e: AccountNotFoundException) {
                    call.respond(HttpStatusCode.NotFound)
                }
            }

            route("transfer") {
                post("{dstAccount}") {
                    try {
                        val request = Gson().fromJson<PostTransferRequest>(call.receiveText())
                        val srcAccount = Iban(call.parameters["srcAccount"]!!)
                        val dstAccount = Iban(call.parameters["dstAccount"]!!)

                        val transferId = controller.transfer(srcAccount, dstAccount, request.amount)
                        call.respond(HttpStatusCode.OK, Gson().toJson(PostTransferResponse(transferId)))
                    } catch (e: InsufficientFunds) {
                        call.respond(HttpStatusCode.BadRequest, "Insufficient Funds")
                    } catch (e: AccountNotFoundException) {
                        call.respond(HttpStatusCode.NotFound, e.message ?: "")
                    }
                }

                get {
                    try {
                        val srcAccount = Iban(call.parameters["srcAccount"]!!)

                        val transfers = controller.getTransfers(srcAccount)
                        call.respond(HttpStatusCode.OK, Gson().toJson(GetTransfersResponse(transfers)))
                    } catch (e: AccountNotFoundException) {
                        call.respond(HttpStatusCode.NotFound)
                    }
                }
            }
        }
    }
}