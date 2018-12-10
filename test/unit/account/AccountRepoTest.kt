package unit.account

import exceptions.AccountNotFoundException
import exceptions.InsufficientFunds
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.Assert
import test.stageAccount
import uk.stumme.account.AccountRepo
import uk.stumme.models.database.Account
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class AccountRepoTest {
    private var repo: AccountRepo
    private var accountNumber1: String
    private var accountNumber2: String

    init {
        Database.connect("jdbc:h2:mem:test;DB_CLOSE_DELAY=-1", "org.h2.Driver")

        this.repo = AccountRepo()
        this.accountNumber1 = "GB00123456789012345678"
        this.accountNumber2 = "GB00876543210987654321"

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
        repo.createAccount(accountNumber1, 0.00)

        transaction {
            val saved = Account.select { Account.id.eq(accountNumber1) }.singleOrNull()
            assertNotNull(saved)
        }
    }

    @Test
    fun testCreateAccount_ShouldOpenTheAccountWithTheSpecifiedAmount() {
        val expectedAmount = 500.98
        repo.createAccount(accountNumber1, expectedAmount)

        transaction {
            val saved = Account.select { Account.id.eq(accountNumber1) }.single()
            assertEquals(expectedAmount.toBigDecimal(), saved[Account.balance])
        }
    }

    @Test
    fun testGetBalance_ShouldReturnBalanceWhenAccountExists() {
        val expected = 315.12
        transaction {
            Account.insert {
                it[Account.id] = accountNumber1
                it[Account.balance] = expected.toBigDecimal()
            }
        }

        val actual = repo.getBalance(accountNumber1)

        assertEquals(expected, actual)
    }

    @Test(expected = AccountNotFoundException::class)
    fun testGetBalance_ShouldThrowExceptionWhenAccountDoesNotExist() {
        repo.getBalance("Some account that doesn't exist")
    }

    @Test
    fun testTransfer_ShouldMoveMoneyBetweenAccounts() {
        stageAccount(accountNumber1, 100.00)
        stageAccount(accountNumber2, 0.00)

        repo.transfer(accountNumber1, accountNumber2, 100.00)

        transaction {
            val actualAccount1 = Account.select{ Account.id.eq(accountNumber1) }.single()
            val actualAccount2 = Account.select{ Account.id.eq(accountNumber2) }.single()

            Assert.assertEquals(0.00, actualAccount1[Account.balance].toDouble(), 0.01)
            Assert.assertEquals(100.00, actualAccount2[Account.balance].toDouble(), 0.01)
        }
    }
}