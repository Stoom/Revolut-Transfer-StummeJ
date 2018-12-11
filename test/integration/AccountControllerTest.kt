package integration

import assertk.assertions.containsAll
import assertk.assertions.doesNotContain
import exceptions.AccountNotFoundException
import exceptions.InsufficientFunds
import exceptions.InvalidArgumentException
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import test.cleanupDatabase
import test.initializeDatabase
import test.stageAccount
import test.stageTransfer
import uk.stumme.account.AccountController
import uk.stumme.account.AccountRepo
import uk.stumme.models.database.Account
import uk.stumme.models.domain.Iban
import kotlin.test.*

class AccountControllerTest {
    private val controller: AccountController
    private val accountNumber1 = Iban("GB32123456789")
    private val accountNumber2 = Iban("GB81987654321")

    init {
        Database.connect("jdbc:h2:mem:test;DB_CLOSE_DELAY=-1", "org.h2.Driver")

        val accountRepo = AccountRepo()
        controller = AccountController(accountRepo)

        initializeDatabase()
    }

    @AfterTest
    fun Teardown() {
        cleanupDatabase()
    }

    @Test
    fun testCreateAccount_ShouldReturnTheNewAccountNumber() {
        val countryCode = "GB"

        val accountNumber = controller.createAccount(countryCode, 0.00)

        assertEquals(countryCode, accountNumber.countryCode)
        assertEquals(26, accountNumber.number.length)
    }

    @Test(expected = InvalidArgumentException::class)
    fun testCreateAccount_InitialBalanceMustBeGreaterThanZero() {
        controller.createAccount("GB", -1.00)
    }

    @Test(expected = InvalidArgumentException::class)
    fun testCreateAccount_ThrowsWhenCountryCodeIsBlank() {
        controller.createAccount("", 0.00)
    }

    @Test
    fun testCreateAccount_SavesAccount() {
        val accountNumber = controller.createAccount("GB", 0.00)

        transaction {
            val account = Account.select { Account.id eq "$accountNumber" }.singleOrNull()
            assertNotNull(account)
        }
    }

    @Test
    fun testGetAccount_ReturnsTheAccountWithTheCorrectBalance() {
        val expectedBalance = 24.50
        transaction {
            Account.insert {
                it[Account.id] = "$accountNumber1"
                it[Account.balance] = expectedBalance.toBigDecimal()
            }
        }

        val account = controller.getAccount(accountNumber1)

        assertEquals(expectedBalance, account.balance)
    }

    @Test(expected = AccountNotFoundException::class)
    fun testGetAccount_ThrowsWhenAccountDoesNotExist() {
        controller.getAccount(accountNumber1)
    }

    @Test
    fun testTransfer_ReturnsTheTransactionUuid() {
        val regex = """\b[0-9a-f]{8}\b-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-\b[0-9a-f]{12}\b"""
            .toRegex(RegexOption.IGNORE_CASE)

        stageAccount(accountNumber1, 50.00)
        stageAccount(accountNumber2, 0.00)

        val transferId = controller.transfer(accountNumber1, accountNumber2, 25.00)

        assertTrue(regex matches transferId)
    }

    @Test(expected = InvalidArgumentException::class)
    fun testTransfer_ThrowsWhenInvalidSourceAccount() {
        controller.transfer(Iban("GB990"), accountNumber2, 1.00)
    }

    @Test(expected = InvalidArgumentException::class)
    fun testTransfer_ThrowsWhenInvalidDestinationAccount() {
        controller.transfer(accountNumber1, Iban("GB990"), 1.00)
    }

    @Test(expected = InvalidArgumentException::class)
    fun testTransfer_ThrowsWhenAmountIsZero() {
        controller.transfer(accountNumber1, accountNumber2, 0.00)
    }

    @Test(expected = InvalidArgumentException::class)
    fun testTransfer_ThrowsWhenAmountIsLessThanZero() {
        controller.transfer(accountNumber1, accountNumber2, -1.05)
    }

    @Test(expected = InsufficientFunds::class)
    fun testTransfer_ShouldThrowExceptionWhenTransferringMoreThanInSourceAccount() {
        stageAccount(accountNumber1, 50.00)
        stageAccount(accountNumber2, 0.00)

        controller.transfer(accountNumber1, accountNumber2, 100.00)
    }

    @Test
    fun testGetTransfers_ShouldReturnTransfers() {
        stageAccount(accountNumber1, 150.00)
        val transfer1 = stageTransfer(accountNumber1, accountNumber2, 50.00)
        val transfer2 = stageTransfer(accountNumber1, accountNumber2, 100.00)
        val transfer3 = stageTransfer(accountNumber2, accountNumber1, 1.00)

        val transfers = controller.getTransfers(accountNumber1)
            .map { it.id }

        assertk.assert(transfers).containsAll(transfer1, transfer2)
        assertk.assert(transfers).doesNotContain(transfer3)
    }

    @Test
    fun testGetTransfers_ShouldReturnEmptyListWhenNoTransfers() {
        stageAccount(accountNumber1, 150.00)
        val transfers = controller.getTransfers(accountNumber1)

        assertEquals(0, transfers.size)
    }

    @Test(expected = InvalidArgumentException::class)
    fun testGetTransfers_ShouldThrowExceptionWhenEmptyAccount() {
        controller.getTransfers(Iban(""))
    }

    @Test(expected = AccountNotFoundException::class)
    fun testGetTransfers_ShouldThrowExceptionWhenAccountDoesNotExist() {
        controller.getTransfers(accountNumber1)
    }
}