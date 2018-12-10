package integration.account

import com.google.gson.Gson
import io.ktor.application.Application
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.*
import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.deleteAll
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.BeforeClass
import test.stageAccount
import uk.stumme.account.AccountController
import uk.stumme.account.AccountRepo
import uk.stumme.models.NewAccountRequest
import uk.stumme.models.NewAccountResponse
import uk.stumme.models.TransferRequest
import uk.stumme.models.database.Account
import uk.stumme.models.domain.Account as DomainAccount
import uk.stumme.module
import uk.stumme.account.fromJson
import uk.stumme.models.GetAccountResponse
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.addFactory
import uy.kohesive.injekt.api.addSingleton
import uy.kohesive.injekt.api.get
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals


class RoutesTest {
    companion object {
        @BeforeClass
        @JvmStatic
        fun classSetup() {
            Injekt.addFactory { AccountRepo() }
            Injekt.addFactory { AccountController(Injekt.get()) }
        }

    }

    init {
        Database.connect("jdbc:h2:mem:test;DB_CLOSE_DELAY=-1", "org.h2.Driver")

        transaction {
            SchemaUtils.create(Account)
        }
    }

    @AfterTest
    fun cleanup() {
        transaction {
            Account.deleteAll()
        }
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

            transaction {
                val balance = Account.slice(Account.balance)
                    .select { Account.id.eq(body.accountNumber) }
                    .single()[Account.balance]

                assertEquals(request.initialDeposit.toBigDecimal(), balance)
            }
        }
    }

    @Test
    fun testGetAccountShouldReturn404WhenAccountIsNotFound() {
        testRequest(HttpMethod.Get, "/accounts/FakeAccount") {
            assertEquals(HttpStatusCode.NotFound, response.status())
        }
    }

    @Test
    fun testGetAccountShouldReturnAccount() {
        val expectedAccount = GetAccountResponse("GB00123456789012345678", 50.00)
        stageAccount(expectedAccount.accountNumber, expectedAccount.balance)

        testRequest(HttpMethod.Get, "/accounts/${expectedAccount.accountNumber}") {
            val account = Gson().fromJson<GetAccountResponse>(response.content!!)

            assertEquals(expectedAccount.accountNumber, account.accountNumber)
            assertEquals(expectedAccount.balance, account.balance)
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