package uk.stumme.account

import exceptions.AccountNotFoundException
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import uk.stumme.models.database.Account
import uk.stumme.models.database.Transfers
import uk.stumme.models.domain.Transfer
import java.util.*

class AccountRepo() {
    fun createAccount(accountNumber: String, initialDeposit: Double) {
        transaction {
            Account.insert {
                it[Account.id] = accountNumber
                it[Account.balance] = initialDeposit.toBigDecimal()
            }
        }
    }

    fun hasAccount(accountNumber: String): Boolean {
        val account = transaction {
            Account.select { Account.id eq accountNumber }.singleOrNull()
        }

        return account != null
    }

    fun getBalance(accountNumber: String): Double {
        return transaction {
            val account = Account.select { Account.id.eq(accountNumber) }.singleOrNull()
                ?: throw AccountNotFoundException()
            account[Account.balance].toDouble()
        }
    }

    fun transfer(srcAccount: String, dstAccount: String, amount: Double): UUID {
        return transaction {
            val source = Account.select { Account.id.eq(srcAccount) }
                .single()[Account.balance]
            val destination = Account.select { Account.id.eq(dstAccount) }
                .single()[Account.balance]
            val transferAmount = amount.toBigDecimal()

            Account.update({ Account.id eq srcAccount }) {
                it[balance] = source - transferAmount
            }
            Account.update({ Account.id eq dstAccount }) {
                it[balance] = destination + transferAmount
            }
            Transfers.insert {
                it[sourceAccount] = srcAccount
                it[destinationAccount] = dstAccount
                it[Transfers.amount] = transferAmount
            }[Transfers.id] ?: throw Exception()
        }
    }

    fun getTransfers(accountNumber: String): List<Transfer> {
        return transaction {
            Transfers.select {
                Transfers.sourceAccount eq accountNumber
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