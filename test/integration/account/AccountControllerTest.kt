package integration.account

import exceptions.AccountNotFoundException
import exceptions.InvalidArgumentException
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.Test
import test.stageAccount
import uk.stumme.account.AccountController
import uk.stumme.account.AccountRepo
import uk.stumme.models.database.Account
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class AccountControllerTest {
    private val controller: AccountController

    init {
        Database.connect("jdbc:h2:mem:test;DB_CLOSE_DELAY=-1", "org.h2.Driver")

        val accountRepo = AccountRepo()
        controller = AccountController(accountRepo)

        transaction {
            SchemaUtils.create(Account)
        }
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
}