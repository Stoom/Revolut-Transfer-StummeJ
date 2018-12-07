package account

import org.jetbrains.exposed.sql.*
import uk.stumme.account.AccountRepo
import uk.stumme.models.database.Account
import kotlin.math.exp
import kotlin.test.*

class AccountRepoTest {
    private var repo: AccountRepo
    private var db: Database

    init {
        this.db = Database.connect("jdbc:h2:mem:test;DB_CLOSE_DELAY=-1", "org.h2.Driver")
        this.repo = AccountRepo(this.db)

        db.transaction {
            create(Account)
        }
    }

    @Test
    fun testCreateAccount_ShouldReturnTheNewAccountNumber() {
        val countryCode = "GB"

        val accountNumber = repo.createAccount(countryCode, 0.00)

        assertEquals(countryCode, accountNumber.substring(0 until 2))
        assertEquals("00", accountNumber.substring(2 until 4))
        assertEquals(22, accountNumber.length)
    }

    @Test
    fun testCreateAccount_ShouldPersistTheAccount() {
        val accountNumber = repo.createAccount("US", 0.00)

        db.transaction {
            val saved = Account.select { Account.id.eq(accountNumber) }.singleOrNull()
            assertNotNull(saved)
        }
    }

    @Test
    fun testCreateAccount_ShouldOpenTheAccountWithTheSpecifiedAmount() {
        val expectedAmount = 500.98
        val accountNumber = repo.createAccount("US", expectedAmount)

        db.transaction {
            val saved = Account.select { Account.id.eq(accountNumber) }.single()
            assertEquals(expectedAmount.toBigDecimal(), saved[Account.balance])
        }
    }

    @Test
    fun testGetBalance_ShouldReturnBalanceWhenAccountExists() {
        val accountNumber = "GB00123456789012345678"
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
}