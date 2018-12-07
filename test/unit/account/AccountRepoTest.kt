package unit.account

import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.deleteAll
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
import exceptions.AccountNotFoundException
import uk.stumme.account.AccountRepo
import uk.stumme.models.database.Account
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class AccountRepoTest {
    private var repo: AccountRepo
    private var db: Database
    private var accountNumber: String

    init {
        this.db = Database.connect("jdbc:h2:mem:test;DB_CLOSE_DELAY=-1", "org.h2.Driver")
        this.repo = AccountRepo(this.db)
        this.accountNumber = "GB00123456789012345678"

        db.transaction {
            create(Account)
        }
    }

    @AfterTest
    fun Teardown() {
        db.transaction {
            Account.deleteAll();
        }
    }

    @Test
    fun testCreateAccount_ShouldPersistTheAccount() {
        val accountNumber = repo.createAccount(this.accountNumber, 0.00)

        db.transaction {
            val saved = Account.select { Account.id.eq(accountNumber) }.singleOrNull()
            assertNotNull(saved)
        }
    }

    @Test
    fun testCreateAccount_ShouldOpenTheAccountWithTheSpecifiedAmount() {
        val expectedAmount = 500.98
        val accountNumber = repo.createAccount(this.accountNumber, expectedAmount)

        db.transaction {
            val saved = Account.select { Account.id.eq(accountNumber) }.single()
            assertEquals(expectedAmount.toBigDecimal(), saved[Account.balance])
        }
    }

    @Test
    fun testGetBalance_ShouldReturnBalanceWhenAccountExists() {
        val accountNumber = this.accountNumber
        val expected = 315.12
        db.transaction {
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