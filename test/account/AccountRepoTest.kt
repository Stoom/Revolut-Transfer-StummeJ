package account

import uk.stumme.account.AccountRepo
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

class AccountRepoTest {
    private var repo: AccountRepo? = null

    @BeforeTest
    fun Setup() {
        this.repo = AccountRepo()
    }

    @Test
    fun testCreateAccount_ShouldReturnTheNewAccountNumber() {
        val countryCode = "GB"

        val accountNumber = repo?.createAccount("GB")

        assertEquals(countryCode, accountNumber?.substring(0 until 2))
        assertEquals("00", accountNumber?.substring(2 until 4))
        assertEquals(22, accountNumber?.length)
    }
}