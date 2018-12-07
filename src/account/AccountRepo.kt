package uk.stumme.account

import org.jetbrains.exposed.sql.*
import org.joda.time.DateTime
import org.joda.time.DateTimeZone
import uk.stumme.models.database.Account
import java.util.*
import kotlin.random.Random

class AccountRepo(val db: Database) {
    fun createAccount(countryCode: String): String {
        val accountNumber = generateAccountNumber(countryCode)

        db.transaction { Account.insert{
            it[Account.id] = accountNumber
            it[Account.dateOpened] = DateTime.now(DateTimeZone.forTimeZone(TimeZone.getTimeZone("UTC")));
        }}

        return accountNumber
    }

    private fun generateAccountNumber(countryCode: String): String {
        val accountNumber = (1..18)
            .map { Random.nextInt(0,9) }
            .joinToString("")
        return "${countryCode}00$accountNumber"
    }
}