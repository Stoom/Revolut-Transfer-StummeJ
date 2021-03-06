package e2e

import assertk.assertions.containsAll
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
import test.stageTransfer
import uk.stumme.account.AccountController
import uk.stumme.account.AccountRepo
import uk.stumme.account.fromJson
import uk.stumme.models.*
import uk.stumme.models.database.Account
import uk.stumme.models.domain.Iban
import uk.stumme.models.domain.Transfer
import uk.stumme.module
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.addFactory
import uy.kohesive.injekt.api.get
import java.util.*
import kotlin.test.*
import uk.stumme.models.domain.Account as DomainAccount


class RoutesTest {
    companion object {
        @BeforeClass
        @JvmStatic
        fun classSetup() {
            Injekt.addFactory { AccountRepo() }
            Injekt.addFactory { AccountController(Injekt.get()) }
        }
    }

    private val accountNumber1 = Iban("GB32123456789")
    private val accountNumber2 = Iban("GB81987654321")

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
            assertEquals(26, body.accountNumber.substring(4).length)
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
        testRequest(HttpMethod.Get, "/accounts/$accountNumber1") {
            assertEquals(HttpStatusCode.NotFound, response.status())
        }
    }

    @Test
    fun testGetAccountShouldReturnAccount() {
        val expectedAccount = GetAccountResponse("$accountNumber1", 50.00)
        stageAccount(expectedAccount.accountNumber, expectedAccount.balance)

        testRequest(HttpMethod.Get, "/accounts/${expectedAccount.accountNumber}") {
            val account = Gson().fromJson<GetAccountResponse>(response.content!!)

            assertEquals(expectedAccount.accountNumber, account.accountNumber)
            assertEquals(expectedAccount.balance, account.balance)
        }
    }

    @Test
    fun testPostTransferShouldReturn200() {
        val regex = """\b[0-9a-f]{8}\b-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-\b[0-9a-f]{12}\b"""
            .toRegex(RegexOption.IGNORE_CASE)

        stageAccount(accountNumber1, 100.00)
        stageAccount(accountNumber2, 0.00)

        testRequest(
            HttpMethod.Post,
            "/accounts/${accountNumber1}/transfer/${accountNumber2}",
            setJsonBody(PostTransferRequest(100.00))
        ) {
            val transfer = Gson().fromJson<PostTransferResponse>(response.content!!)

            assertEquals(HttpStatusCode.OK, response.status())
            assertTrue(regex matches transfer.transferId)
        }
    }

    @Test
    fun testPostTransferShouldReturn400WhenInsufficientFunds() {
        stageAccount(accountNumber1, 25.00)
        stageAccount(accountNumber2, 0.00)

        testRequest(
            HttpMethod.Post,
            "/accounts/${accountNumber1}/transfer/${accountNumber2}",
            setJsonBody(PostTransferRequest(50.00))
        ) {
            assertEquals(HttpStatusCode.BadRequest, response.status())
            assertEquals("Insufficient Funds", response.content)
        }
    }

    @Test
    fun testPostTransferShouldReturn404WhenSourceAccountDoesNotExist() {
        stageAccount(accountNumber2, 0.00)

        testRequest(
            HttpMethod.Post,
            "/accounts/${accountNumber1}/transfer/${accountNumber2}",
            setJsonBody(PostTransferRequest(50.00))
        ) {
            assertEquals(HttpStatusCode.NotFound, response.status())
            assertEquals("Source account does not exist", response.content)
        }
    }

    @Test
    fun testPostTransferShouldReturn404WhenDestinationAccountDoesNotExist() {
        stageAccount(accountNumber1, 50.00)

        testRequest(
            HttpMethod.Post,
            "/accounts/${accountNumber1}/transfer/${accountNumber2}",
            setJsonBody(PostTransferRequest(50.00))
        ) {
            assertEquals(HttpStatusCode.NotFound, response.status())
            assertEquals("Destination account does not exist", response.content)
        }
    }

    @Test
    fun testGetTransferShouldReturn200() {
        stageAccount(accountNumber1, 0.00)
        stageTransfer(accountNumber1, accountNumber2, 5.00)
        stageTransfer(accountNumber1, accountNumber2, 10.00)

        testRequest(
            HttpMethod.Get,
            "/accounts/${accountNumber1}/transfer"
        ) {
            val transfers = Gson()
                .fromJson<GetTransfersResponse>(response.content!!)
                .transfers
                .map { Transfer(UUID(0L, 0L), it.source, it.destination, it.amount) }

            assertk.assert(transfers).containsAll(
                Transfer(UUID(0L, 0L), "$accountNumber1", "$accountNumber2", 5.00),
                Transfer(UUID(0L, 0L), "$accountNumber1", "$accountNumber2", 10.00)
            )
        }
    }

    @Test
    fun testGetTransferShouldReturn404WhenAccountDoesNotExist() {
        testRequest(
            HttpMethod.Get,
            "/accounts/${accountNumber1}/transfer"
        ) {
            assertEquals(HttpStatusCode.NotFound, response.status())
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