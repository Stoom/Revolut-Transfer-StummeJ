package test

import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.transactions.transaction
import uk.stumme.models.database.Account

fun stageAccount(accountNumber: String, balance: Double = 0.00) = transaction {
    Account.insert {
        it[Account.id] = accountNumber
        it[Account.balance] = balance.toBigDecimal()
    }
}