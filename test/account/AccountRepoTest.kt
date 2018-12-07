package account

import org.jetbrains.exposed.sql.*
import uk.stumme.account.AccountRepo
import uk.stumme.models.database.Account
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

        val accountNumber = repo.createAccount(countryCode)

        assertEquals(countryCode, accountNumber.substring(0 until 2))
        assertEquals("00", accountNumber.substring(2 until 4))
        assertEquals(22, accountNumber.length)
    }

    @Test
    fun testCreateAccount_ShouldPersistTheAccount() {
        val accountNumber = repo.createAccount("US")

        db.transaction {
            val saved = Account.select { Account.id.eq(accountNumber) }.singleOrNull()
            assertNotNull(saved)
        }
    }
}