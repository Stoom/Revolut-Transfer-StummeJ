package uk.stumme.account

import exceptions.AccountNotFoundException
import org.jetbrains.exposed.sql.*
import uk.stumme.models.database.Account

class AccountRepo(val db: Database) {
    fun createAccount(accountNumber: String, initialDeposit: Double): String {
        db.transaction { Account.insert{
            it[Account.id] = accountNumber
            it[Account.balance] = initialDeposit.toBigDecimal()
        }}

        return accountNumber
    }

    fun getBalance(accountNumber: String): Double {
        return db.transaction {
            var account = Account.select { Account.id.eq(accountNumber) }.singleOrNull()
            if (account == null) {
                throw AccountNotFoundException()
            }
            account[Account.balance].toDouble()
        }
    }
}