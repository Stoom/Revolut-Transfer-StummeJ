package uk.stumme.account

import exceptions.AccountNotFoundException
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import uk.stumme.models.database.Account
import uk.stumme.models.database.Transfers
import uk.stumme.models.domain.Iban
import uk.stumme.models.domain.Transfer
import java.util.*

class AccountRepo {
    companion object {
        val sourceLocks = mutableMapOf<String, Mutex>()
        val destinationLocks = mutableMapOf<String, Mutex>()
    }
    fun createAccount(accountNumber: Iban, initialDeposit: Double) {
        transaction {
            Account.insert {
                it[Account.id] = "$accountNumber"
                it[Account.balance] = initialDeposit.toBigDecimal()
            }
        }
        sourceLocks["$accountNumber"] = Mutex()
        destinationLocks["$accountNumber"] = Mutex()
    }

    fun hasAccount(accountNumber: Iban): Boolean {
        val account = transaction {
            Account.select { Account.id eq "$accountNumber" }.singleOrNull()
        }

        return account != null
    }

    fun getBalance(accountNumber: Iban): Double {
        return transaction {
            val account = Account.select { Account.id eq "$accountNumber" }.singleOrNull()
                ?: throw AccountNotFoundException()
            account[Account.balance].toDouble()
        }
    }

    fun transfer(srcAccount: Iban, dstAccount: Iban, amount: Double): UUID = runBlocking {
        val sourceAccount = "$srcAccount"
        val destAccount = "$dstAccount"

        try {
            AccountRepo.sourceLocks[sourceAccount]!!.lock()
            AccountRepo.destinationLocks[destAccount]!!.lock()
            transaction {
                val source = Account.select { Account.id eq sourceAccount }
                    .single()[Account.balance]
                val destination = Account.select { Account.id eq destAccount }
                    .single()[Account.balance]
                val transferAmount = amount.toBigDecimal()

                Account.update({ Account.id eq sourceAccount }) {
                    it[balance] = source - transferAmount
                }
                Account.update({ Account.id eq destAccount }) {
                    it[balance] = destination + transferAmount
                }
                Transfers.insert {
                    it[Transfers.sourceAccount] = sourceAccount
                    it[Transfers.destinationAccount] = destAccount
                    it[Transfers.amount] = transferAmount
                }[Transfers.id] ?: throw Exception()
            }
        }
        finally {
            AccountRepo.sourceLocks[sourceAccount]!!.unlock()
            AccountRepo.destinationLocks[destAccount]!!.unlock()
        }
    }

    fun getTransfers(accountNumber: Iban): List<Transfer> {
        return transaction {
            Transfers.select {
                Transfers.sourceAccount eq "$accountNumber"
            }.toList()
                .map { Transfer(
                    it[Transfers.id],
                    it[Transfers.sourceAccount],
                    it[Transfers.destinationAccount],
                    it[Transfers.amount].toDouble()
                )
            }
        }
    }
}