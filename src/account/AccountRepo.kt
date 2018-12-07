package uk.stumme.account

import kotlin.random.Random

class AccountRepo {
    fun createAccount(countryCode: String): String {
        val accountNumber = generateAccountNumber(countryCode)

        return accountNumber
    }

    private fun generateAccountNumber(countryCode: String): String {
        val accountNumber = (1..18)
            .map { Random.nextInt(0,9) }
            .joinToString("")
        return "${countryCode}00$accountNumber"
    }
}