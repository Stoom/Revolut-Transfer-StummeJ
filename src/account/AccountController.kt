package uk.stumme.account

import exceptions.AccountNotFoundException
import exceptions.InsufficientFunds
import exceptions.InvalidArgumentException
import kotlinx.coroutines.runBlocking
import uk.stumme.models.domain.Account
import uk.stumme.models.domain.Iban
import uk.stumme.models.domain.Transfer
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import kotlin.random.Random

class AccountController(
    val accountRepo: AccountRepo = Injekt.get()
) {
    fun createAccount(countryCode: String, initialDeposit: Double): Iban {
        if(countryCode.isEmpty())
            throw InvalidArgumentException("countryCode")
        if(initialDeposit < 0)
            throw InvalidArgumentException("initialDeposit")

        val accountNumber = generateAccountNumber(countryCode)

        this.accountRepo.createAccount(accountNumber, initialDeposit)

        return accountNumber
    }

    fun getAccount(accountNumber: Iban): Account {
        val balance = this.accountRepo.getBalance(accountNumber)
        return Account(accountNumber, balance)
    }

    fun transfer(srcAccount: Iban, dstAccount: Iban, amount: Double): String {
        if(!srcAccount.isValid())
            throw InvalidArgumentException("srcAccount")
        if(!dstAccount.isValid())
            throw InvalidArgumentException("dstAccount")
        if(amount <= 0.00)
            throw InvalidArgumentException("amount")

        if (!accountRepo.hasAccount(srcAccount))
            throw AccountNotFoundException("Source account does not exist")
        if (!accountRepo.hasAccount(dstAccount))
            throw AccountNotFoundException("Destination account does not exist")

        val sourceBalance = accountRepo.getBalance(srcAccount)
        if (sourceBalance - amount < 0.00)
            throw InsufficientFunds()

        val transferId = accountRepo.transfer(srcAccount, dstAccount, amount)
        return "$transferId"
    }

    fun getTransfers(accountNumber: Iban): List<Transfer> {
        if (!accountNumber.isValid())
            throw InvalidArgumentException("accountNumber")

        if (!accountRepo.hasAccount(accountNumber))
            throw AccountNotFoundException()

        return accountRepo.getTransfers(accountNumber)
    }

    private fun generateAccountNumber(countryCode: String): Iban {
        val accountNumber = (1..26)
            .map { Random.nextInt(0,9) }
            .joinToString("")
        return Iban("${countryCode}00$accountNumber")
            .calculateChecksum()
    }
}