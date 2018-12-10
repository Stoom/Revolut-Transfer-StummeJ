package uk.stumme.account

import exceptions.AccountNotFoundException
import exceptions.InsufficientFunds
import exceptions.InvalidArgumentException
import kotlinx.coroutines.runBlocking
import uk.stumme.models.domain.Account
import uk.stumme.models.domain.Transfer
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

    fun transfer(srcAccount: String, dstAccount: String, amount: Double): String {
        if(srcAccount.isEmpty())
            throw InvalidArgumentException("srcAccount")
        if(dstAccount.isEmpty())
            throw InvalidArgumentException("dstAccount")
        if(amount <= 0.00)
            throw InvalidArgumentException("amount")

        trapAccountNotFound("Source account does not exist") {
            val sourceBalance = accountRepo.getBalance(srcAccount)
            if (sourceBalance - amount < 0.00)
                throw InsufficientFunds()
        }
        trapAccountNotFound("Destination account does not exist") {
            accountRepo.getBalance(dstAccount)
        }

        val transferId = accountRepo.transfer(srcAccount, dstAccount, amount)
        return transferId.toString()
    }

    fun getTransfers(accountNumber: String): List<Transfer> {
        if (accountNumber.isEmpty())
            throw InvalidArgumentException("accountNumber")

        trapAccountNotFound {
            accountRepo.getBalance(accountNumber)
        }

        return accountRepo.getTransfers(accountNumber)
    }

    private fun generateAccountNumber(countryCode: String): String {
        val accountNumber = (1..18)
            .map { Random.nextInt(0,9) }
            .joinToString("")
        return "${countryCode}00$accountNumber"
    }

    private fun trapAccountNotFound(message: String? = null, callback: suspend () -> Unit) {
        try {
            runBlocking {
                callback()
            }
        } catch (e: AccountNotFoundException){
            throw AccountNotFoundException(message)
        }
    }
}