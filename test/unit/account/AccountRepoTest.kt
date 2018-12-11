package unit.account

import assertk.assertions.containsAll
import exceptions.AccountNotFoundException
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.Assert
import test.cleanupDatabase
import test.initializeDatabase
import test.stageAccount
import uk.stumme.account.AccountRepo
import uk.stumme.models.database.Account
import uk.stumme.models.database.Transfers
import java.util.*
import kotlin.test.*

class AccountRepoTest {
    private var repo: AccountRepo
    private var accountNumber1: String
    private var accountNumber2: String

    init {
        Database.connect("jdbc:h2:mem:test;DB_CLOSE_DELAY=-1", "org.h2.Driver")

        this.repo = AccountRepo()
        this.accountNumber1 = "GB00123456789012345678"
        this.accountNumber2 = "GB00876543210987654321"

        initializeDatabase()
    }

    @AfterTest
    fun Teardown() {
        cleanupDatabase()
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

    @Test
    fun testTransfer_ShouldAddAnAuditRecord() {
        stageAccount(accountNumber1, 50.00)
        stageAccount(accountNumber2, 0.00)

        val amount = 25.00
        val transferId = repo.transfer(accountNumber1, accountNumber2, amount)

        transaction {
            val transfer = Transfers.select { Transfers.id eq transferId }.singleOrNull()

            assertNotNull(transfer)
            assertEquals(accountNumber1, transfer[Transfers.sourceAccount])
            assertEquals(accountNumber2, transfer[Transfers.destinationAccount])
            Assert.assertEquals(amount, transfer[Transfers.amount].toDouble(), 0.01)
        }
    }

    @Test
    fun testGetTransfers_ShouldReturnAllTransfersForAnAccount() {
        var transferId1: UUID? = UUID(0L, 0L)
        var transferId2: UUID? = UUID(0L, 0L)
        transaction {
            transferId1 = Transfers.insert {
                it[Transfers.id] = UUID.randomUUID()
                it[Transfers.sourceAccount] = "source"
                it[Transfers.destinationAccount] = "destination"
                it[Transfers.amount] = 25.toBigDecimal()
            } get Transfers.id
            transferId2 = Transfers.insert {
                it[Transfers.id] = UUID.randomUUID()
                it[Transfers.sourceAccount] = "source"
                it[Transfers.destinationAccount] = "destination"
                it[Transfers.amount] = 25.toBigDecimal()
            } get Transfers.id
        }

        val transfers = repo.getTransfers("source")
            .map { it.id }

        assertk.assert(transfers).containsAll(transferId1, transferId2)
    }

    @Test
    fun testHasAccount_ShouldReturnTrueWhenAccountExists() {
        stageAccount(accountNumber1, 0.00)

        val actual = repo.hasAccount(accountNumber1)

        assertTrue(actual)
    }

    @Test
    fun testHasAccount_ShouldReturnFalseWhenAccountDoesNotExists() {
        val actual = repo.hasAccount(accountNumber1)

        assertFalse(actual)
    }
}