package uk.stumme.account

import org.jetbrains.exposed.sql.*
import uk.stumme.models.database.Account
import kotlin.random.Random

class AccountRepo(val db: Database) {
    fun createAccount(countryCode: String, initialDeposit: Double): String {
        val accountNumber = generateAccountNumber(countryCode)

        db.transaction { Account.insert{
            it[Account.id] = accountNumber
            it[Account.balance] = initialDeposit.toBigDecimal()
        }}

        return accountNumber
    }

    fun getBalance(accountNumber: String): Double {
        return db.transaction {
            var account = Account.select { Account.id.eq(accountNumber) }.single()
            account[Account.balance].toDouble()
        }
    }

    private fun generateAccountNumber(countryCode: String): String {
        val accountNumber = (1..18)
            .map { Random.nextInt(0,9) }
            .joinToString("")
        return "${countryCode}00$accountNumber"
    }
}