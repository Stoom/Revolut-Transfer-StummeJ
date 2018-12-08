package uk.stumme.account

import exceptions.AccountNotFoundException
import org.jetbrains.exposed.sql.*
import uk.stumme.models.database.Account
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class AccountRepo(private val db: Database =  Injekt.get()) {
    fun createAccount(accountNumber: String, initialDeposit: Double): String {
        db.transaction { Account.insert{
            it[Account.id] = accountNumber
            it[Account.balance] = initialDeposit.toBigDecimal()
        }}

        return accountNumber
    }

    fun getBalance(accountNumber: String): Double {
        return db.transaction {
            val account = Account.select { Account.id.eq(accountNumber) }.singleOrNull()
                ?: throw AccountNotFoundException()
            account[Account.balance].toDouble()
        }
    }
}