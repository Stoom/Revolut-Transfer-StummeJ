package uk.stumme.account

import exceptions.AccountNotFoundException
import exceptions.InsufficientFunds
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import uk.stumme.models.database.Account

class AccountRepo() {
    fun createAccount(accountNumber: String, initialDeposit: Double) {
        transaction {
            Account.insert {
                it[Account.id] = accountNumber
                it[Account.balance] = initialDeposit.toBigDecimal()
            }
        }
    }

    fun getBalance(accountNumber: String): Double {
        return transaction {
            val account = Account.select { Account.id.eq(accountNumber) }.singleOrNull()
                ?: throw AccountNotFoundException()
            account[Account.balance].toDouble()
        }
    }

    fun transfer(srcAccount: String, dstAccount: String, amount: Double) {
        transaction {
            val source = Account.select { Account.id.eq(srcAccount) }
                .single()[Account.balance]
            val destination = Account.select { Account.id.eq(dstAccount) }
                .single()[Account.balance]
            val transferAmount = amount.toBigDecimal()

            if (source - transferAmount < 0.00.toBigDecimal())
                throw InsufficientFunds()

            Account.update({ Account.id eq srcAccount }) {
                it[Account.balance] = source - transferAmount
            }
            Account.update({ Account.id eq dstAccount }) {
                it[Account.balance] = destination + transferAmount
            }
        }
    }
}