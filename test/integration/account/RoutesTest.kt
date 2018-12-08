package integration.account

import com.google.gson.Gson
import io.ktor.application.Application
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.*
import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.deleteAll
import org.jetbrains.exposed.sql.select
import org.junit.BeforeClass
import uk.stumme.account.AccountController
import uk.stumme.account.AccountRepo
import uk.stumme.models.NewAccountRequest
import uk.stumme.models.NewAccountResponse
import uk.stumme.models.TransferRequest
import uk.stumme.models.database.Account
import uk.stumme.module
import uk.stumme.account.fromJson
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.addFactory
import uy.kohesive.injekt.api.addSingleton
import uy.kohesive.injekt.api.get
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals


class RoutesTest {
    companion object {
        val database: Database = Database.connect("jdbc:h2:mem:test;DB_CLOSE_DELAY=-1", "org.h2.Driver")

        @BeforeClass
        @JvmStatic
        fun classSetup() {
            Injekt.addSingleton(database)
            Injekt.addFactory { AccountRepo(Injekt.get()) }
            Injekt.addFactory { AccountController(Injekt.get()) }
        }

    }

    private val db: Database

    init {
        db = RoutesTest.database

        db.transaction {
            create(Account)
        }
    }

    @AfterTest
    fun cleanup() = db.transaction {
        Account.deleteAll()
    }

    @Test
    fun testPostAccount_ShouldReturn200AndReturnTheNewAccountNumber() {
        testRequest(
            HttpMethod.Post,
            "/accounts",
            setJsonBody(NewAccountRequest("GB"))
        ) {
            assertEquals(HttpStatusCode.OK, response.status())

            val body = Gson().fromJson<NewAccountResponse>(response.content!!)
            assertEquals("GB", body.accountNumber.substring(0 until 2))
            assertEquals("00", body.accountNumber.substring(2 until 4))
        }
    }

    @Test
    fun testPostAccount_ShouldReturn400WhenMissingCountryCode() {
        testRequest(
            HttpMethod.Post,
            "/accounts",
            setJsonBody(NewAccountRequest(""))
        ) {
            assertEquals(HttpStatusCode.BadRequest, response.status())
        }
    }

    @Test
    fun testPostAccount_ShouldAcceptInitialDeposit() {
        val request = NewAccountRequest("GB", 521.35)
        testRequest(
            HttpMethod.Post,
            "/accounts",
            setJsonBody(request)
        ) {
            assertEquals(HttpStatusCode.OK, response.status())

            val body = Gson().fromJson<NewAccountResponse>(response.content!!)

            db.transaction {
                val balance = Account.slice(Account.balance)
                    .select { Account.id.eq(body.accountNumber) }
                    .single()[Account.balance]

                assertEquals(request.initialDeposit.toBigDecimal(), balance)
            }
        }
    }

    @Test
    fun testPostTransferShouldReturn200() {
        testRequest(
            HttpMethod.Post,
            "/accounts/source/transfer/destination",
            setJsonBody(TransferRequest(100.00))
        ) {
            assertEquals(HttpStatusCode.OK, response.status())
        }
    }

    @Test
    fun testGetAccountShouldReturn200() {
        testRequest(HttpMethod.Get, "/accounts/source") {
            assertEquals(HttpStatusCode.OK, response.status())
        }
    }

    private fun testRequest(
        method: HttpMethod, uri: String,
        setup: suspend TestApplicationRequest.() -> Unit = {},
        check: suspend TestApplicationCall.() -> Unit
    ) {
        httpBinTest {
            val req = handleRequest(method, uri) { runBlocking { setup() } }
            check(req)
        }
    }

    private fun httpBinTest(callback: suspend TestApplicationEngine.() -> Unit) {
        withTestApplication(Application::module) {
            runBlocking { callback() }
        }
    }


    private fun setJsonBody(value: Any): suspend TestApplicationRequest.() -> Unit {
        return {
            setBody(Gson().toJson(value))
            addHeader("Content-Type", "application/json")
            addHeader("Accept", "application/json")
        }
    }
}