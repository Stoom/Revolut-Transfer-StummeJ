package unit.account

import exceptions.AccountNotFoundException
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import test.stageAccount
import uk.stumme.account.AccountRepo
import uk.stumme.models.database.Account
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class AccountRepoTest {
    private var repo: AccountRepo
    private var accountNumber: String

    init {
        Database.connect("jdbc:h2:mem:test;DB_CLOSE_DELAY=-1", "org.h2.Driver")

        this.repo = AccountRepo()
        this.accountNumber = "GB00123456789012345678"

        transaction {
            SchemaUtils.create(Account)
        }
    }

    @AfterTest
    fun Teardown() {
        transaction {
            Account.deleteAll()
        }
    }

    @Test
    fun testCreateAccount_ShouldPersistTheAccount() {
        repo.createAccount(accountNumber, 0.00)

        transaction {
            val saved = Account.select { Account.id.eq(accountNumber) }.singleOrNull()
            assertNotNull(saved)
        }
    }

    @Test
    fun testCreateAccount_ShouldOpenTheAccountWithTheSpecifiedAmount() {
        val expectedAmount = 500.98
        repo.createAccount(accountNumber, expectedAmount)

        transaction {
            val saved = Account.select { Account.id.eq(accountNumber) }.single()
            assertEquals(expectedAmount.toBigDecimal(), saved[Account.balance])
        }
    }

    @Test
    fun testGetBalance_ShouldReturnBalanceWhenAccountExists() {
        val expected = 315.12
        transaction {
            Account.insert {
                it[Account.id] = accountNumber
                it[Account.balance] = expected.toBigDecimal()
            }
        }

        val actual = repo.getBalance(accountNumber)

        assertEquals(expected, actual)
    }

    @Test(expected = AccountNotFoundException::class)
    fun testGetBalance_ShouldThrowExceptionWhenAccountDoesNotExist() {
        repo.getBalance("Some account that doesn't exist")
    }
}