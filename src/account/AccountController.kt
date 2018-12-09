package uk.stumme.account

import exceptions.InvalidArgumentException
import uk.stumme.models.domain.Account
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import kotlin.random.Random

class AccountController(
    val accountRepo: AccountRepo = Injekt.get()
) {
    fun createAccount(countryCode: String, initialDeposit: Double): String {
        if(countryCode.isEmpty())
            throw InvalidArgumentException("countryCode")
        if(initialDeposit < 0)
            throw InvalidArgumentException("initialDeposit")

        val accountNumber = generateAccountNumber(countryCode)

        this.accountRepo.createAccount(accountNumber, initialDeposit)

        return accountNumber
    }

    fun getAccount(accountNumber: String): Account {
        val balance = this.accountRepo.getBalance(accountNumber)
        return Account(accountNumber, balance)
    }

    fun transfer(srcAccount: String, dstAccount: String, amount: Double) {
        if(srcAccount.isEmpty())
            throw InvalidArgumentException("srcAccount")
        if(dstAccount.isEmpty())
            throw InvalidArgumentException("dstAccount")
        if(amount <= 0.00)
            throw InvalidArgumentException("amount")
    }

    private fun generateAccountNumber(countryCode: String): String {
        val accountNumber = (1..18)
            .map { Random.nextInt(0,9) }
            .joinToString("")
        return "${countryCode}00$accountNumber"
    }
}