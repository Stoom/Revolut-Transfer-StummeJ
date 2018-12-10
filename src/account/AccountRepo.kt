package uk.stumme.account

import exceptions.AccountNotFoundException
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
}