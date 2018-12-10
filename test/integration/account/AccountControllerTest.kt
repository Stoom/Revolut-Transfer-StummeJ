package integration.account

import assertk.assertions.contains
import assertk.assertions.containsAll
import assertk.assertions.containsExactly
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
import kotlin.test.*

class AccountControllerTest {
    private val controller: AccountController

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

        assertEquals(countryCode, accountNumber.substring(0 until 2))
        assertEquals("00", accountNumber.substring(2 until 4))
        assertEquals(22, accountNumber.length)
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
            val account = Account.select { Account.id.eq(accountNumber) }.singleOrNull()
            assertNotNull(account)
        }
    }

    @Test
    fun testGetAccount_ReturnsTheAccountWithTheCorrectBalance() {
        val accountNumber = "GB00123456789012345678"
        val expectedBalance = 24.50
        transaction {
            Account.insert {
                it[Account.id] = accountNumber
                it[Account.balance] = expectedBalance.toBigDecimal()
            }
        }

        val account = controller.getAccount(accountNumber)

        assertEquals(expectedBalance, account.balance)
    }

    @Test(expected = AccountNotFoundException::class)
    fun testGetAccount_ThrowsWhenAccountDoesNotExist() {
        controller.getAccount("THIS ACCOUNT DOES NOT EXIST")
    }

    @Test
    fun testTransfer_ReturnsTheTransactionUuid() {
        val regex = """\b[0-9a-f]{8}\b-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-\b[0-9a-f]{12}\b"""
            .toRegex(RegexOption.IGNORE_CASE)

        val accountNumber1 = "GB00123456789"
        val accountNumber2 = "GB00987654321"
        stageAccount(accountNumber1, 50.00)
        stageAccount(accountNumber2, 0.00)

        val transferId = controller.transfer(accountNumber1, accountNumber2, 25.00)

        assertTrue(regex matches transferId)
    }

    @Test(expected = InvalidArgumentException::class)
    fun testTransfer_ThrowsWhenMissingSourceAccount() {
        controller.transfer("", "foobar", 1.00)
    }

    @Test(expected = InvalidArgumentException::class)
    fun testTransfer_ThrowsWhenMissingDestinationAccount() {
        controller.transfer("foobar", "", 1.00)
    }

    @Test(expected = InvalidArgumentException::class)
    fun testTransfer_ThrowsWhenAmountIsZero() {
        controller.transfer("foobar", "fizzbuzz", 0.00)
    }

    @Test(expected = InvalidArgumentException::class)
    fun testTransfer_ThrowsWhenAmountIsLessThanZero() {
        controller.transfer("foobar", "fizzbuzz", -1.05)
    }

    @Test(expected = InsufficientFunds::class)
    fun testTransfer_ShouldThrowExceptionWhenTransferringMoreThanInSourceAccount() {
        val accountNumber1 = "GB00123456789"
        val accountNumber2 = "GB00987654321"
        stageAccount(accountNumber1, 50.00)
        stageAccount(accountNumber2, 0.00)

        controller.transfer(accountNumber1, accountNumber2, 100.00)
    }

    @Test
    fun testGetTransfers_ShouldReturnTransfers() {
        val accountNumber1 = "GB00123456789"
        val accountNumber2 = "GB00987654321"
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
        val accountNumber = "GB00123456789"
        stageAccount(accountNumber, 150.00)
        val transfers = controller.getTransfers(accountNumber)

        assertEquals(0, transfers.size)
    }

    @Test(expected = InvalidArgumentException::class)
    fun testGetTransfers_ShouldThrowExceptionWhenEmptyAccount() {
        controller.getTransfers("")
    }

    @Test(expected = AccountNotFoundException::class)
    fun testGetTransfers_ShouldThrowExceptionWhenAccountDoesNotExist() {
        controller.getTransfers("ACCOUNT THAT DOES NOT EXIST")
    }
}