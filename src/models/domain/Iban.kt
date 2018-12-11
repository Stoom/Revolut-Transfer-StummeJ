package uk.stumme.models.domain

import exceptions.InvalidArgumentException
import java.math.BigInteger

class Iban(private var accountNumber: String) {
    init {
        if (accountNumber.length > 30)
            throw InvalidArgumentException("accountNumber")

        accountNumber = accountNumber.replace(" ", "")

        if (accountNumber.substring(2 until 4) == "00")
            calculateChecksum()
    }

    override fun toString(): String {
        return accountNumber
    }

    fun isValid(): Boolean {
        val countryCodeChecksum = accountNumber.substring(0 until 4)
        val number = accountNumber.substring(4)

        val checksumBigInt = BigInteger("$number$countryCodeChecksum".toIbanIntString())
        return checksumBigInt.mod(97.toBigInteger()) == 1.toBigInteger()
    }

    private fun calculateChecksum() {
        val countryCode = accountNumber.substring(0 until 2)
        val number = accountNumber.substring(4)

        val checksumBigInt = BigInteger("${number}${countryCode}00".toIbanIntString())
        val checksumNumber = checksumBigInt.mod(97.toBigInteger()).toInt()
        val checksum = (98 - checksumNumber).toString().padStart(2, '0')

        accountNumber = "$countryCode$checksum$number"
    }

    private fun Char.toIbanInt(): Int {
        if (this !in 'A'..'Z')
            return this.toString().toInt()

        return (this.dec() - 54).toInt()
    }

    private fun String.toIbanIntString(): String {
        return this.map { it.toIbanInt() }.joinToString("")
    }
}