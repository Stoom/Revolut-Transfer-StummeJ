package test

import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.deleteAll
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.transactions.transaction
import uk.stumme.models.database.Account
import uk.stumme.models.database.Transfers

fun stageAccount(accountNumber: String, balance: Double = 0.00) = transaction {
    Account.insert {
        it[Account.id] = accountNumber
        it[Account.balance] = balance.toBigDecimal()
    }
}

fun initializeDatabase() {
    transaction {
        SchemaUtils.create(Account, Transfers)
    }
}

fun cleanupDatabase() {
    transaction {
        Account.deleteAll()
        Transfers.deleteAll()
    }
}