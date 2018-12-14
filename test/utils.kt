package test

import kotlinx.coroutines.sync.Mutex
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.deleteAll
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.transactions.transaction
import uk.stumme.account.AccountRepo
import uk.stumme.models.database.Account
import uk.stumme.models.database.Transfers
import uk.stumme.models.domain.Iban
import java.lang.Exception
import java.util.*

fun stageAccount(accountNumber: Iban, balance: Double = 0.00) = stageAccount(accountNumber.toString(), balance)
fun stageAccount(accountNumber: String, balance: Double = 0.00) = transaction {
    Account.insert {
        it[Account.id] = accountNumber
        it[Account.balance] = balance.toBigDecimal()
    }
    AccountRepo.accountLocks[accountNumber] = Mutex()
}

fun stageTransfer(srcAccount: Iban, dstAccount: Iban, amount: Double = 0.00)
        : UUID = stageTransfer(srcAccount.toString(), dstAccount.toString(), amount)
fun stageTransfer(srcAccount: String, dstAccount: String, amount: Double = 0.00): UUID = transaction {
    Transfers.insert {
        it[Transfers.id] = UUID.randomUUID()
        it[Transfers.sourceAccount] = srcAccount
        it[Transfers.destinationAccount] = dstAccount
        it[Transfers.amount] = amount.toBigDecimal()
    }[Transfers.id] ?: throw Exception()
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